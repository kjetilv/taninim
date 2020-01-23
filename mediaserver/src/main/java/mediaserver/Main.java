package mediaserver;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import mediaserver.externals.ACL;
import mediaserver.externals.S3;
import mediaserver.externals.S3Connector;
import mediaserver.gui.*;
import mediaserver.http.Fail;
import mediaserver.http.Gatekeeper;
import mediaserver.http.WebCache;
import mediaserver.media.CloudMedia;
import mediaserver.media.Media;
import mediaserver.media.PlaylistYaml;
import mediaserver.sessions.Ids;
import mediaserver.sessions.Sessions;
import mediaserver.toolkit.Templater;
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
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public final class Main {

    public static final Clock CLOCK = Clock.system(Config.TIMEZONE);

    public static final WebCache<String, String> TEMPLATE_CACHE = new WebCache<>(IO::read);

    public static final WebCache<String, byte[]> RESOURCE_CACHE = new WebCache<>(IO::readBytes);

    private static final OnceEvery ONCE_EVERY = new OnceEvery(Executors.newSingleThreadScheduledExecutor());

    private static final String RES = "res";

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    private static final String FB_SEC = "fbSec";

    private static final String FAVICON_ICO = RES + "/favicon.ico";

    private Main() {

    }

    public static void main(String[] args) {

        Sessions sessions = new Sessions(
            Config.SESSION_LENGTH,
            Config.INACTIVITY_MAX,
            Config.BYTES_PER_SESSION,
            Config.DEV_LOGIN);

        boolean local = local(args);
        boolean devLogin = local && Config.DEV_LOGIN;
        boolean noStream = Config.NEUTER;
        boolean sslPlaylists = Config.PRETEND_SSL || !devLogin && !local;

        log.info(
            "{}: Running in {}, streaming {}abled",
            sessions,
            local ? "local mode" : "the cloud",
            noStream ? "dis" : "en");

        if (local) {
            CloudMedia.updateLocals(Ids.IDS_RESOURCE, PlaylistYaml.CURATED_RESOURCE, PlaylistYaml.PLAYLISTS_RESOURCE);
        }

        SslContext mockSslContext = Config.PRETEND_SSL ? mockSslContext() : null;

        if (mockSslContext != null) {
            log.warn("Mock SSL context: {}", mockSslContext);
        }

        S3.Client s3 = S3Connector.get().orElse(null);

        log.info("Refresh resources every {}", Config.REFRESH_TIME);

        Supplier<Ids> ids = idsSupplier(args, local, s3);
        Supplier<Media> media = mediaSupplier(args, local);

        Templater templater = new Templater(TEMPLATE_CACHE);

        Streamer streamer = noStream ? new NullStreamer()
            : local ? new FileStreamer(CLOCK, media, Config.BYTES_PER_CHUNK)
            : new S3Streamer(CLOCK, media, s3, Config.BYTES_PER_CHUNK);

        log.info("Streamer: {}", streamer);

        Router router = new Router(
            sessions,
            templater,
            CLOCK,
            Arrays.asList(
                streamer,
                new Gatekeeper(templater),
                new FbUnauth(sessions),
                new FbAuth(sessions, ids, secretsProvider()),
                new Resources(RESOURCE_CACHE, RES),
                new Favicon(RESOURCE_CACHE, FAVICON_ICO),
                new Login(templater),
                new Admin(ids, sessions, templater, s3),
                new Playlists(media, templater, sslPlaylists),
                new GUI(media, templater),
                new Fail()));

        log.info("Binding to port {}", Config.PORT);

        new NettyRunner(
            Config.LISTEN_GROUP,
            Config.WORK_GROUP,
            Config.THREAD_GROUP,
            Config.THREAD_QUEUE,
            Config.IO_TIMEOUT,
            Config.CONNECT_TIMEOUT
        ).run(
            router,
            Config.PORT,
            mockSslContext);
    }

    private static Supplier<Media> mediaSupplier(String[] args, boolean local) {

        return ONCE_EVERY.interval(Config.REFRESH_TIME)
            .when(shouldRefresh(local, args))
            .get(() ->
                retrieveMedia(local, args));
    }

    private static Supplier<Ids> idsSupplier(String[] args, boolean local, S3.Client client) {

        return ONCE_EVERY.interval(Config.REFRESH_TIME)
            .when(shouldRefresh(local, args))
            .get(() ->
                refreshIds(local, client));
    }

    private static Ids refreshIds(boolean local, S3.Client s3) {

        ACL sources = local
            ? IO.readLocalACL(Ids.IDS_RESOURCE)
            : CloudMedia.acl();
        return new Ids(sources, s3);
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
