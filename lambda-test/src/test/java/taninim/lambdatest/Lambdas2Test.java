package taninim.lambdatest;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalAmount;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import com.github.kjetilv.uplift.flambda.CorsSettings;
import com.github.kjetilv.uplift.flambda.EmptyEnv;
import com.github.kjetilv.uplift.flambda.LambdaHarness;
import com.github.kjetilv.uplift.flambda.Reqs;
import com.github.kjetilv.uplift.kernel.io.BinaryWritable;
import com.github.kjetilv.uplift.kernel.uuid.Uuid;
import com.github.kjetilv.uplift.lambda.LambdaClientSettings;
import com.github.kjetilv.uplift.lambda.LambdaHandler;
import com.github.kjetilv.uplift.s3.S3Accessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import taninim.TaninimSettings;
import taninim.fb.Authenticator;
import taninim.fb.ExtUser;
import taninim.kudu.KuduLambdaHandler;
import taninim.music.medias.AlbumTrackIds;
import taninim.music.medias.MediaIds;
import taninim.yellin.YellinLambdaHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static taninim.lambdatest.Parse.authResponse;
import static taninim.lambdatest.Parse.leasesActivation;

@SuppressWarnings(
    {
        "MagicNumber", "SameParameterValue", "FieldCanBeLocal", "WeakerAccess", "unused"
    }
)
class Lambdas2Test {

    private Map<String, MyS3.S3Data> s3 = new ConcurrentHashMap<>();

    private AtomicReference<Instant> time = new AtomicReference<>();

    private S3Accessor s3Accessor;

    private LambdaHandler kuduHandler;

    private LambdaHandler yellinHandler;

    private LambdaHarness yellinHarness;

    private LambdaHarness kuduHarness;

    private Reqs yellinReqs;

    private Reqs kuduReqs;

    @BeforeEach
    void setupAll() {
        setTime(Instant.EPOCH);

        s3Accessor = new MyS3(s3, this::now);
        s3Accessor.put(idsJson, "ids.json");
        s3Accessor.put("media.jsonl.gz", zippedLibrary());

        MediaIds mediaIds = mediaIds(
            album(album1, track1a, track1b),
            album(album2, track2a, track2b, track2c)
        );

        put("media-digest.bin", mediaIds);
        List.of(track1a, track1b, track2a, track2b, track2c)
            .forEach(track ->
                putRandomBytes(track.uuid().toString() + ".m4a"));

        Supplier<Instant> timeRetriever = timeRetriever();

        TaninimSettings taninimSettings = new TaninimSettings(
            Duration.ofDays(1),
            Duration.ofHours(4),
            16
        );

        CorsSettings kuduCors = new CorsSettings(
            List.of("*"),
            List.of("POST", "DELETE"),
            List.of("content-type")
        );

        CorsSettings yellinCors = new CorsSettings(
            List.of("*"),
            List.of("GET"),
            List.of("content-type", "range")
        );

        LambdaClientSettings yellinClientSettings =
            new LambdaClientSettings(new com.github.kjetilv.uplift.flambda.EmptyEnv(), timeRetriever);

        LambdaClientSettings kuduClientSettings =
            new LambdaClientSettings(new EmptyEnv(), timeRetriever);

        yellinHandler = YellinLambdaHandler.handler(
            yellinClientSettings,
            taninimSettings,
            () -> s3Accessor,
            AUTHENTICATOR
        );

        kuduHandler = KuduLambdaHandler.create(kuduClientSettings, taninimSettings, () -> s3Accessor);

        yellinHarness = new LambdaHarness("yellin", yellinHandler, yellinCors, timeRetriever);
        kuduHarness = new LambdaHarness("kudu", kuduHandler, kuduCors, timeRetriever);

        logger().info("Kudu   : {}", kuduHarness);
        logger().info("Yellin : {}", yellinHarness);

        yellinReqs = yellinHarness.reqs();
        kuduReqs = kuduHarness.reqs();
    }

    @AfterEach
    void teardownAll() {
        s3.clear();
        s3 = null;
        s3Accessor = null;
        yellinHarness.close();
        kuduHarness.close();

        yellinHarness = null;
        kuduHarness = null;

        time = null;
        kuduHandler = null;
        yellinHandler = null;
    }

    @Test
    void shouldRejectStrangers() {
        HttpResponse<String> response = authAs(Uuid.random().digest()).join();
        assertThat(response.statusCode()).isBetween(400, 500);
    }

    @Test
    void shouldAcceptKnownUser() {
        HttpResponse<String> response = ok(authAs(userId).join());
        AuthResponse authResponse = authResponse(response.body());
        assertThat(authResponse.trackUUIDs()).isEmpty();
        assertThat(authResponse.token()).isNotBlank();
    }

    @Test
    void shouldBeAbleToRequestLease() {
        HttpResponse<String> authResponse = authAs(userId).join();
        AuthResponse auth1 = authResponse(authResponse.body());
        assertThat(auth1.trackUUIDs()).isEmpty();

        LeasesActivation leasesActivation =
            leasesActivation(ok(lease(Uuid.from(auth1.token()), album2).join()).body());
        assertThat(leasesActivation.trackUUIDs())
            .containsExactly(track2a.digest(), track2b.digest(), track2c.digest());
    }

    @Test
    void shouldBeAbleToRequestLeaseAgain() {
        HttpResponse<String> authResponse = authAs(userId).join();
        AuthResponse auth1 = authResponse(authResponse.body());
        assertThat(auth1.trackUUIDs()).isEmpty();

        Uuid token = Uuid.from(auth1.token());
        HttpResponse<String> leaseResponse1 = ok(lease(token, album2).join());
        HttpResponse<String> leaseResponse2 = ok(lease(token, album1).join());

        LeasesActivation leasesActivation = leasesActivation(leaseResponse2.body());
        assertThat(leasesActivation.trackUUIDs()).containsExactlyInAnyOrder(
            track1a.digest(),
            track1b.digest(),
            track2a.digest(),
            track2b.digest(),
            track2c.digest()
        );
    }

    @Test
    void shouldBeAbleToDropLease() {
        HttpResponse<String> authResponse = authAs(userId).join();
        AuthResponse auth1 = authResponse(authResponse.body());
        assertThat(auth1.trackUUIDs()).isEmpty();

        Uuid token = Uuid.from(auth1.token());

        ok(lease(token, album2).join());
        ok(lease(token, album1).join());

        HttpResponse<String> dropResponse = ok(release(token, album2).join());
        LeasesActivation leasesDectivation = leasesActivation(dropResponse.body());

        assertThat(leasesDectivation.trackUUIDs()).containsExactlyInAnyOrder(track1a.digest(), track1b.digest());
    }

    @Test
    void shouldBeAbleToStreamAfterLease() {
        HttpResponse<String> authResponse = authAs(userId).join();
        AuthResponse auth1 = authResponse(authResponse.body());
        assertThat(auth1.trackUUIDs()).isEmpty();

        Uuid token = Uuid.from(auth1.token());
        ok(lease(token, album2).join());
        ok(lease(token, album1).join());

        assertThat(stream(token, track1a, "bytes=0-15").join().statusCode()).isEqualTo(206);
    }

    @Test
    void shouldBeAbleToStreamALotAfterLease() {
        HttpResponse<String> authResponse = authAs(userId).join();
        AuthResponse auth1 = authResponse(authResponse.body());
        assertThat(auth1.trackUUIDs()).isEmpty();

        Uuid token = Uuid.from(auth1.token());
        ok(lease(token, album2).join());
        ok(lease(token, album1).join());

        IntStream.range(0, 128).mapToObj(i -> {
            int start = i * 2;
            int end = start + 1;
            return stream(token, track1a, "bytes=" + start + "-" + end);
        }).forEach(action -> {
            HttpResponse<String> response = action.join();
            assertThat(response.statusCode()).isEqualTo(206);
            assertThat(response.headers().firstValueAsLong("Content-Length")).hasValue(2L);
        });
    }

    @Test
    void shouldBeRestrictedToStreamingSize() {
        HttpResponse<String> authResponse = authAs(userId).join();
        AuthResponse auth1 = authResponse(authResponse.body());
        assertThat(auth1.trackUUIDs()).isEmpty();

        Uuid token = Uuid.from(auth1.token());
        ok(lease(token, album2).join());
        ok(lease(token, album1).join());

        assertThat(stream(token, track1a, "bytes=0-1024").join()).satisfies(response -> {
            assertThat(response.statusCode()).isEqualTo(206);
            assertThat(response.headers().allValues("content-range")).containsExactly("bytes 0-15/256");
        });
        assertThat(stream(token, track1a, "bytes=0-7").join()).satisfies(response -> {
            assertThat(response.statusCode()).isEqualTo(206);
            assertThat(response.headers().allValues("content-range")).containsExactly("bytes 0-7/256");
            assertThat(response.headers().firstValueAsLong("content-length")).hasValue(8L);
        });
    }

    @Test
    void shouldBeAbleToDropLeaseAgain() {
        HttpResponse<String> authResponse = authAs(userId).join();
        AuthResponse auth1 = authResponse(authResponse.body());
        Uuid token = Uuid.from(auth1.token());

        HttpResponse<String> leaseResponse1 = ok(lease(token, album2).join());
        HttpResponse<String> leaseResponse2 = ok(lease(token, album1).join());
        HttpResponse<String> dropResponse1 = ok(release(token, album2).join());
        HttpResponse<String> dropResponse2 = ok(release(token, album1).join());

        LeasesActivation leasesDectivation = leasesActivation(dropResponse2.body());
        assertThat(leasesDectivation.trackUUIDs()).isEmpty();
    }

    @Test
    void shouldFindAlbumsAfterLease() {
        HttpResponse<String> authResponse = authAs(userId).join();
        AuthResponse auth1 = authResponse(authResponse.body());
        assertThat(auth1.trackUUIDs()).isEmpty();

        HttpResponse<String> leaseResponse = ok(lease(Uuid.from(auth1.token()), album2).join());

        HttpResponse<String> authResponse2 = authAs(userId).join();
        AuthResponse auth2 = authResponse(authResponse2.body());
        assertThat(auth2.token()).isEqualTo(auth1.token());
        assertThat(auth2.trackUUIDs()).containsExactly(track2a.digest(), track2b.digest(), track2c.digest());
    }

    @Test
    void leaseShouldTimeOut() {
        HttpResponse<String> authResponse = authAs(userId).join();
        AuthResponse auth1 = authResponse(authResponse.body());

        HttpResponse<String> leaseResponse = ok(lease(Uuid.from(auth1.token()), album2).join());

        passTime(Duration.ofHours(6));

        HttpResponse<String> authResponse2 = authAs(userId).join();
        AuthResponse auth2 = authResponse(authResponse2.body());
        assertThat(auth2.token()).isNotEqualTo(auth1.token());
        assertThat(auth2.trackUUIDs()).isEmpty();
    }

    @Test
    void shouldRenewLeaseAfterTimeout() {
        HttpResponse<String> authResponse1 = authAs(userId).join();
        AuthResponse auth1 = authResponse(authResponse1.body());

        HttpResponse<String> leaseResponse = ok(lease(Uuid.from(auth1.token()), album2).join());

        passTime(Duration.ofDays(1));

        HttpResponse<String> tooLateLeaseResponse = lease(Uuid.from(auth1.token()), album2).join();
        assertThat(tooLateLeaseResponse.statusCode()).isEqualTo(400);

        HttpResponse<String> authResponse2 = authAs(userId).join();
        AuthResponse auth2 = authResponse(authResponse2.body());
        assertThat(auth2.token()).isNotEqualTo(auth1.token());
        assertThat(auth2.trackUUIDs()).isEmpty();

        HttpResponse<String> renewedLeaseResponse = ok(lease(Uuid.from(auth2.token()), album1).join());

        AuthResponse reauthResponse = authResponse(renewedLeaseResponse.body());
        assertThat(reauthResponse.trackUUIDs()).containsExactly(track1a.digest(), track1b.digest());
    }

    private CompletableFuture<HttpResponse<String>> release(Uuid token, Uuid album) {
        return lease("DELETE", token, album);
    }

    private CompletableFuture<HttpResponse<String>> lease(Uuid token, Uuid album) {
        return lease("POST", token, album);
    }

    private CompletableFuture<HttpResponse<String>> authAs(String id) {
        return yellinReqs.path("/auth").execute("POST", extAuthResponse(id));
    }

    private CompletableFuture<HttpResponse<String>> stream(Uuid token, Uuid track, String range) {
        Map<String, String> headers = Optional.ofNullable(range).map(header -> Map.of("Range", header)).orElseGet(
            Collections::emptyMap);
        return kuduReqs.path("/audio/%1$s.m4a?t=%2$s".formatted(track.digest(), token.digest())).get(headers);
    }

    private void putRandomBytes(String file) {
        byte[] bytes = new byte[256];
        ThreadLocalRandom.current().nextBytes(bytes);
        s3Accessor.put(file, bytes);
    }

    private Supplier<Instant> timeRetriever() {
        return this.time::get;
    }

    private void put(String file, BinaryWritable id) {
        try (
            ByteArrayOutputStream boas = new ByteArrayOutputStream(); DataOutputStream dos = new DataOutputStream(boas)
        ) {
            id.writeTo(dos);
            dos.close();
            s3Accessor.put(file, boas.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private CompletableFuture<HttpResponse<String>> lease(String method, Uuid token, Uuid album) {
        if ("DELETE".equalsIgnoreCase(method)) {
            return yellinReqs.path("/lease?userId=" + userId + "&token=" + token.digest() + "&album=" + album.digest())
                .execute(method);
        }
        return yellinReqs.path("/lease").execute(method, Map.of(
            "userId", userId,
            "token", token.digest(),
            "album", album.digest()
        ));
    }

    private void setTime(Instant time) {
        this.time.set(time);
    }

    private void passTime(TemporalAmount duration) {
        this.time.updateAndGet(time -> time.plus(duration));
    }

    private Instant now() {
        return time.get();
    }

    private static final Uuid album1 = Uuid.random();

    private static final Uuid track1a = Uuid.random();

    private static final Uuid track1b = Uuid.random();

    private static final Uuid album2 = Uuid.random();

    private static final Uuid track2a = Uuid.random();

    private static final Uuid track2b = Uuid.random();

    private static final Uuid track2c = Uuid.random();

    private static final String userId = Uuid.random().digest();

    private static final Authenticator AUTHENTICATOR = authResponse -> Optional.of(new ExtUser(
        "dave",
        authResponse.userID()
    ));

    private static final String idsJson = """
                                          {
                                            "acl": [
                                              {
                                                "ser": "%s"
                                              }
                                            ]
                                          }
                                          """.formatted(userId);

    private static final String mediasJson = """
                                             [
                                               {
                                                 "uuid": "%s",
                                                 "name": "Buer: Book of Angels vol. 31",
                                                 "artist": "Brian Marsella",
                                                 "year": 2017,
                                                 "artists": [
                                                   {
                                                     "uuid": "DZ8tx-pzPFa3UES6gswUYg",
                                                     "name": "Brian Marsella"
                                                   },
                                                   {
                                                     "uuid": "PYlV94yfO42XwBkNOQzLGQ",
                                                     "name": "Kenny Wollesen"
                                                   },
                                                   {
                                                     "uuid": "-QQNc2RfMaCH_VRyRsnVHw",
                                                     "name": "Trevor Dunn"
                                                   },
                                                   {
                                                     "uuid": "kpK9rVmrMO_opipEA3MhsA",
                                                     "name": "John Zorn"
                                                   },
                                                   {
                                                     "uuid": "ptJqfOSWN5a-Ky4VFSJSEQ",
                                                     "name": "Brian Marsella Trio"
                                                   }
                                                 ],
                                                 "credits": [
                                                   {
                                                     "artist": "Brian Marsella",
                                                     "credit": "Piano"
                                                   },
                                                   {
                                                     "artist": "Kenny Wollesen",
                                                     "credit": "Drums"
                                                   },
                                                   {
                                                     "artist": "Trevor Dunn",
                                                     "credit": "Bass"
                                                   },
                                                   {
                                                     "artist": "John Zorn",
                                                     "credit": "Arranged By"
                                                   },
                                                   {
                                                     "artist": "Brian Marsella",
                                                     "credit": "Arranged By"
                                                   },
                                                   {
                                                     "artist": "Brian Marsella Trio"
                                                   },
                                                   {
                                                     "artist": "John Zorn"
                                                   }
                                                 ],
                                                 "prodCredits": [
                                                   {
                                                     "name": "Aaron Nevezie",
                                                     "credit": "Recorded By, Mixed By"
                                                   },
                                                   {
                                                     "name": "John Zorn",
                                                     "credit": "Producer [Produced By]"
                                                   },
                                                   {
                                                     "name": "Scott Hull (2)",
                                                     "credit": "Mastered By"
                                                   },
                                                   {
                                                     "name": "M. Jarrault",
                                                     "credit": "Illustration"
                                                   },
                                                   {
                                                     "name": "Kazunori Sugiyama",
                                                     "credit": "Executive-Producer [Associate Producer]"
                                                   },
                                                   {
                                                     "name": "John Zorn",
                                                     "credit": "Executive-Producer"
                                                   },
                                                   {
                                                     "name": "Heung-Heung Chin",
                                                     "credit": "Design"
                                                   },
                                                   {
                                                     "name": "Chippy (3)",
                                                     "credit": "Design"
                                                   },
                                                   {
                                                     "name": "John Zorn",
                                                     "credit": "Composed By [All Music Composed By]"
                                                   }
                                                 ],
                                                 "sections": [
                                                   {
                                                     "name": "1",
                                                     "tracks": [
                                                       {
                                                         "uuid": "%s",
                                                         "no": "1",
                                                         "name": "Jekusiel",
                                                         "seconds": 308
                                                       },
                                                       {
                                                         "uuid": "%s",
                                                         "no": "2",
                                                         "name": "Diniel",
                                                         "seconds": 259
                                                       }
                                                     ]
                                                   }
                                                 ],
                                                 "obi": "obi-1",
                                                 "discog": "https://www.discogs.com/release/10215523-John-Zorn-Brian-Marsella-Trio-Buer-Book-Of-Angels-Volume-31",
                                                 "discogImage": "https://i.discogs.com/SKSiB7vvKEWDEJL_s7EkpPw3Q127i-ixoDhX1b8JmLw/rs:fit/g:sm/q:40/h:150/w:150/czM6Ly9kaXNjb2dz/LWRhdGFiYXNlLWlt/YWdlcy9SLTEwMjE1/NTIzLTE0OTM1NDE5/ODUtMTU4OS5qcGVn.jpeg",
                                                 "compilation": true,
                                                 "series": [
                                                   {
                                                     "name": "Archival Series"
                                                   },
                                                   {
                                                     "name": "Book Of Angels"
                                                   }
                                                 ]
                                               },
                                               {
                                                 "uuid": "%s",
                                                 "name": "Yankees",
                                                 "artist": "Derek Bailey, George Lewis, John Zorn",
                                                 "year": 1998,
                                                 "artists": [
                                                   {
                                                     "uuid": "bfqedqONOwKiPnrTfBrXeQ",
                                                     "name": "Derek Bailey"
                                                   },
                                                   {
                                                     "uuid": "ALohcUuMMEOyHsW4vril7w",
                                                     "name": "George Lewis"
                                                   },
                                                   {
                                                     "uuid": "kpK9rVmrMO_opipEA3MhsA",
                                                     "name": "John Zorn"
                                                   }
                                                 ],
                                                 "credits": [
                                                   {
                                                     "artist": "George Lewis",
                                                     "credit": "Trombone"
                                                   },
                                                   {
                                                     "artist": "John Zorn",
                                                     "credit": "Alto Saxophone, Soprano Saxophone, Clarinet, Featuring [Game Calls]"
                                                   },
                                                   {
                                                     "artist": "Derek Bailey",
                                                     "credit": "Acoustic Guitar, Electric Guitar"
                                                   },
                                                   {
                                                     "artist": "John Zorn"
                                                   },
                                                   {
                                                     "artist": "George Lewis"
                                                   },
                                                   {
                                                     "artist": "Derek Bailey"
                                                   }
                                                 ],
                                                 "prodCredits": [
                                                   {
                                                     "name": "Martin Bisi",
                                                     "credit": "Recorded By"
                                                   },
                                                   {
                                                     "name": "Howie Weinberg",
                                                     "credit": "Mastered By"
                                                   }
                                                 ],
                                                 "sections": [
                                                   {
                                                     "name": "1",
                                                     "tracks": [
                                                       {
                                                         "uuid": "%s",
                                                         "no": "1",
                                                         "name": "City City City",
                                                         "seconds": 509
                                                       },
                                                       {
                                                         "uuid": "%s",
                                                         "no": "2",
                                                         "name": "The Legend Of Enos Slaughter",
                                                         "seconds": 566
                                                       },
                                                       {
                                                         "uuid": "%s",
                                                         "no": "3",
                                                         "name": "Who's On First",
                                                         "seconds": 195
                                                       }
                                                     ]
                                                   }
                                                 ],
                                                 "discog": "https://www.discogs.com/release/1147845-Derek-Bailey-George-Lewis-John-Zorn-Yankees",
                                                 "discogImage": "https://i.discogs.com/ArKKqO8J7ftuYQpKA1XAoRhg3dY-U9iixERzF6hyCAM/rs:fit/g:sm/q:40/h:150/w:150/czM6Ly9kaXNjb2dz/LWRhdGFiYXNlLWlt/YWdlcy9SLTExNDc4/NDUtMTE5NTk3Mzk0/My5qcGVn.jpeg",
                                                 "compilation": true
                                               }
                                             ]
                                             """.formatted(
        album1.digest(),
        track1a.digest(),
        track2a.digest(),
        album2.digest(),
        track2a.digest(),
        track2b.digest(),
        track2c.digest()
    );

    private static HttpResponse<String> ok(HttpResponse<String> response) {
        assertThat(response.statusCode()).isEqualTo(200);
        return response;
    }

    private static byte[] zippedLibrary() {
        ByteArrayOutputStream gz = new ByteArrayOutputStream();
        try (PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(gz, StandardCharsets.UTF_8))) {
            printWriter.println(mediasJson);
        }
        return gz.toByteArray();
    }

    private static Map<String, Object> extAuthResponse(String userId) {
        BigInteger expirationTime =
            BigInteger.valueOf(Instant.now().getEpochSecond())
                .add(BigInteger.valueOf(Duration.ofHours(1).getSeconds()));
        return Map.of(
            "userID", userId,
            "accessToken", Uuid.random().digest(),
            "signedRequest", Uuid.random().digest(),
            "expiresIn", Duration.ofHours(1).toSeconds(),
            "data_access_expiration_time", expirationTime
        );
    }

    private static void close(Closeable lambda) {
        if (lambda != null) {
            try {
                lambda.close();
            } catch (Exception e) {
                logger().warn("Failed to close {}", lambda, e);
            }
        }
    }

    private static Logger logger() {
        return LoggerFactory.getLogger(Lambdas2Test.class);
    }

    private static MediaIds mediaIds(AlbumTrackIds... albumTrackIds) {
        return new MediaIds(Arrays.asList(albumTrackIds));
    }

    private static AlbumTrackIds album(Uuid albumId, Uuid... tracks) {
        return new AlbumTrackIds(albumId, Arrays.asList(tracks));
    }

    @SuppressWarnings("WeakerAccess")
    record AuthResponse(
        String name,
        String userId,
        String token,
        List<String> trackUUIDs,
        Long expiry
    ) {

    }

    public record LeaseRequest(
        String userId,
        String token,
        String album
    ) {

    }

    public record LeasesActivation(
        String name,
        String userId,
        String token,
        List<String> trackUUIDs,
        Long expiry
    ) {

    }
}
