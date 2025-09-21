package taninim.music.legal;

import module java.base;
import module taninim.taninim;
import module uplift.flogs;
import module uplift.uuid;
import org.slf4j.Logger;
import taninim.music.Archives.ArchivedRecord;

import static java.util.Objects.requireNonNull;

public final class ArchivedLeasesRegistry implements LeasesRegistry {

    private static final Logger log = LoggerFactory.getLogger(ArchivedLeasesRegistry.class);

    private final Duration leaseDuration;

    private final Supplier<Instant> time;

    private final Archives archives;

    public ArchivedLeasesRegistry(
        Archives archives,
        Duration leaseDuration,
        Supplier<Instant> time
    ) {
        this.archives = requireNonNull(archives, "archives");
        this.leaseDuration = requireNonNull(leaseDuration, "leaseLimit");
        this.time = requireNonNull(time, "clock");
    }

    @Override
    public Optional<LeasesPath> getActive(Uuid token) {
        Instant time = this.time.get();
        LeasePeriod leasePeriod = LeasePeriod.starting(time).ofLength(leaseDuration);
        try (Stream<String> pathsForToken = archives.retrievePaths(LEASE_PREFIX, recordFor(token))) {
            return pathsForToken.max(BY_EPOCH_HOUR).flatMap(path ->
                archives.retrieveRecord(path)
                    .map(archivedRecord ->
                        leases(token, archivedRecord).validAt(time))
                    .filter(leases ->
                        leases.stillActiveAt(leasePeriod.start()))
                    .map(leases ->
                        leases.validAt(time))
                    .map(leases -> new LeasesPath(leases, leasePeriod)));
        }
    }

    @Override
    public LeasesPath setActive(Leases leases, LeasePeriod leasePeriod) {
        Instant time = this.time.get();
        try {
            Leases valid = leases.validAt(time);
            LeasesPath leasesPath = new LeasesPath(valid, leasePeriod);
            return stored(leasesPath);
        } finally {
            deleteOutdated(time);
        }
    }

    private LeasesPath stored(LeasesPath leasesPath) {
        ArchivedRecord archivedRecord = recordOf(leasesPath);
        log.info(
            "Creating leases @ {}: {} bytes, {} lines",
            archivedRecord.path(),
            archivedRecord.body().length(),
            archivedRecord.contents().size()
        );
        archives.storeRecord(archivedRecord);
        return leasesPath;
    }

    private void deleteOutdated(Instant time) {
        Collection<Long> active = LeasePeriod.starting(time).epochHoursBack(leaseDuration)
            .collect(Collectors.toSet());
        try (
            Stream<String> records = archives.retrievePaths(
                LEASE_PREFIX,
                path -> !active.contains(epochHour(path))
            )
        ) {
            List<String> paths = records.toList();
            if (!paths.isEmpty()) {
                archives.clearRecords(paths);
                log.debug(
                    "{} outdated prefixes gone, {} still active",
                    (Supplier<Integer>) paths::size,
                    (Supplier<Integer>) active::size
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
        return archivedRecord.contents()
            .stream()
            .map(String::trim)
            .filter(string -> !string.isBlank())
            .map(line ->
                line.split(" ", 2))
            .map(tokenLease ->
                new Leases.Lease(
                    Uuid.from(tokenLease[0]),
                    Instant.ofEpochSecond(Long.parseLong(tokenLease[1]))
                ))
            .toList();
    }

    private static Long epochHour(String path) {
        String hourstamp = path.substring(LEASE_PREFIX.length());
        int dashindex = hourstamp.indexOf('-');
        return Long.parseLong(hourstamp.substring(0, dashindex));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + leaseDuration + " from " + archives + "]";
    }
}
