package taninim.music.legal;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.kjetilv.uplift.kernel.uuid.Uuid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import taninim.music.Archives;
import taninim.music.Archives.ArchivedRecord;
import taninim.music.Leases;
import taninim.music.LeasesPath;
import taninim.music.LeasesRegistry;
import taninim.music.Period;

import static java.util.Objects.requireNonNull;

public final class ArchivedLeasesRegistry implements LeasesRegistry {

    private static final Logger log = LoggerFactory.getLogger(ArchivedLeasesRegistry.class);

    private final Duration leaseDuration;

    private final Supplier<Instant> time;

    private final Executor executor;

    private final Archives archives;

    public ArchivedLeasesRegistry(
        Archives archives, Duration leaseDuration, Supplier<Instant> time, Executor executor
    ) {
        this.archives = requireNonNull(archives, "archives");
        this.leaseDuration = requireNonNull(leaseDuration, "leaseLimit");
        this.time = requireNonNull(time, "clock");
        this.executor = executor;
    }

    @Override
    public Optional<LeasesPath> getActive(Uuid token) {
        Instant time = this.time.get();
        Period period = Period.starting(time).ofLength(leaseDuration);
        try (Stream<String> pathsForToken = archives.retrievePaths(LEASE_PREFIX, recordFor(token))) {
            return pathsForToken.max(BY_EPOCH_HOUR).flatMap(path ->
                archives.retrieveRecord(path)
                    .flatMap(archivedRecord ->
                        leases(token, archivedRecord).validAt(time))
                    .filter(leases ->
                        leases.stillActiveAt(period.start()))
                    .flatMap(leases ->
                        leases.validAt(time))
                    .map(leases -> new LeasesPath(leases, period)));
        }
    }

    @Override
    public Optional<LeasesPath> setActive(Leases leases, Period period) {
        Instant time = this.time.get();
        try {
            return leases.validAt(time).map(valid ->
                new LeasesPath(valid, period)).map(leasesPath -> {
                ArchivedRecord archivedRecord = recordOf(leasesPath);
                log.info(
                    "Creating leases @ {}: {} bytes, {} lines",
                    archivedRecord.path(),
                    archivedRecord.body().length(),
                    archivedRecord.contents().size()
                );
                archives.storeRecord(archivedRecord);
                return leasesPath;
            });
        } finally {
            deleteOutdated(time);
        }
    }

    private void deleteOutdated(Instant time) {
        Collection<Long> active = Period.starting(time).epochHoursBack(leaseDuration).collect(Collectors.toSet());
        if (executor == null) {
            doDeleteOutdated(active);
        } else {
            CompletableFuture.runAsync(() -> doDeleteOutdated(active), executor);
        }
    }

    private void doDeleteOutdated(Collection<Long> activeEpochHours) {
        try (
            Stream<String> records = archives.retrievePaths(
                LEASE_PREFIX,
                path -> !activeEpochHours.contains(epochHour(path))
            )
        ) {
            List<String> paths = records.toList();
            if (!paths.isEmpty()) {
                archives.clearRecords(paths);
                log.debug(
                    "{} outdated prefixes gone, {} still active",
                    (Supplier<Integer>) paths::size,
                    (Supplier<Integer>) activeEpochHours::size
                );
            }
        }
    }

    private static final String LEASE_PREFIX = "lease-";

    private static final String LEASE_SUFFIX = ".lease.txt";

    private static final Comparator<String> BY_EPOCH_HOUR = Comparator.comparing(ArchivedLeasesRegistry::epochHour);

    private static ArchivedRecord recordOf(LeasesPath leasesPath) {
        return new ArchivedRecord(leasesPath.toPath(), leasesPath.leases().toLines());
    }

    private static Predicate<String> recordFor(Uuid token) {
        String endPath = token.digest() + LEASE_SUFFIX;
        return path -> path.endsWith(endPath);
    }

    private static Leases leases(Uuid token, ArchivedRecord archivedRecord) {
        return new Leases(token, leases(archivedRecord));
    }

    private static List<Leases.Lease> leases(ArchivedRecord archivedRecord) {
        return archivedRecord.contents().stream()
            .map(String::trim)
            .filter(string -> !string.isBlank())
            .map(line ->
                line.split(" ", 2)).map(tokenLease -> new Leases.Lease(
                Uuid.from(tokenLease[0]),
                Instant.ofEpochSecond(Long.parseLong(tokenLease[1]))
            )).toList();
    }

    private static Long epochHour(String path) {
        String hourstamp = path.substring(LEASE_PREFIX.length());
        int dashindex = hourstamp.indexOf('-');
        return Long.parseLong(hourstamp.substring(0, dashindex));
    }
}
