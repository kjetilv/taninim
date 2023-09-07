package taninim.music.medias;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.EOFException;
import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.github.kjetilv.uplift.kernel.io.BinaryWritable;
import com.github.kjetilv.uplift.kernel.io.BytesIO;
import com.github.kjetilv.uplift.kernel.util.Maps;

public record UserAuths(List<UserAuth> userAuths) implements BinaryWritable {

    public static UserAuths from(DataInput input) {
        int count;
        try {
            count = input.readInt();
        } catch (EOFException e) {
            return new UserAuths();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return new UserAuths(IntStream.range(0, count).mapToObj(__ -> UserAuth.from(input)).toList());
    }

    public UserAuths() {
        this(null);
    }

    public UserAuths(List<UserAuth> userAuths) {
        this.userAuths = userAuths == null || userAuths.isEmpty()
            ? Collections.emptyList()
            : userAuths;
    }

    @Override
    public int writeTo(DataOutput dos) {
        return userAuths.isEmpty() ? 0 : BytesIO.writeWritables(dos, userAuths);
    }

    public UserAuths without(UserAuth userAuth) {
        return new UserAuths(userAuths.stream()
            .map(existing ->
                existing.matches(userAuth) ? existing.without(userAuth) : existing)
            .toList());
    }

    public UserAuths updatedWith(UserAuth userAuth, Instant time) {
        List<UserAuth> authList = Stream.concat(userAuths.stream(), Stream.of(userAuth))
            .map(auth ->
                auth.withoutExpiredLeasesAt(time))
            .toList();
        Map<String, List<UserAuth>> grouped = Maps.groupBy(authList, UserAuth::userId);
        return new UserAuths(grouped.entrySet()
            .stream()
            .map(entry ->
                auths(entry.getKey(), entry.getValue()))
            .toList());
    }

    private static UserAuth auths(String name, List<UserAuth> auths) {
        return auths.stream()
            .sorted(Comparator.naturalOrder())
            .reduce(UserAuth::combine)
            .orElseThrow(() ->
                new IllegalStateException("Execpted auths for " + name));
    }

    public Optional<UserAuth> forUser(String userId) {
        return find(auth -> Objects.equals(auth.userId(), userId));
    }

    public Optional<UserAuth> forAuth(UserRequest userRequest) {
        return find(userRequest::matches);
    }

    private Optional<UserAuth> find(Predicate<? super UserAuth> userAuthPredicate) {
        return userAuths.stream()
            .filter(userAuthPredicate)
            .findFirst();
    }
}
