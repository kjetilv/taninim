package taninim.music.medias;

import java.io.DataInput;
import java.io.DataOutput;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.kjetilv.uplift.kernel.io.BinaryWritable;
import com.github.kjetilv.uplift.kernel.util.Maps;
import com.github.kjetilv.uplift.uuid.Uuid;

import static com.github.kjetilv.uplift.kernel.io.BytesIO.*;
import static java.util.Objects.requireNonNull;

public record UserAuth(
    String userId,
    Instant expiry,
    Uuid token,
    List<AlbumLease> albumLeases
) implements BinaryWritable, Comparable<UserAuth> {

    static UserAuth from(DataInput input) {
        try {
            String userId = readString(input);
            Instant time = readInstant(input);
            Uuid token = readUuid(input);
            int count = input.readInt();
            List<AlbumLease> albumLeases = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                Uuid uuid = readUuid(input);
                Instant expiry = readInstant(input);
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

    public UserAuth(String userId, Instant expiry, Uuid token, List<AlbumLease> albumLeases) {
        this.userId = requireNonNull(userId, "userId");
        this.expiry = requireNonNull(expiry, "expiry");
        this.token = token == null ? Uuid.random() : token;
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
               writeUuid(output, token) +
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
                .filter(albumLease ->
                    albumLease.expiry().isAfter(time))
                .toList()
        );
    }

    public UserAuth combine(UserAuth update) {
        if (matches(update)) {
            List<AlbumLease> combinedAlbumLeases = Stream.concat(
                albumLeases.stream(),
                update.albumLeases().stream()
            ).toList();
            List<AlbumLease> newestAlbumLeases = Maps.groupBy(combinedAlbumLeases, AlbumLease::albumId)
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
            Collection<Uuid> clearedUuids =
                userAuth.albumLeases().stream().map(AlbumLease::albumId).collect(Collectors.toSet());
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

    public record AlbumLease(
        Uuid albumId,
        Instant expiry
    ) implements BinaryWritable {

        public AlbumLease(Uuid albumId, Instant expiry) {
            this.albumId = requireNonNull(albumId, "albumId");
            this.expiry = requireNonNull(expiry, "expiry");
        }

        @Override
        public int writeTo(DataOutput dos) {
            return writeUuid(dos, albumId) + writeEpoch(dos, expiry);
        }

        public boolean isFor(Uuid uuid) {
            return albumId.equals(uuid);
        }

        public boolean validAt(Instant time) {
            return expiry.isAfter(requireNonNull(time, "time"));
        }
    }
}
