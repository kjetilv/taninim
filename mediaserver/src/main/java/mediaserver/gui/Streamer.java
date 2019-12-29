package mediaserver.gui;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpResponse;
import mediaserver.http.WebPath;
import mediaserver.media.Track;
import mediaserver.sessions.Session;

public interface Streamer {

    HttpResponse stream(WebPath webPath, Session session, Track track, ChannelHandlerContext ctx);
}
