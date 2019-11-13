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
import mediaserver.util.IO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.security.cert.CertificateException;
import java.util.function.Supplier;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    private static final int LOCALPORT = 8443;

    private static final int CLOUD_PORT = 80;

    private static final String DEV_FLAG = "dev";

    private static final String LIVE_FLAG = "dev";

    public static void main(String[] args) {

        log.info("Running in {}{} mode",
            local(args) ? "local" : "cloud", dev() ? " dev" : "");

        Media media = retrieveMedia(args);

        EventLoopGroup listenGroup = new NioEventLoopGroup(1);
        EventLoopGroup workGroup = new NioEventLoopGroup(4);

        boolean neuter = !live();
        IO io = new IO(dev());

        Supplier<Router> routerProvider = routerSupplier(io, media, neuter, local(args));

        ChannelInitializer<SocketChannel> handler = new ServerInitializer(routerProvider, sslContext());

        ServerBootstrap bootstrap = bootstrap(listenGroup, workGroup, handler);

        int port = dev() || local(args) ? LOCALPORT : CLOUD_PORT;

        start(bootstrap, port, listenGroup, workGroup);
    }

    public static boolean dev() {

        return Boolean.getBoolean(DEV_FLAG);
    }

    public static boolean live() {

        return Boolean.getBoolean(LIVE_FLAG);
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

    private static Supplier<Router> routerSupplier(IO io, Media media, boolean neuter, boolean local) {

        return () -> new Router(
            streamer(io, media, neuter, local),
            new FbAuth(io),
            new Playlists(io, media),
            new Resources(io),
            new GUI(io, media));
    }

    private static Nettish streamer(IO io, Media media, boolean neuter, boolean local) {

        return neuter ? new NullStreamer(io, media)
            : local ? new FileStreamer(io, media)
            : new S3Streamer(io, media);
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
