package mediaserver;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import mediaserver.gui.*;
import mediaserver.http.Fail;
import mediaserver.http.Gatekeeper;
import mediaserver.http.NettyHandler;
import mediaserver.http.WebCache;
import mediaserver.media.CloudMedia;
import mediaserver.media.Media;
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
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public final class Main {

    private static final OnceEvery ONCE_EVERY = new OnceEvery(Executors.newSingleThreadScheduledExecutor());

    private static final Duration SESSION_LENGTH = duration("sessionLength", Duration.ofDays(1));

    private static final Duration INACTIVITY_MAX = duration("inactivityMax", Duration.ofHours(1));

    private static final Duration REFRESH_TIME = duration("refresh", Duration.ofMinutes(5));

    private static final Clock CET = Clock.system(ZoneId.of(setting("timeZone").orElse("CET")));

    private static final String RES = "res";

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    private static final String FB_SEC = "fbSec";

    private static final boolean DEV_LOGIN = isTrue("dev");

    private static final boolean LIVE = isTrue("live");

    private static final boolean NEUTER = !(LIVE || DEV_LOGIN);

    private static final boolean PRETEND_SSL = isTrue("ssl");

    private static final String FAVICON_ICO = RES + "/favicon.ico";

    private static final int PORT = PRETEND_SSL ? 1443
        : DEV_LOGIN ? 1080
        : 80;

    public static void main(String[] args) {

        boolean local = local(args);
        boolean devLogin = DEV_LOGIN && local;
        boolean noStream = NEUTER;

        log.info(
            "Running in {}{}, streaming {}abled",
            local ? "local mode" : "the cloud",
            devLogin ? " with dev login" : "",
            noStream ? "dis" : "en");

        SslContext mockSslContext = PRETEND_SSL && devLogin ? mockSslContext() : null;

        Supplier<Ids> ids = idsSupplier(args, local);
        Supplier<Media> media = mediaSupplier(args, local);
        Sessions sessions =
            new Sessions(SESSION_LENGTH, INACTIVITY_MAX, CET, devLogin);
        WebCache<String, byte[]> webCache = new WebCache<>(IO::readBytes);
        Templater templater = new Templater();

        Streamer streamer = noStream
            ? new NullStreamer()
            : streamer(local, media, sessions);

        Supplier<Router> router = () ->
            new Router(
                streamer,
                new Debug(),
                new Gatekeeper(sessions, templater),
                new FbUnauth(sessions),
                new FbAuth(sessions, ids, secretsProvider()),
                new Resources(RES, webCache),
                new Favicon(webCache, FAVICON_ICO),
                new Login(templater),
                new PlaylistsM3U(media, templater, !(devLogin || local)),
                new GUI(media, sessions, templater),
                new Fail());

        log.info("Binding to port {}", PORT);

        new NettyRun(4, 1)
            .run(router, PORT, mockSslContext);
    }

    private static Supplier<Media> mediaSupplier(String[] args, boolean local) {

        return ONCE_EVERY.interval(REFRESH_TIME)
            .when(shouldRefresh(local, args))
            .get(() ->
                retrieveMedia(local, args));
    }

    private static Supplier<Ids> idsSupplier(String[] args, boolean local) {

        return ONCE_EVERY.interval(REFRESH_TIME)
            .when(shouldRefresh(local, args))
            .get(() ->
                refreshIds(local));
    }

    private static Ids refreshIds(boolean local) {

        Map<String, ?> sources = local
            ? IO.readResource("ids.json")
            : CloudMedia.ids();
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

    private static Duration duration(String setting, Duration defaultDuration) {

        return setting(setting)
            .map(Duration::parse)
            .orElse(defaultDuration);
    }

    private static Optional<String> setting(String setting) {

        return Optional.ofNullable(System.getProperty(setting, System.getenv(setting)));
    }

    private static boolean isTrue(String flag) {

        return Boolean.getBoolean(flag) ||
            Optional.ofNullable(System.getenv(flag)).filter("true"::equals).isPresent();
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

    private static Streamer streamer(boolean local, Supplier<Media> media, Sessions sessions) {

        return local
            ? new FileStreamer(media, sessions)
            : new S3Streamer(media, sessions);
    }
}
