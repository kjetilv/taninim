package taninim.fb;

import module java.base;

public final class FbSec {

    public static Supplier<char[]> secretsProvider() {
        return () -> {
            String fbSec = System.getProperty(FB_SEC);
            if (set(fbSec)) {
                return fbSec.toCharArray();
            }
            String envSec = System.getenv(FB_SEC);
            if (set(envSec)) {
                return envSec.toCharArray();
            }
            throw new IllegalStateException(FB_SEC + " not set");
        };
    }

    private FbSec() {
    }

    private static final String FB_SEC = "fbSec";

    private static boolean set(String fbSec) {
        return fbSec != null && !fbSec.isBlank();
    }
}
