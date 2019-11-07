package mediaserver.gui;

import io.netty.channel.ChannelProgressiveFuture;
import io.netty.channel.ChannelProgressiveFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ProgressListener implements ChannelProgressiveFutureListener {

    private static final Logger log = LoggerFactory.getLogger(ProgressListener.class);

    private final Object source;

    ProgressListener(Object source) {
        this.source = source;
    }

    @Override
    public void operationProgressed(ChannelProgressiveFuture future, long progress, long total) {
        if (total < 0) { // total unknown
            log.info("{} {}% => {}", source, progress, future.channel().remoteAddress());
        } else {
            log.info("{} {}% => {}", source, 100 * progress / total, future.channel().remoteAddress());
        }
    }

    @Override
    public void operationComplete(ChannelProgressiveFuture future) {
        log.info("{} OK {}", future.channel(), source);
    }
}
