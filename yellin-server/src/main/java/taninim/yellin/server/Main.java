package taninim.yellin.server;

import module java.base;
import com.github.kjetilv.uplift.kernel.Env;
import com.github.kjetilv.uplift.s3.S3Accessor;
import com.github.kjetilv.uplift.synchttp.HttpHandler;
import com.github.kjetilv.uplift.synchttp.Servers;
import com.github.kjetilv.uplift.util.Time;
import com.github.kjetilv.uplift.util.Virtuals;
import taninim.fb.DefaultFbAuthenticator;
import taninim.yellin.DefaultYellin;

import static com.github.kjetilv.uplift.util.MainSupport.*;

class Main {

    void main(String[] args) {

        var parameters = parameterMap(args);
        var s3Accessor = S3Accessor.fromEnvironment(Env.actual(), Virtuals.executor("yellin"));

        HttpHandler handler = new YellinHttpHandler(
            DefaultYellin.create(
                s3Accessor,
                Time.UTC_CLOCK::instant,
                Duration.ofDays(1),
                Duration.ofHours(1),
                new DefaultFbAuthenticator()
            ));

        Servers.await("Yellin", resolvePort(parameters), handler);
    }

    private int resolvePort(Map<String, String> parameters) {
        return validatePort(possibleIntArg(parameters, "port").orElse(8080));
    }
}
