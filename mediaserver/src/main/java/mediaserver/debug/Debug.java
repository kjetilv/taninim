package mediaserver.debug;

import mediaserver.gui.TemplateEnabled;
import mediaserver.http.Handling;
import mediaserver.http.Page;
import mediaserver.http.Req;
import mediaserver.templates.TPar;
import mediaserver.toolkit.Templater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class Debug extends TemplateEnabled {

    private static final Logger log = LoggerFactory.getLogger(Debug.class);

    private final Supplier<Collection<Exchange>> latestExchanges;

    public Debug(Templater templater, Supplier<Collection<Exchange>> latestExchanges) {

        super(templater, Page.DEBUG);
        this.latestExchanges = latestExchanges;
    }

    @Override
    protected Handling handleRequest(Req req) {

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
