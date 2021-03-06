package mediaserver;

import java.net.SocketException;
import java.util.Optional;
import java.util.stream.Stream;
import javax.net.ssl.SSLHandshakeException;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.ssl.NotSslRecordException;

final class Exceptions {

    static SeriosityLevel seriosityLevl(ChannelHandlerContext ctx, Throwable cause) {

        boolean testing = Optional.ofNullable(ctx.channel())
            .map(Channel::localAddress)
            .map(Object::toString)
            .filter(s -> s.contains("/0:0:0:0:0:0:0:1:"))
            .isPresent();

        return Optional.of(cause).stream()
            .flatMap(e -> {
                for (Throwable t = e; t != null && t.getCause() != t; t = t.getCause()) {
                    String msg = e.getMessage();
                    if (e instanceof SocketException && "Connection reset".equalsIgnoreCase(msg)) {
                        return Stream.of(SeriosityLevel.CLIENT_WHIM);
                    }
                    if (sslStuff(e) && testing) {
                        return Stream.of(SeriosityLevel.MEH);
                    }
                }
                return Stream.empty();
            })
            .findFirst()
            .orElse(SeriosityLevel.LOG);
    }

    enum SeriosityLevel {
        LOG, CLIENT_WHIM, SUMMARIZE, MEH
    }

    private Exceptions() {

    }

    private static boolean sslStuff(Throwable e) {

        return is(SSLHandshakeException.class, e) || is(NotSslRecordException.class, e);
    }

    private static boolean is(Class<?> sslHandshakeExceptionClass, Throwable e) {

        return e.getMessage() != null && e.getMessage().contains(sslHandshakeExceptionClass.getName());
    }
}
