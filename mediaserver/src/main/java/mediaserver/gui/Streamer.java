package mediaserver.gui;

import io.netty.channel.ChannelHandlerContext;
import mediaserver.http.Handling;
import mediaserver.http.WebPath;
import mediaserver.media.Track;
import mediaserver.sessions.Session;

public interface Streamer {

    Handling stream(WebPath webPath, Session session, Track track, ChannelHandlerContext ctx);
}
