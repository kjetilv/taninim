package mediaserver.gui;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import mediaserver.util.IO;

import java.util.List;

import static io.netty.handler.codec.http.HttpResponseStatus.CONTINUE;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class FbAuth extends Nettish {

    public FbAuth(IO io) {

        super(io, "/auth");
    }

    @Override
    public HttpResponse handle(HttpRequest req, String path, ChannelHandlerContext ctx) {

        HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(req);
        try {

            List<InterfaceHttpData> bodyHttpDatas = decoder.getBodyHttpDatas();

            System.out.println(bodyHttpDatas.size());

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
