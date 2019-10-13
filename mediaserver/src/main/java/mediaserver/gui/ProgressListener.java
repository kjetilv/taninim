package mediaserver.gui;

import io.netty.channel.ChannelProgressiveFuture;
import io.netty.channel.ChannelProgressiveFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

final class ProgressListener implements ChannelProgressiveFutureListener {

    private static final Logger log = LoggerFactory.getLogger(ProgressListener.class);

    private final File file;

    ProgressListener(File file) {
        this.file = file;
    }

    @Override
    public void operationProgressed(ChannelProgressiveFuture future, long progress, long total) {
        if (total < 0) { // total unknown
            log.info("{} {} => {}", future.channel(), file, progress);
        } else {
            log.info("{} {} => {}%", future.channel(), file, 100 * progress / total);
        }
    }

    @Override
    public void operationComplete(ChannelProgressiveFuture future) {
        log.info("{} OK {}", future.channel(), file);
    }
}
