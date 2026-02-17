import module java.base;
import com.github.kjetilv.uplift.kernel.Env;
import com.github.kjetilv.uplift.s3.S3Accessor;
import taninim.music.Archives;
import taninim.music.legal.S3Archives;
import taninim.music.medias.UserAuth;
import taninim.music.medias.UserAuths;

import static com.github.kjetilv.uplift.flogs.Flogs.initialize;

@SuppressWarnings("UseOfSystemOutOrSystemErr")
void main() {
    initialize();

    var s3Accessor = S3Accessor.fromEnvironment(Env.actual(), Executors.newVirtualThreadPerTaskExecutor());
    var archives = S3Archives.create(s3Accessor);
    System.out.println("auth-digest.bin:");
    s3Accessor.stream("auth-digest.bin")
        .map(inputStream ->
            UserAuths.from(new DataInputStream(inputStream)))
        .ifPresent(userAuths -> {

            System.out.println("  User-auths:");
            userAuths.userAuths()
                .forEach(userAuth -> {
                    var leases = userAuth.albumLeases();
                    System.out.println(
                        "    User: " + userAuth.userId() + ", " + leases.size() + " lease" +
                        (leases.size() == 1 ? "" : "s") +
                        (leases.isEmpty() ? "" : ":"));
                    leases
                        .forEach(lease ->
                            System.out.println(
                                "      " + lease.albumId() + " @ " + lease.expiry()));
                });
        });

    System.out.println("\nLeases:");
    s3Accessor.listInfos("lease")
        .forEach(info -> {

            var lease = info.key();
            var instant =
                Instant.ofEpochSecond(SECONDS_PER_HOUR * epochHour(lease));
            var time =
                info.lastModified().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_DATE_TIME);
            System.out.println(
                "  " + instant.atOffset(ZoneOffset.UTC).format(DAY_HOUR) + ": (" + time + ")"
            );

            s3Accessor.stream(lease)
                .ifPresent(_ -> {
                    archives.retrieveRecord(lease)
                        .stream()
                        .findFirst()
                        .ifPresent(archivedRecord ->
                            System.out.println("    " + archivedRecord));
                    int tracks = archives.retrieveRecord(lease)
                        .map(Archives.ArchivedRecord::contents)
                        .map(Collection::size)
                        .orElse(0);
                    if (tracks > 0) {
                        System.out.println("    Tracks: " + tracks);
                    }
                });
        });
}

private static final long SECONDS_PER_HOUR = Duration.ofHours(1).toSeconds();

private static final String LEASE_PREFIX = "lease-";

private static final DateTimeFormatter DAY_HOUR =
    DateTimeFormatter.ofPattern("HH @ MMM dd", Locale.ROOT);

private static Long epochHour(String path) {
    var hourstamp = path.substring(LEASE_PREFIX.length());
    var dashindex = hourstamp.indexOf('-');
    return Long.parseLong(hourstamp.substring(0, dashindex));
}
