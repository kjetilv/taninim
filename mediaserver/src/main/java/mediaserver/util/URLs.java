package mediaserver.util;

import mediaserver.http.QPar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public final class URLs {

    private static final Logger log = LoggerFactory.getLogger(URLs.class);

    private URLs() {

    }

    public static Map<QPar, Collection<String>> queryParams(String pars) {
        if (pars == null || pars.isBlank()) {
            return Collections.emptyMap();
        }
        int index = 0;
        Map<QPar, Collection<String>> map = new EnumMap<>(QPar.class);
        while (true) {
            int nextPair = pars.indexOf('&', index);
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

    private static void expandMap(String pars, Map<QPar, Collection<String>> map, int index, int nextPair) {

        eqIndex(pars, index, nextPair).ifPresentOrElse(
            eqIndex ->
                QPar.get(pars.substring(index, eqIndex))
                    .ifPresentOrElse(
                        param ->
                            map.computeIfAbsent(param, __ -> new ArrayList<>()).add(
                                pars.substring(eqIndex + 1, nextPair)),
                        () ->
                            log.debug("Unknown parameter: {}", pars.substring(index, eqIndex))),
            logMalformed(pars, index, nextPair));
    }

    private static OptionalInt eqIndex(String pars, int index, int nextPair) {

        int eqIndex = pars.indexOf('=', index);
        if (eqIndex < 0 || eqIndex > nextPair) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(eqIndex);
    }

    private static Runnable logMalformed(String pars, int start, int end) {

        return () ->
            log.warn("Expected value for {}", pars.substring(start + 1, end));
    }

}
