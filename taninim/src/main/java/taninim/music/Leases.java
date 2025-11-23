package taninim.music;

import module java.base;
import com.github.kjetilv.uplift.hash.Hash;
import com.github.kjetilv.uplift.hash.HashKind.K128;
import taninim.util.Maps;

import static java.util.Objects.requireNonNull;

public record Leases(
    Hash<K128> token,
    List<Lease> leases
) {

    public Leases(Hash<K128> token) {
        this(token, null);
    }

    public Leases(Hash<K128> token, List<Lease> leases) {
        this.token = requireNonNull(token, "token");
        this.leases = leases == null || leases.isEmpty()
            ? Collections.emptyList()
            : sorted(leases);
    }

    public boolean validFor(Hash<K128> trackUUID, Instant time) {
        return leases.stream().anyMatch(lease ->
            lease.isFor(trackUUID) && lease.validAt(time));
    }

    public boolean stillActiveAt(Instant time) {
        return leases().isEmpty() || leases().stream().anyMatch(lease -> lease.validAt(time));
    }

    public Leases validAt(Instant time) {
        if (leases.isEmpty()) {
            return this;
        }
        return new Leases(
            token,
            this.leases()
                .stream()
                .filter(lease -> lease.validAt(time))
                .toList()
        );
    }

    public List<String> toLines() {
        return leases().stream()
            .map(Leases.Lease::toLine)
            .toList();
    }

    Leases withTracks(List<? extends Hash<K128>> tracks, Instant lapse) {
        return new Leases(
            token,
            Stream.concat(
                    leases.stream(),
                    leases(tracks, lapse)
                )
                .toList()
        );
    }

    private static List<Lease> sorted(List<Lease> leases) {
        return Maps.groupBy(leases, Lease::track)
            .values()
            .stream()
            .map(entries ->
                entries.stream()
                    .max(Comparator.comparing(Lease::lapse)))
            .flatMap(Optional::stream)
            .toList();
    }

    private static Stream<Lease> leases(Collection<? extends Hash<K128>> tracks, Instant lapse) {
        return tracks.stream()
            .map(track ->
                new Lease(track, lapse));
    }

    public record Lease(
        Hash<K128> track,
        Instant lapse
    ) {

        public Lease {
            requireNonNull(track, "track");
            requireNonNull(lapse, "lapse");
        }

        private String toLine() {
            return MessageFormat.format(
                "{0} {1}",
                track().digest(),
                String.valueOf(lapse().getEpochSecond())
            );
        }

        private boolean isFor(Hash<K128> uuid) {
            return uuid.equals(track);
        }

        private boolean validAt(Instant time) {
            return lapse.isAfter(time);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + token + " " + leases.size() + "]";
    }
}
