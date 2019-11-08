package mediaserver;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
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

    private static final int LOCALPORT = 8080;

    private static final int CLOUD_PORT = 80;

    private static final String DEV_FLAG = "dev";

    public static void main(String[] args) {

        boolean isDev = Boolean.getBoolean(DEV_FLAG);
        log.info("Running in {} mode", local(args) ? "local" : "cloud");

        SslContext sslCtx = getSslContext();

        Media media = retrieveMedia(args);

        EventLoopGroup listenGroup = new NioEventLoopGroup(1);
        EventLoopGroup workGroup = new NioEventLoopGroup(4);

        Supplier<Router> routerProvider = routerSupplier(media, local(args), isDev);

        ChannelInitializer<SocketChannel> handler = new ChannelInitializer<>() {
            @Override
            protected void initChannel(SocketChannel ch) {

                ChannelPipeline pipeline = ch.pipeline();
                if (sslCtx != null) {
                    pipeline.addLast(sslCtx.newHandler(ch.alloc()));
                }
                pipeline.addLast(new CustomHttpServerCodec());
                pipeline.addLast(routerProvider.get());
            }
        };

        ServerBootstrap bootstrap = new ServerBootstrap()
            .group(listenGroup, workGroup)
            .channel(NioServerSocketChannel.class)
            .handler(new LoggingHandler(LogLevel.DEBUG))
            .childHandler(handler);

        int port = local(args) ? LOCALPORT : CLOUD_PORT;

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

    public static SslContext getSslContext() {

        if (Boolean.getBoolean("openssl")) {

            File certificateFile = new File("taninim-cert.pem");
            File keyFile = new File("taninim-key.pkcs8.pem");

            SslContext sslCtx;
            try {
                sslCtx = SslContextBuilder.forServer(certificateFile, keyFile, "pasju").build();
            } catch (Exception e) {
                throw new IllegalStateException("Failed to setup SSL context", e);
            }
            return sslCtx;
        }

        if (Boolean.getBoolean("ssc")) {
            SelfSignedCertificate ssc;
            try {
                ssc = new SelfSignedCertificate();
            } catch (CertificateException e) {
                throw new IllegalStateException("Unexpected ssc error", e);
            }

            SslContext sslCtx;
            try {
                sslCtx = SslContextBuilder.forServer(
                    ssc.certificate(),
                    ssc.privateKey()
                ).build();
            } catch (Exception e) {
                throw new IllegalStateException("Failed to setup SSL context", e);
            }
            return sslCtx;
        }

        return null;
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

    public static boolean local(String[] args) {

        return args.length > 2 &&
            new File(args[0]).isDirectory() &&
            new File(args[1]).isFile() &&
            new File(args[2]).isDirectory();
    }

    private static Supplier<Router> routerSupplier(
        Media media,
        boolean local,
        boolean dev
    ) {

        IO io = new IO(dev);
        return () -> new Router(
            new Playlists(io, media),
            local ? new FileStreamer(io, media) : new S3Streamer(io, media),
            new Resources(io),
            new GUI(io, media));
    }
}
