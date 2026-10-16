package taninim.server;

import module java.base;

import static com.github.kjetilv.uplift.util.MainSupport.*;

public record Parameters(int port, int buffer) {

    public static Parameters parse(String[] args) {
        var map = parameterMap(args);
        var server = boolArg(map, "server") || possibleIntArg(map, "port").isPresent();
        return new Parameters(
            validatePort(intArg(map, "port", server ? PORT_80 : 0)),
            intArg(map, "buffer", DEFAULT_RESPONSE_LENGTH)
        );
    }

    private static final int PORT_80 = 80;

    private static final int DEFAULT_RESPONSE_LENGTH = 64 * 1_024;

    private static int validatePort(int port) {
        if (port > PORTS_AVAILABLE) {
            throw new IllegalStateException("Invalid port: " + port);
        }
        return port > 0 ? port : PORT_80;
    }
}
