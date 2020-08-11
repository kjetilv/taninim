package mediaserver.stream;

import java.net.SocketAddress;
import java.time.Clock;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

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
        if (log.isDebugEnabled()) {
            log.debug("{} {}: {}%", albumTrack.getTrack().getName(), chunk, chunk.getPerc(progress, 2));
        }
    }
}
