package mediaserver;

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

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import mediaserver.externals.ACL;
import mediaserver.externals.S3Client;
import mediaserver.externals.S3Connector;
import mediaserver.gui.AdminPage;
import mediaserver.gui.AlbumPage;
import mediaserver.gui.Favicon;
import mediaserver.gui.FbAuth;
import mediaserver.gui.FbUnauth;
import mediaserver.gui.IndexPage;
import mediaserver.gui.Login;
import mediaserver.gui.Playlists;
import mediaserver.gui.Resources;
import mediaserver.http.Route;
import mediaserver.http.Route.Method;
import mediaserver.http.WebCache;
import mediaserver.media.CloudMedia;
import mediaserver.media.Media;
import mediaserver.media.PlaylistYaml;
import mediaserver.sessions.AccessLevel;
import mediaserver.sessions.Ids;
import mediaserver.sessions.Sessions;
import mediaserver.stream.FileStreamer;
import mediaserver.stream.NullStreamer;
import mediaserver.stream.S3Streamer;
import mediaserver.stream.Streamer;
import mediaserver.toolkit.Templater;
import mediaserver.util.IO;
import mediaserver.util.OnceEvery;
import mediaserver.util.UpdateDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static mediaserver.Config.*;

final class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {

        boolean local = local(args);
        boolean devLogin = local && DEV_LOGIN;
        boolean noStream = NEUTER;
        boolean sslPlaylists = PRETEND_SSL || !devLogin && !local;

        if (local) {
            CloudMedia.updateLocals(Ids.IDS_RESOURCE, PlaylistYaml.CURATED_RESOURCE, PlaylistYaml.PLAYLISTS_RESOURCE);
        }

        SslContext mockSslContext = PRETEND_SSL ? mockSslContext() : null;

        if (mockSslContext != null) {
            log.warn("Mock SSL context: {}", mockSslContext);
        }

        S3Client s3 = S3Connector.get().orElse(null);

        log.info("Refreshing resources every {}", REFRESH_TIME);

        Supplier<Ids> ids = idsSupplier(args, local, s3);
        Supplier<Media> media = mediaSupplier(args, local);

        Templater templater = new Templater(TMPL_CACHE);

        Route audio = new Route("audio", AccessLevel.STREAM_CURATED, Method.GET, Method.HEAD);
        Streamer streamer = noStream ? new NullStreamer(audio)
            : local ? new FileStreamer(audio, CLOCK, media, BYTES_PER_CHUNK)
                : new S3Streamer(audio, CLOCK, media, s3, BYTES_PER_CHUNK);

        log.info("Streamer: {}", streamer);
        log.info("Binding to port {}", PORT);

        Sessions sessions =
            new Sessions(ids, SESSION_LENGTH, INACTIVITY_MAX, BYTES_PER_SESSION, DEV_LOGIN);

        NettyRunner nettyRunner = new NettyRunner(
            LISTEN_GROUP, WORK_GROUP, THREAD_GROUP, THREAD_QUEUE, IO_TIMEOUT, CONNECT_TIMEOUT);

        Router router = new Router(sessions, templater, CLOCK,
            streamer,
            new Favicon(
                new Route("favicon.ico", AccessLevel.NONE, Method.GET),
                RES_CACHE, FAVICON_ICO),
            new Login(
                new Route("login", AccessLevel.NONE, Method.GET),
                templater),
            new FbAuth(
                new Route("auth", AccessLevel.NONE, Method.POST),
                sessions, ids, secretsProvider()),
            new Resources(
                new Route("res", AccessLevel.NONE, Method.GET),
                RES_CACHE, RES),
            new AlbumPage(
                new Route("album", AccessLevel.LOGIN, Method.GET),
                media, templater),
            new Playlists(
                new Route("playlist", AccessLevel.STREAM, Method.GET),
                media, templater, sslPlaylists),
            new FbUnauth(
                new Route("unauth", AccessLevel.LOGIN, Method.GET, Method.POST),
                sessions),
            new AdminPage(
                new Route("admin", AccessLevel.ADMIN, Method.GET, Method.POST),
                media, ids, sessions, templater, s3),
            new IndexPage(
                new Route("", AccessLevel.LOGIN, Method.GET),
                media, templater));

        log.info(
            "{}: Running in {}, streaming {}abled",
            sessions,
            local ? "local mode" : "the cloud",
            noStream ? "dis" : "en");

        nettyRunner.run(router, PORT, mockSslContext);
    }

    private Main() {

    }

    private static final Clock CLOCK = Clock.system(TIMEZONE);

    private static final WebCache<String, String> TMPL_CACHE = new WebCache<>(IO::readUTF8);

    private static final WebCache<String, byte[]> RES_CACHE = new WebCache<>(IO::readBytes);

    private static final OnceEvery.TimingBuilder PERIODICALLY =
        new OnceEvery(Executors.newSingleThreadScheduledExecutor()).interval(REFRESH_TIME);

    private static final String RES = "res";

    private static final String FB_SEC = "fbSec";

    private static final String FAVICON_ICO = RES + "/favicon.ico";

    private static Supplier<Media> mediaSupplier(String[] args, boolean local) {

        return PERIODICALLY
            .when(shouldRefresh(local, args))
            .get(() ->
                retrieveMedia(local, args));
    }

    private static Supplier<Ids> idsSupplier(String[] args, boolean local, S3Client client) {

        return PERIODICALLY
            .when(shouldRefresh(local, args))
            .get(() ->
                refreshIds(local, client));
    }

    private static Ids refreshIds(boolean local, S3Client s3) {

        ACL sources = local
            ? ACL.readLocalACL(Ids.IDS_RESOURCE)
            : CloudMedia.acl();
        return new Ids(sources, s3);
    }

    private static BooleanSupplier shouldRefresh(boolean local, String[] args) {

        return new UpdateDetector(local
            ? () -> lastMediaUpdate(args)
            : CloudMedia::lastUpdatedMedia);
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
