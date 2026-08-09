package taninim.util;

import module java.base;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record VirtualRun(String name, Runnable runnable) {

    public static void join(String name, Runnable runnable) {
        new VirtualRun(name, runnable).join();
    }

    private static final Logger log = LoggerFactory.getLogger(VirtualRun.class);

    public CompletableFuture<?> start() {
        return CompletableFuture.runAsync(runnable, executor(name))
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

    private static final ThreadFactory FACTORY = Thread.ofVirtual().factory();

    private static ExecutorService executor(String name) {
        return Executors.newThreadPerTaskExecutor(virtualThreadFactory(name));
    }

    private static ThreadFactory virtualThreadFactory(String name) {
        return runnable -> thread(name, runnable);
    }

    private static Thread thread(String name, Runnable runnable) {
        var virtualThread = FACTORY.newThread(runnable);
        virtualThread.setName(name);
        return virtualThread;
    }
}
