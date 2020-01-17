package mediaserver.debug;

import mediaserver.gui.TemplateEnabled;
import mediaserver.http.Handling;
import mediaserver.http.Page;
import mediaserver.http.WebPath;
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
    public Handling handleRequest(WebPath webPath) {

        log.info("Request receieved @ {}: {}", webPath, webPath.getRequest());
        Map<String, List<Exchange>> exchanges = latestExchanges.get().stream()
            .sorted(
                Comparator.comparingLong(Exchange::getSequenceNo).reversed())
            .collect(Collectors.groupingBy(
                Exchange::getSession,
                Collectors.toCollection(ArrayList::new)));
        return respondHtml(webPath, getTemplate(DEBUG_PAGE).add(
            "exchanges",
            exchanges.entrySet()));
    }
}
