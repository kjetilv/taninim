package mediaserver.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class QPars implements Function<QPar, Optional<UUID>> {

    private static final Logger log = LoggerFactory.getLogger(QPars.class);

    private final Map<QPar, String> pars;

    QPars(Map<QPar, String> pars) {

        this.pars = pars;
    }

    @Override
    public Optional<UUID> apply(QPar par) {

        Optional<String> value = Optional.ofNullable(pars.get(par));
        try {
            return value.map(UUID::fromString);
        } catch (Exception e) {
            log.debug("Invalid UUID received: {}", value.get(), e);
            return Optional.empty();
        }
    }

    @Override
    public String toString() {

        return getClass().getSimpleName() + "[" +
            pars.entrySet().stream()
                .map(e ->
                    e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(" ")) +
            "]";
    }
}
