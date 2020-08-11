package mediaserver.debug;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import mediaserver.gui.TemplateEnabled;
import mediaserver.http.Handling;
import mediaserver.http.Req;
import mediaserver.http.Route;
import mediaserver.templates.TPar;
import mediaserver.toolkit.Templater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Debug extends TemplateEnabled {
    
    private static final Logger log = LoggerFactory.getLogger(Debug.class);
    
    private final Supplier<Collection<Exchange>> latestExchanges;
    
    public Debug(
        Route route,
        Templater templater,
        Supplier<Collection<Exchange>> latestExchanges
    ) {
        super(route, templater);
        this.latestExchanges = Objects.requireNonNull(latestExchanges, "latestExchanges");
    }
    
    @Override
    protected Handling handle(Req req) {
        log.info("Request receieved @ {}: {}", req, req.getRequest());
        Map<String, List<Exchange>> exchanges = latestExchanges.get().stream()
            .sorted(
                Comparator.comparingLong(Exchange::getSequenceNo).reversed())
            .collect(Collectors.groupingBy(
                Exchange::getSession,
                Collectors.toCollection(ArrayList::new)));
        return respondHtml(req, getTemplate(DEBUG_PAGE).add(
            TPar.exchanges,
            exchanges.entrySet()));
    }
}
