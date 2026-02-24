package taninim.music.medias;

import module java.base;
import com.github.kjetilv.uplift.hash.Hash;
import com.github.kjetilv.uplift.kernel.io.BinaryWritable;
import taninim.util.Maps;

import static com.github.kjetilv.uplift.hash.HashKind.K128;
import static com.github.kjetilv.uplift.kernel.io.BytesIO.*;
import static java.util.Objects.requireNonNull;

public record UserAuth(
    String userId,
    Instant expiry,
    Hash<K128> token,
    List<AlbumLease> albumLeases
) implements BinaryWritable, Comparable<UserAuth> {

    static UserAuth from(DataInput input) {
        try {
            var userId = readString(input);
            var time = readInstant(input);
            var token = K128.from(input);
            var count = input.readInt();
            List<AlbumLease> albumLeases = new ArrayList<>(count);
            for (var i = 0; i < count; i++) {
                var uuid = K128.from(input);
                var expiry = readInstant(input);
                albumLeases.add(new AlbumLease(uuid, expiry));
            }
            return new UserAuth(userId, time, token, albumLeases);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public UserAuth(String userId, Instant expiry) {
        this(userId, expiry, null);
    }

    public UserAuth(String userId, Instant expiry, List<AlbumLease> albumLeases) {
        this(userId, expiry, null, albumLeases);
    }

    public UserAuth(String userId, Instant expiry, Hash<K128> token, List<AlbumLease> albumLeases) {
        this.userId = requireNonNull(userId, "userId");
        this.expiry = requireNonNull(expiry, "expiry");
        this.token = token == null ? K128.random() : token;
        if (this.userId.isBlank()) {
            throw new IllegalArgumentException(this + ": No user id");
        }
        this.albumLeases = albumLeases == null || albumLeases.isEmpty()
            ? Collections.emptyList()
            : Maps.noDuplicates(albumLeases, AlbumLease::albumId);
    }

    @Override
    public int writeTo(DataOutput output) {
        return writeString(output, userId) +
               writeInstant(output, expiry) +
               writeHash128(output, token) +
               writeWritables(output, albumLeases);
    }

    @Override
    public int compareTo(UserAuth o) {
        return Comparator.comparing(UserAuth::expiry).compare(this, o);
    }

    public UserAuth withoutExpiredLeasesAt(Instant time) {
        return new UserAuth(
            userId(),
            expiry(),
            token(),
            albumLeases().stream()
                .filter(activeAt(time))
                .toList()
        );
    }

    public UserAuth combine(UserAuth update) {
        if (matches(update)) {
            var combinedAlbumLeases = Stream.concat(
                    albumLeases.stream(),
                    update.albumLeases()
                        .stream()
                )
                .toList();
            var newestAlbumLeases = Maps.groupBy(combinedAlbumLeases, AlbumLease::albumId)
                .values()
                .stream()
                .map(leaseGroup ->
                    leaseGroup.stream()
                        .min(Comparator.comparing(AlbumLease::expiry)))
                .flatMap(Optional::stream)
                .toList();
            return new UserAuth(
                update.userId(),
                update.expiry(),
                update.token(),
                newestAlbumLeases
            );
        }
        throw new IllegalStateException(this + " != " + update);
    }

    public UserAuth without(UserAuth userAuth) {
        if (matches(userAuth)) {
            Collection<Hash<K128>> clearedUuids =
                userAuth.albumLeases()
                    .stream()
                    .map(AlbumLease::albumId)
                    .collect(Collectors.toSet());
            return new UserAuth(
                userId,
                expiry,
                token,
                albumLeases.stream()
                    .filter(existing ->
                        !clearedUuids.contains(existing.albumId()))
                    .toList()
            );
        }
        throw new IllegalStateException(this + " != " + userAuth);
    }

    public boolean matches(UserAuth auth) {
        return matches(auth.userId());
    }

    public boolean matches(String userId) {
        return Objects.equals(this.userId(), userId);
    }

    public boolean validAt(Instant time) {
        return expiry.isAfter(requireNonNull(time, "time"));
    }

    private static Predicate<AlbumLease> activeAt(Instant time) {
        return albumLease ->
            time.isBefore(albumLease.expiry());
    }

    public record AlbumLease(Hash<K128> albumId, Instant expiry) implements BinaryWritable {

        public AlbumLease(Hash<K128> albumId, Instant expiry) {
            this.albumId = requireNonNull(albumId, "albumId");
            this.expiry = requireNonNull(expiry, "expiry");
        }

        @Override
        public int writeTo(DataOutput dos) {
            return writeHash128(dos, albumId) + writeEpoch(dos, expiry);
        }

        public boolean isFor(Hash<K128> uuid) {
            return albumId.equals(uuid);
        }

        public boolean validAt(Instant time) {
            return expiry.isAfter(requireNonNull(time, "time"));
        }
    }
}
