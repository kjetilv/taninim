package taninim.kudu.server;

import module java.base;
import module uplift.asynchttp;

import static com.github.kjetilv.uplift.asynchttp.MainSupport.*;

public record Parameters(
    int port,
    int buffer,
    boolean server
) {
    private static final int PORT_80 = 80;

    private static final int DEFAULT_RESPONSE_LENGTH = 64 * 1_024;

    public static Parameters parse(String[] args) {
        Map<String, String> map = parameterMap(args);
        boolean server = boolArg(map, "server") || possibleIntArg(map, "port").isPresent();
        return new Parameters(
            validatePort(intArg(map, "port", server ? PORT_80 : 0)),
            intArg(map, "buffer", DEFAULT_RESPONSE_LENGTH),
            server
        );
    }
}
