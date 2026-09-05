package taninim.util;

import module java.base;
import com.github.kjetilv.uplift.util.Virtuals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record VirtualRun(String name, Runnable runnable) {

    private static final Logger log = LoggerFactory.getLogger(VirtualRun.class);

    public static void join(String name, Runnable runnable) {
        new VirtualRun(name, runnable).join();
    }

    public CompletableFuture<?> start() {
        return CompletableFuture.runAsync(
                runnable,
                Virtuals.executor(name)
            )
            .whenComplete((_, throwable) -> {
                if (throwable != null) {
                    log.info("Completed exceptionally: {}", name, throwable);
                } else {
                    log.info("Completed: {}", name);
                }
            });
    }

    public void join() {
        start().join();
    }
}
