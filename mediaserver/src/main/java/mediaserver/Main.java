package mediaserver;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import mediaserver.externals.ACL;
import mediaserver.gui.*;
import mediaserver.http.Fail;
import mediaserver.http.Gatekeeper;
import mediaserver.http.WebCache;
import mediaserver.media.CloudMedia;
import mediaserver.media.Media;
import mediaserver.media.PlaylistYaml;
import mediaserver.sessions.Ids;
import mediaserver.sessions.Sessions;
import mediaserver.util.IO;
import mediaserver.util.OnceEvery;
import mediaserver.util.UpdateDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.CertificateException;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.stream.Stream;

public final class Main {

    public static final Clock CLOCK = Clock.system(Config.TIMEZONE);

    public static final WebCache<String, String> TEMPLATE_CACHE = new WebCache<>(IO::read);

    public static final WebCache<String, byte[]> RESOURCE_CACHE = new WebCache<>(IO::readBytes);

    private static final OnceEvery ONCE_EVERY = new OnceEvery(Executors.newSingleThreadScheduledExecutor());

    private static final String RES = "res";

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    private static final String FB_SEC = "fbSec";

    private static final String FAVICON_ICO = RES + "/favicon.ico";

    public static void main(String[] args) {

        Sessions sessions = new Sessions(
            Config.SESSION_LENGTH,
            Config.INACTIVITY_MAX,
            Config.BYTES_PER_SESSION,
            Config.DEV_LOGIN);

        boolean local = local(args);
        boolean devLogin = Config.DEV_LOGIN;
        boolean noStream = Config.NEUTER;
        boolean sslPlaylists = Config.PRETEND_SSL || !devLogin && !local;

        log.info(
            "{}: Running in {}, streaming {}abled",
            sessions,
            local ? "local mode" : "the cloud",
            noStream ? "dis" : "en");

        if (local) {
            Stream.of(Ids.IDS_RESOURCE, PlaylistYaml.CURATED_RESOURCE, PlaylistYaml.PLAYLISTS_RESOURCE)
                .forEach(resource -> {
                    if (CloudMedia.updatedFromRemote(resource)) {
                        log.info("Updated local: {}", resource);
                    } else {
                        log.info("Local is current: {}", resource);
                    }
                });
        }

        SslContext mockSslContext = Config.PRETEND_SSL && devLogin ? mockSslContext() : null;

        if (mockSslContext != null) {
            log.warn("Mock SSL context: {}", mockSslContext);
        }

        Supplier<Ids> ids = idsSupplier(args, local);
        Supplier<Media> media = mediaSupplier(args, local);

        Templater templater = new Templater(TEMPLATE_CACHE);

        AbstractStreamer streamer = streamer(media, local, noStream);

        Router router = new Router(
            sessions,
            CLOCK,
            streamer,
            new Debug(),
            new Gatekeeper(templater),
            new FbUnauth(sessions),
            new FbAuth(sessions, ids, secretsProvider()),
            new Resources(RES, RESOURCE_CACHE),
            new Favicon(RESOURCE_CACHE, FAVICON_ICO),
            new Login(templater),
            new Admin(ids, sessions, templater),
            new Playlists(media, templater, sslPlaylists),
            new GUI(media, templater),
            new Fail());

        log.info("Binding to port {}", Config.PORT);

        new NettyRunner(4, 1).run(router, Config.PORT, mockSslContext);
    }

    private static AbstractStreamer streamer(Supplier<Media> media, boolean local, boolean noStream) {

        AbstractStreamer streamer = noStream
            ? new NullStreamer()
            : local ? new FileStreamer(media)
            : new S3Streamer(media);

        log.info("Streamer: {}", streamer);
        return streamer;
    }

    private static Supplier<Media> mediaSupplier(String[] args, boolean local) {

        return ONCE_EVERY.interval(Config.REFRESH_TIME)
            .when(shouldRefresh(local, args))
            .get(() ->
                retrieveMedia(local, args));
    }

    private static Supplier<Ids> idsSupplier(String[] args, boolean local) {

        return ONCE_EVERY.interval(Config.REFRESH_TIME)
            .when(shouldRefresh(local, args))
            .get(() ->
                refreshIds(local));
    }

    private static Ids refreshIds(boolean local) {

        ACL sources = local
            ? IO.readLocalACL(Ids.IDS_RESOURCE)
            : CloudMedia.acl();
        return new Ids(sources);
    }

    private static BooleanSupplier shouldRefresh(boolean local, String[] args) {

        Supplier<Optional<Instant>> updater = local
            ? () -> lastMediaUpdate(args)
            : CloudMedia::lastUpdatedMedia;
        return new UpdateDetector(updater);
    }

    private static Optional<Instant> lastMediaUpdate(String[] args) {

        try {
            return Optional.of(Files.getLastModifiedTime(mediaFile(args)).toInstant());
        } catch (IOException e) {
            log.warn("Failed to check modified time of {}", mediaFile(args));
            return Optional.empty();
        }
    }

    private static boolean local(String[] args) {

        return args.length > 2 &&
            new File(args[0]).isDirectory() &&
            new File(args[1]).isFile() &&
            new File(args[2]).isDirectory();
    }

    private static Media retrieveMedia(boolean local, String[] args) {

        try {
            return local
                ? Media.local(mediaFile(args), libraryPath(args), resourcesPath(args))
                : CloudMedia.download();
        } catch (Exception e) {
            log.error("Error retrieving media, proceeding with empty... ", e);
            return Media.empty();
        }
    }

    private static Path mediaFile(String[] args) {

        return pathArg(0, args);
    }

    private static Path libraryPath(String[] args) {

        return pathArg(1, args);
    }

    private static Path resourcesPath(String[] args) {

        return pathArg(2, args);
    }

    private static Path pathArg(int i, String[] args) {

        return new File(args[i]).toPath();
    }

    private static SslContext mockSslContext() {

        SelfSignedCertificate ssc = selfSignedCertificate();
        log.info("Faux self-signed: {}/{}", ssc.certificate(), ssc.key());
        SslContext sslContext = selfSignedSsl(ssc);
        log.info("Faux SSL: {}", sslContext);
        return sslContext;
    }

    private static SslContext selfSignedSsl(SelfSignedCertificate ssc) {

        SslContext sslContext;
        try {
            sslContext = SslContextBuilder.forServer(
                ssc.certificate(),
                ssc.privateKey()
            ).build();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to setup SSL context", e);
        }
        return sslContext;
    }

    private static SelfSignedCertificate selfSignedCertificate() {

        SelfSignedCertificate ssc;
        try {
            ssc = new SelfSignedCertificate();
        } catch (CertificateException e) {
            throw new IllegalStateException("Unexpected ssc error", e);
        }
        return ssc;
    }

    private static Supplier<char[]> secretsProvider() {

        return () -> IO.getProperty(FB_SEC).toCharArray();
    }

}
