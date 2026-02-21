package taninim.music.medias;

import module java.base;
import com.github.kjetilv.uplift.kernel.io.BinaryWritable;
import com.github.kjetilv.uplift.kernel.io.BytesIO;
import taninim.util.Maps;

public record UserAuths(List<UserAuth> auths) implements BinaryWritable {

    public static UserAuths from(DataInput input) {
        int count;
        try {
            count = input.readInt();
        } catch (EOFException e) {
            return new UserAuths();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return new UserAuths(IntStream.range(0, count).mapToObj(__ -> UserAuth.from(input))
            .toList());
    }

    public UserAuths() {
        this(null);
    }

    public UserAuths(List<UserAuth> auths) {
        this.auths = auths == null || auths.isEmpty()
            ? Collections.emptyList()
            : auths;
    }

    @Override
    public int writeTo(DataOutput dos) {
        return auths.isEmpty() ? 0 : BytesIO.writeWritables(dos, auths);
    }

    public UserAuths without(UserAuth userAuth) {
        return new UserAuths(auths.stream()
            .map(existing ->
                existing.matches(userAuth) ? existing.without(userAuth) : existing)
            .toList());
    }

    public UserAuths updatedWith(UserAuth userAuth, Instant time) {
        var authList = Stream.concat(auths.stream(), Stream.of(userAuth))
            .map(auth ->
                auth.withoutExpiredLeasesAt(time))
            .toList();
        var grouped = Maps.groupBy(authList, UserAuth::userId);
        var auths = grouped.entrySet()
            .stream()
            .map(entry ->
                auths(entry.getValue())
                    .orElseThrow(() ->
                        new IllegalStateException("Execpted auths for " + entry.getKey())))
            .toList();
        return new UserAuths(auths);
    }

    public Optional<UserAuth> forUser(String userId) {
        return auths.stream()
            .filter(auth ->
                Objects.equals(auth.userId(), userId))
            .findFirst();
    }

    public Optional<UserAuth> requestedAuth(UserRequest userRequest) {
        return auths.stream()
            .filter(userRequest::matches)
            .findFirst();
    }

    private static Optional<UserAuth> auths(List<UserAuth> auths) {
        return auths.stream()
            .sorted(Comparator.naturalOrder())
            .reduce(UserAuth::combine);
    }

}
