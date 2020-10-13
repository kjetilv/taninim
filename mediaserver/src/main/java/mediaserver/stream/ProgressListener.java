package mediaserver.stream;

import java.net.SocketAddress;
import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.atomic.LongAdder;

import io.netty.channel.ChannelProgressiveFuture;
import io.netty.channel.ChannelProgressiveFutureListener;
import mediaserver.http.Req;
import mediaserver.media.AlbumTrack;
import mediaserver.media.Track;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ProgressListener implements ChannelProgressiveFutureListener {

    private static final Logger log = LoggerFactory.getLogger(ProgressListener.class);

    private final Range range;

    private final Chunk chunk;

    private final Clock clock;

    private final Req req;

    private final AlbumTrack albumTrack;

    private final LongAdder progressions = new LongAdder();

    private double lastPerc = Double.MIN_VALUE;

    ProgressListener(Clock clock, Req req, AlbumTrack albumTrack, Range range, Chunk chunk) {
        this.clock = clock;
        this.req = req;
        this.albumTrack = albumTrack;
        this.range = range;
        this.chunk = chunk;

        if (log.isInfoEnabled()) {
            Track track = albumTrack.getTrack();
            log.info("{}: {} -> {}: {} {}",
                this.req.getCtx(), this.range, this.chunk, track.getPrettyTrackNo(), track.getName());
        }
    }

    @Override
    public void operationComplete(ChannelProgressiveFuture future) {
        Duration dur = Duration.between(req.getTime(), clock.instant());
        SocketAddress remote = future.channel().remoteAddress();
        if (log.isInfoEnabled()) {
            log.info("{}: {} -> {} in {}: {}:{} > {} > {}",
                req.getCtx(), range, chunk, dur, req, req.getSession(), albumTrack.getTrack().getName(), remote);
        }
    }

    @SuppressWarnings("MagicNumber")
    @Override
    public void operationProgressed(ChannelProgressiveFuture future, long progress, long total) {
        try {
            long l = progressions.longValue();
            double perc = chunk.getPerc(progress, 2);
            if (l == 0L || lastPerc >= 0.1d && perc - lastPerc >= PERC_LOGGABLE && log.isDebugEnabled()) {
                String name = albumTrack.getTrack().getName();
                double loggablePerc = Math.max(0.1d, perc);
                if (crosses(perc, lastPerc, 50, 80)) {
                    log.info("{} {}: {} / {}%", name, chunk, l, loggablePerc);
                } else {
                    log.debug("{} {}: {} / {}%", name, chunk, l, loggablePerc);
                }
                lastPerc = perc;
            }
        } finally {
            progressions.increment();
        }
    }

    private static boolean crosses(double perc, double lastPerc, int... milestones) {
        return Arrays.stream(milestones)
            .anyMatch(milestone -> lastPerc < milestone && perc >= milestone);
    }

    private static final int PERC_LOGGABLE = 5;
}
