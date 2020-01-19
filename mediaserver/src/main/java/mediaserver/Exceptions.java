package mediaserver;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.ssl.NotSslRecordException;

import javax.net.ssl.SSLHandshakeException;
import java.net.SocketException;
import java.util.Optional;
import java.util.stream.Stream;

final class Exceptions {

    private Exceptions() {

    }

    static IgnoreLevel ignoreLevel(ChannelHandlerContext ctx, Throwable cause) {

        boolean testing = Optional.ofNullable(ctx.channel())
            .map(Channel::localAddress)
            .map(Object::toString)
            .filter(s -> s.contains("/0:0:0:0:0:0:0:1:"))
            .isPresent();

        return Optional.of(cause).stream()
            .flatMap(throwable -> {
                for (Throwable t = throwable; t != null && t.getCause() != t; t = t.getCause()) {
                    String message = throwable.getMessage();
                    if (throwable instanceof SocketException &&
                        message != null &&
                        message.equalsIgnoreCase("Connection reset")
                    ) {
                        return Stream.of(IgnoreLevel.SUMMARIZE);
                    }
                    if (
                        (is(SSLHandshakeException.class, throwable) || is(
                            NotSslRecordException.class, throwable)
                        ) && testing
                    ) {
                        return Stream.of(IgnoreLevel.MEH);
                    }
                }
                return Stream.empty();
            })
            .findFirst()
            .orElse(IgnoreLevel.LOG);
    }

    private static boolean is(Class<?> sslHandshakeExceptionClass, Throwable e) {

        return e.getMessage() != null && e.getMessage().contains(sslHandshakeExceptionClass.getName());
    }

    enum IgnoreLevel {

        LOG, SUMMARIZE, MEH
    }
}
