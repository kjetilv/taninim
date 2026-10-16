package taninim.server;

import module java.base;
import com.github.kjetilv.uplift.kernel.Env;
import com.github.kjetilv.uplift.s3.S3Accessor;
import com.github.kjetilv.uplift.util.Virtuals;

public abstract class AbstractServer {

    private final Parameters parameters;

    private final S3Accessor s3Accessor;

    protected AbstractServer(String name, String... args) {
        parameters = Parameters.parse(args);
        s3Accessor = S3Accessor.fromEnvironment(
            Env.actual(),
            Virtuals.executor(name)
        );
    }

    public abstract void run();

    protected Parameters parameters() {
        return parameters;
    }

    protected S3Accessor s3Accessor() {
        return s3Accessor;
    }
}
