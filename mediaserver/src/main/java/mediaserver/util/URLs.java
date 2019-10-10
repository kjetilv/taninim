package mediaserver.util;

import java.util.HashMap;
import java.util.Map;

public final class URLs {

    private URLs() {
    }

    public static Map<String, String> queryParams(String pars) {
        int index = 0;
        Map<String, String> map = new HashMap<>();
        while (true) {
            int nextPair = pars.indexOf("&", index);
            boolean last = nextPair <= 0;
            if (last) {
                nextPair = pars.length();
            }
            int eqIndex = pars.indexOf("=", index);
            if (eqIndex < 0 || eqIndex > nextPair) {
                throw new IllegalStateException("Expected value for " + pars.substring(index + 1, nextPair));
            }
            map.put(pars.substring(index, eqIndex), pars.substring(eqIndex + 1, nextPair));
            if (last) {
                return map;
            }
            index = nextPair + 1;
        }
    }
}
