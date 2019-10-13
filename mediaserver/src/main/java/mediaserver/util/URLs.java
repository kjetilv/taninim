package mediaserver.util;

import mediaserver.gui.QPar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.Map;
import java.util.OptionalInt;
import java.util.function.Consumer;

public final class URLs {

    private static final Logger log = LoggerFactory.getLogger(URLs.class);

    private URLs() {
    }

    public static Map<QPar, String> queryParams(String pars) {
        int index = 0;
        Map<QPar, String> map = new EnumMap<>(QPar.class);
        while (true) {
            int nextPair = pars.indexOf("&", index);
            boolean last = nextPair <= 0;
            if (last) {
                nextPair = pars.length();
            }
            expandMap(pars, map, index, nextPair);
            if (last) {
                return map;
            }
            index = nextPair + 1;
        }
    }

    private static void expandMap(String pars, Map<QPar, String> map, int index, int nextPair) {
        eqIndex(pars, index, nextPair).ifPresentOrElse(
            eqIndex ->
                QPar.get(pars.substring(index, eqIndex)).ifPresentOrElse(
                    expandMap(map, pars, eqIndex + 1, nextPair),
                    logUnknown(pars, index, eqIndex)),
            logMalformed(pars, index, nextPair));
    }

    private static OptionalInt eqIndex(String pars, int index, int nextPair) {
        int eqIndex = pars.indexOf("=", index);
        if (eqIndex < 0 || eqIndex > nextPair) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(eqIndex);
    }

    private static Consumer<QPar> expandMap(
        Map<QPar, String> map,
        String string,
        int start,
        int end
    ) {
        return param ->
            map.put(param, string.substring(start, end));
    }

    private static Runnable logMalformed(String pars, int start, int end) {
        return () ->
            log.warn("Expected value for {}", pars.substring(start + 1, end));
    }

    private static Runnable logUnknown(String string, int start, int end) {
        return () ->
            log.debug("Unknown parameter: {}", string.substring(start, end));
    }
}
