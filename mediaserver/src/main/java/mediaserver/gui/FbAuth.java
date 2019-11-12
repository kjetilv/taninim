package mediaserver.gui;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import mediaserver.util.IO;

public class FbAuth extends Nettish {

    public FbAuth(IO io) {

        super(io, "/auth");
    }

    @Override
    public HttpResponse handle(HttpRequest req, String path, ChannelHandlerContext ctx) {
        HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(req);
        try {
//            decoder.offer().
//
//            List<InterfaceHttpData> bodyHttpDatas = decoder.getBodyHttpDatas();

            return respond(ctx, path, HttpResponseStatus.BAD_REQUEST);
        } finally {
            decoder.destroy();
        }
    }

    private enum FPar {

        userID,

        accessToken,

        signedRequest,

        timeoutInSeconds;
    }
}
