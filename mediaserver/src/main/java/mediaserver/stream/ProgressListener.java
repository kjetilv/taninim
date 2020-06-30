package mediaserver.stream;

import java.net.SocketAddress;
import java.time.Clock;
import java.time.Duration;

import io.netty.channel.ChannelProgressiveFuture;
import io.netty.channel.ChannelProgressiveFutureListener;
import mediaserver.http.Req;
import mediaserver.media.Track;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ProgressListener
    implements ChannelProgressiveFutureListener
{

    private final Range range;

    private final Chunk chunk;

    private final Clock clock;

    private final Req req;

    private final Track track;

    private volatile long lastProgress;

    ProgressListener(Clock clock, Req req, Track track, Range range, Chunk chunk) {

        this.clock = clock;
        this.req = req;
        this.track = track;
        this.range = range;
        this.chunk = chunk;
    }

    @Override
    public void operationComplete(ChannelProgressiveFuture future) {

        Duration streamingTime = Duration.between(req.getTime(), clock.instant());
        SocketAddress remote = future.channel().remoteAddress();
        if (streamingTime.toSeconds() > WARN_THRESHOLD_SECONDS) {
            log.warn("{}: {} -> {} in {} IS A LONG TIME: {}:{} > {} > {}",
                     req.getCtx(), range, chunk, printed(streamingTime), req, req.getSession(), track, remote);
        }
        log.info("{}: {} -> {} in {}: {}:{} > {} > {}",
                 req.getCtx(), range, chunk, printed(streamingTime), req, req.getSession(), track, remote);
    }

    @Override
    public void operationProgressed(ChannelProgressiveFuture future, long progress, long total) {

        try {
            if (progress == lastProgress) {
                log.debug("Repeated: {} {}: {}%", track.getName(), chunk, chunk.perMille(progress) / 10.0);
            } else if (progress < total) {
                log.debug("{} {}: {}%", track.getName(), chunk, chunk.perMille(progress) / 10.0);
            }
        }
        finally {
            lastProgress = progress;
        }
    }

    private static final Logger log = LoggerFactory.getLogger(ProgressListener.class);

    private static final int KILO = 1_000;

    private static final int MEGA = KILO * KILO;

    private static final int WARN_THRESHOLD_SECONDS = 30;

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
