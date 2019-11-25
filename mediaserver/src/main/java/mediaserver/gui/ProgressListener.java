package mediaserver.gui;

import io.netty.channel.ChannelProgressiveFuture;
import io.netty.channel.ChannelProgressiveFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;

final class ProgressListener implements ChannelProgressiveFutureListener {

    private static final Logger log = LoggerFactory.getLogger(ProgressListener.class);

    private final Object source;

    private static final int KILO = 1_000;

    private static final int MEGA = 1_000_000;

    ProgressListener(Object source) {
        this.source = source;
    }

    @Override
    public void operationProgressed(ChannelProgressiveFuture future, long progress, long total) {

        SocketAddress remote = future.channel().remoteAddress();
        if (total < 0) { // total unknown
            log.info("{} {}/? => {}", source, progress, remote);
        } else {
            long kilos = total / KILO;
            boolean lotsaKilos = kilos > KILO;
            log.info("{} {}%/{}{} => {}", source,
                100 * progress / total,
                lotsaKilos ? total / MEGA : kilos,
                lotsaKilos ? "m" : "k",
                remote);
        }
    }

    @Override
    public void operationComplete(ChannelProgressiveFuture future) {
        log.info("{} OK {}", future.channel(), source);
    }
}
