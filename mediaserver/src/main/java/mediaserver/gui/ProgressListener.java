package mediaserver.gui;

import io.netty.channel.ChannelProgressiveFuture;
import io.netty.channel.ChannelProgressiveFutureListener;
import mediaserver.http.WebPath;
import mediaserver.media.Track;
import mediaserver.toolkit.Chunk;
import mediaserver.toolkit.Range;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.time.Clock;
import java.time.Duration;

final class ProgressListener implements ChannelProgressiveFutureListener {

    private static final Logger log = LoggerFactory.getLogger(ProgressListener.class);

    private final Range range;

    private final Chunk chunk;

    private static final int KILO = 1_000;

    private static final int MEGA = KILO * KILO;

    private final Clock clock;

    private final WebPath webPath;

    private final Track track;

    private static final int WARN_THRESHOLD_SECONDS = 30;

    ProgressListener(Clock clock, WebPath webPath, Track track, Range range, Chunk chunk) {

        this.clock = clock;
        this.webPath = webPath;
        this.track = track;
        this.range = range;
        this.chunk = chunk;
    }

    @Override
    public void operationComplete(ChannelProgressiveFuture future) {

        Duration streamingTime = Duration.between(webPath.getTime(), clock.instant());
        SocketAddress remote = future.channel().remoteAddress();
        if (streamingTime.toSeconds() > WARN_THRESHOLD_SECONDS) {
            log.warn("{}: {} -> {} in {} IS A LONG TIME: {} > {} > {}",
                webPath.getCtx(), range, chunk, printed(streamingTime), webPath, track, remote);

        }
        log.info("{}: {} -> {} in {}: {} > {} > {}",
            webPath.getCtx(), range, chunk, printed(streamingTime), webPath, track, remote);
    }

    @Override
    public void operationProgressed(ChannelProgressiveFuture future, long progress, long total) {

        if (progress < total) {
            log.debug("{} {}: {}%", track.getName(), chunk, progress * 100 / total);
        }
    }

    private static String printed(Duration streamingTime) {

        String interval;
        if (streamingTime.toMillis() < 1000) {
            interval = streamingTime.toMillis() + "ms";
        } else {
            interval = streamingTime.toString();
        }
        return interval;
    }
}
