package mediaserver.gui;

import io.netty.handler.codec.http.HttpResponse;
import mediaserver.http.WebPath;
import mediaserver.media.Track;

public interface Streamer {

    HttpResponse stream(WebPath webPath, Track track);
}
