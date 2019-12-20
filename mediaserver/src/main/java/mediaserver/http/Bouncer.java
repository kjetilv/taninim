package mediaserver.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.util.Optional;

public class Bouncer extends Nettish {

    public Bouncer() {

        super("/");
    }

    @Override
    public Optional<HttpResponse> handle(FullHttpRequest req, String path, ChannelHandlerContext ctx) {

        return Optional.of(respond(ctx, HttpResponseStatus.BAD_REQUEST));
    }
}
