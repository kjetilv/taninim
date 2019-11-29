package mediaserver;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import mediaserver.gui.*;
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
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    private static final int LOCALPORT = 1080;

    private static final int LOCALPORT_SSL = 1443;

    private static final int CLOUD_PORT = 80;

    private static final String DEV_FLAG = "dev";

    private static final String LIVE_FLAG = "live";

    private static final String SSL_FLAG = "ssl";

    private static final String FB_SEC = "fbSec";

    private static final Duration REFRESH_TIME = Duration.ofMinutes(5);

    public static void main(String[] args) {

        boolean dev = dev();
        boolean live = live();
        boolean neuter = !(live || dev);
        boolean local = local(args);
        boolean ssl = ssl();

        log.info(
            "Running in {}{} mode, streaming {}abled",
            local ? "local" : "cloud",
            dev ? " dev" : "",
            neuter ? "dis" : "en");

        OnceEvery onceEvery = new OnceEvery(Executors.newSingleThreadScheduledExecutor());

        Supplier<Media> media = onceEvery.interval(REFRESH_TIME)
            .when(shouldRefresh(local, args))
            .get(() ->
                retrieveMedia(local, args));
        Supplier<Ids> ids = onceEvery.interval(REFRESH_TIME)
            .when(shouldRefresh(local, args))
            .get(() ->
                refreshIds(local, ssl, dev));

        Supplier<Router> routerProvider = routerSupplier(media, ids, neuter, local);

        ChannelInitializer<SocketChannel> handler =
            new ServerInitializer(routerProvider, ssl ? sslContext() : null);

        EventLoopGroup listenGroup = new NioEventLoopGroup(1);
        EventLoopGroup workGroup = new NioEventLoopGroup(4);

        ServerBootstrap bootstrap = bootstrap(listenGroup, workGroup, handler);

        try {
            Channel ch = bootstrap.bind(ssl ? LOCALPORT_SSL
                : dev ? LOCALPORT
                : CLOUD_PORT).sync().channel();
            log.info("Bound to port {}", ssl ? LOCALPORT_SSL
                : dev ? LOCALPORT
                : CLOUD_PORT);
            ch.closeFuture().sync();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted", e);
        } finally {
            listenGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
        }
    }

    public static Ids refreshIds(boolean local, boolean ssl, boolean dev) {

        Map<String, ?> sources = local
            ? IO.readResource("ids.json")
            : CloudMedia.ids();
        return new Ids(sources, dev && !ssl);
    }

    public static BooleanSupplier shouldRefresh(boolean local, String[] args) {

        Supplier<Optional<Instant>> updater = local
            ? () -> lastMediaUpdate(args)
            : CloudMedia::lastUpdatedMedia;
        return new UpdateDetector(updater);
    }

    public static Optional<Instant> lastMediaUpdate(String[] args) {

        try {
            return Optional.of(Files.getLastModifiedTime(mediaFile(args)).toInstant());
        } catch (IOException e) {
            log.warn("Failed to check modified time of {}", mediaFile(args));
            return Optional.empty();
        }
    }

    public static boolean dev() {

        return isTrue(DEV_FLAG);
    }

    public static boolean live() {

        return isTrue(LIVE_FLAG);
    }

    public static boolean ssl() {

        return isTrue(SSL_FLAG);
    }

    public static boolean isTrue(String flag) {

        return Boolean.getBoolean(flag) || Optional.ofNullable(System.getenv(flag)).filter("true"::equals).isPresent();
    }

    public static boolean local(String[] args) {

        return args.length > 2 &&
            new File(args[0]).isDirectory() &&
            new File(args[1]).isFile() &&
            new File(args[2]).isDirectory();
    }

    public static Media retrieveMedia(boolean local, String[] args) {

        try {
            return local
                ? Media.local(mediaFile(args), libraryPath(args), resourcesPath(args))
                : CloudMedia.download();
        } catch (Exception e) {
            log.error("Error retrieving media, proceeding with empty... ", e);
            return Media.empty();
        }
    }

    public static Path mediaFile(String[] args) {

        return new File(args[0]).toPath();
    }

    public static Path libraryPath(String[] args) {

        return pathArg(1, args);
    }

    public static Path resourcesPath(String[] args) {

        return new File(args[2]).toPath();
    }

    private static Path pathArg(int i, String[] args) {

        return new File(args[i]).toPath();
    }

    private static SslContext sslContext() {

        SelfSignedCertificate ssc;
        try {
            ssc = new SelfSignedCertificate();
        } catch (CertificateException e) {
            throw new IllegalStateException("Unexpected ssc error", e);
        }

        try {
            return SslContextBuilder.forServer(
                ssc.certificate(),
                ssc.privateKey()
            ).build();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to setup SSL context", e);
        }
    }

    private static Supplier<Router> routerSupplier(
        Supplier<Media> media,
        Supplier<Ids> ids,
        boolean neuter,
        boolean local
    ) {

        Supplier<char[]> secretProvider = () ->
            IO.getProperty(FB_SEC).toCharArray();
        Sessions sessions =
            new Sessions(ids, Duration.ofDays(1), Clock.system(ZoneId.of("CET")));
        return () -> new Router(
            neuter ? new NullStreamer(media, sessions)
                : local ? new FileStreamer(media, sessions)
                : new S3Streamer(media, sessions),
            new FbUnauth(sessions),
            new FbAuth(sessions, secretProvider, ids),
            new Playlists(media),
            new Resources(),
            new GUI(media, sessions));
    }

    private static ServerBootstrap bootstrap(
        EventLoopGroup listenGroup,
        EventLoopGroup workGroup,
        ChannelInitializer<SocketChannel> handler
    ) {

        return new ServerBootstrap()
            .group(listenGroup, workGroup)
            .channel(NioServerSocketChannel.class)
            .handler(new LoggingHandler(LogLevel.DEBUG))
            .childHandler(handler);
    }
}
