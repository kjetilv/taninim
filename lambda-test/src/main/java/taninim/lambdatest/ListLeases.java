package taninim.lambdatest;

import com.github.kjetilv.uplift.flogs.Flogs;
import com.github.kjetilv.uplift.kernel.Env;
import com.github.kjetilv.uplift.s3.S3Accessor;
import taninim.music.Archives;
import taninim.music.legal.S3Archives;
import taninim.music.medias.UserAuths;

import java.io.DataInputStream;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Locale;
import java.util.concurrent.Executors;

@SuppressWarnings("UseOfSystemOutOrSystemErr")
public final class ListLeases {

    public static void main(String[] args) {
        Flogs.initialize();

        S3Accessor s3Accessor = S3Accessor.fromEnvironment(Env.actual(), Executors.newVirtualThreadPerTaskExecutor());
        Archives archives = new S3Archives(s3Accessor);
        System.out.println("auth-digest.bin:");
        s3Accessor.stream("auth-digest.bin")
            .map(inputStream ->
                UserAuths.from(new DataInputStream(inputStream)))
            .ifPresent(userAuths -> {
                System.out.println("  User-auths:");
                userAuths.userAuths()
                    .forEach(userAuth -> {
                        System.out.println(
                            "    User: " + userAuth.userId() + ", " + userAuth.albumLeases().size() + " leases:");
                        userAuth.albumLeases()
                            .forEach(lease ->
                                System.out.println(
                                    "      " + lease.albumId() + " @ " + lease.expiry()));
                    });
            });

        System.out.println("Leases:");
        s3Accessor.listInfos("lease")
            .forEach(info -> {
                String lease = info.key();
                Instant instant =
                    Instant.ofEpochSecond(SECONDS_PER_HOUR * epochHour(lease));
                String time =
                    info.lastModified().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_DATE_TIME);
                System.out.println(
                    "  " + instant.atOffset(ZoneOffset.UTC).format(DAY_HOUR) + ": (" + time + ")");
                s3Accessor.stream(lease)
                    .ifPresent(inputStream -> {
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
        String hourstamp = path.substring(LEASE_PREFIX.length());
        int dashindex = hourstamp.indexOf('-');
        return Long.parseLong(hourstamp.substring(0, dashindex));
    }
}
