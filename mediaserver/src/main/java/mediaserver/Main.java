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
import mediaserver.sessions.Sessions;
import mediaserver.util.IO;
import mediaserver.util.OnceEvery;
import mediaserver.util.UpdateDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
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

    private static final int LOCALPORT = 8443;

    private static final int CLOUD_PORT = 80;

    private static final String DEV_FLAG = "dev";

    private static final String LIVE_FLAG = "live";

    private static final String FB_SEC = "fbSec";

    private static final Duration REFRESH_TIME = Duration.ofMinutes(5);

    public static void main(String[] args) {

        boolean dev = dev();
        boolean live = live();
        boolean neuter = !(live || dev);
        boolean ssl = dev || local(args);

        log.info(
            "Running in {}{} mode, streaming {}abled",
            local(args) ? "local" : "cloud",
            dev ? " dev" : "",
            neuter ? "dis" : "en");

        OnceEvery onceEvery = new OnceEvery(Executors.newSingleThreadScheduledExecutor());

        Supplier<Media> media =
            onceEvery.interval(REFRESH_TIME)
                .when(shouldRefresh(args, CloudMedia::lastUpdatedMedia))
                .get(() -> retrieveMedia(args));
        Supplier<Map<String, ?>> ids =
            onceEvery.interval(REFRESH_TIME)
                .when(shouldRefresh(args, CloudMedia::lastUpdatedIds))
                .get(() -> refreshIds(args));

        EventLoopGroup listenGroup = new NioEventLoopGroup(1);
        EventLoopGroup workGroup = new NioEventLoopGroup(4);

        IO io = new IO(dev);

        Supplier<Router> routerProvider = routerSupplier(io, media, neuter, local(args), ids);

        ChannelInitializer<SocketChannel> handler =
            new ServerInitializer(routerProvider, ssl ? sslContext() : null);

        ServerBootstrap bootstrap = bootstrap(listenGroup, workGroup, handler);

        int port = ssl ? LOCALPORT : CLOUD_PORT;

        start(bootstrap, port, listenGroup, workGroup);
    }

    public static Map<String, ?> refreshIds(String[] args) {

        return local(args) ? IO.readResource("ids.json") : CloudMedia.ids();
    }

    public static BooleanSupplier shouldRefresh(String[] args, Supplier<Instant> lastUpdatedMedia) {

        return local(args)
                ? () -> true
                : new UpdateDetector(lastUpdatedMedia);
    }

    public static boolean dev() {

        return isTrue(DEV_FLAG);
    }

    public static boolean live() {

        return isTrue(LIVE_FLAG);
    }

    public static boolean isTrue(String flag) {

        return Boolean.getBoolean(flag) || Optional.ofNullable(System.getenv(flag)).filter("true"::equals).isPresent();
    }

    public static String getProperty(String property) {

        return System.getProperty(property, System.getenv(property));
    }

    public static boolean local(String[] args) {

        return args.length > 2 &&
            new File(args[0]).isDirectory() &&
            new File(args[1]).isFile() &&
            new File(args[2]).isDirectory();
    }

    public static Media retrieveMedia(String[] args) {

        try {
            return local(args)
                ? Media.local(args[0], args[1], args[2])
                : CloudMedia.download();
        } catch (Exception e) {
            log.error("Error retrieving media, proceeding with empty... ", e);
            return Media.empty();
        }
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
        IO io,
        Supplier<Media> media,
        boolean neuter,
        boolean local,
        Supplier<Map<String, ?>> ids
    ) {

        Supplier<char[]> secretProvider = () ->
            getProperty(FB_SEC).toCharArray();
        Sessions sessions =
            new Sessions(Duration.ofDays(1), Clock.system(ZoneId.of("CET")));
        return () ->
            new Router(
                streamer(io, media, sessions, neuter, local),
                new FbUnauth(io, sessions),
                new FbAuth(io, sessions, secretProvider, ids),
                new Playlists(io, media),
                new Resources(io),
                new GUI(io, media, sessions));
    }

    private static Nettish streamer(IO io, Supplier<Media> media, Sessions sessions, boolean neuter, boolean local) {

        return neuter ? new NullStreamer(io, media, sessions)
            : local ? new FileStreamer(io, media, sessions)
            : new S3Streamer(io, media, sessions);
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

    private static void start(
        ServerBootstrap bootstrap,
        int port,
        EventLoopGroup listenGroup,
        EventLoopGroup workGroup
    ) {

        try {
            Channel ch = bootstrap.bind(port).sync().channel();
            log.info("Bound to port {}", port);
            ch.closeFuture().sync();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted", e);
        } finally {
            listenGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
        }
    }
}
