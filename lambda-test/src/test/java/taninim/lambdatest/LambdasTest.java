package taninim.lambdatest;

import module java.base;
import com.github.kjetilv.uplift.hash.Hash;
import com.github.kjetilv.uplift.kernel.io.BinaryWritable;
import com.github.kjetilv.uplift.kernel.io.Range;
import com.github.kjetilv.uplift.s3.S3Accessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import taninim.fb.Authenticator;
import taninim.fb.ExtAuthResponse;
import taninim.fb.ExtUser;
import taninim.kudu.DefaultKudu;
import taninim.kudu.Kudu;
import taninim.kudu.Track;
import taninim.kudu.TrackRange;
import taninim.music.Archives;
import taninim.music.aural.Chunk;
import taninim.music.legal.ArchivedLeasesRegistry;
import taninim.music.legal.CloudMediaLibrary;
import taninim.music.legal.S3Archives;
import taninim.music.medias.AlbumTrackIds;
import taninim.music.medias.MediaIds;
import taninim.music.medias.MediaLibrary;
import taninim.yellin.*;

import static com.github.kjetilv.uplift.hash.HashKind.K128;
import static org.assertj.core.api.Assertions.assertThat;
import static taninim.yellin.Operation.ACQUIRE;
import static taninim.yellin.Operation.RELEASE;

@SuppressWarnings("MagicNumber")
class LambdasTest {

    private final Duration sessionDuration = Duration.ofDays(1);

    private final Duration leaseDuration = Duration.ofHours(1);

    private final AtomicReference<Instant> time = new AtomicReference<>(Instant.EPOCH);

    private final Hash<K128> album1 = K128.random();

    private final Hash<K128> track1a = K128.random();

    private final Hash<K128> track1b = K128.random();

    private final Hash<K128> album2 = K128.random();

    private final Hash<K128> track2a = K128.random();

    private final Hash<K128> track2b = K128.random();

    private final Hash<K128> track2c = K128.random();

    private final String userId = K128.random().digest();

    private final String idsJson = """
        {
          "acl": [
            {
              "ser": "%s"
            }
          ]
        }
        """.formatted(userId);

    private final Map<String, S3Data> s3 = new ConcurrentHashMap<>();

    private S3Accessor s3Accessor;

    private Archives archives;

    private LeasesDispatcher leasesDispatcher;

    private Kudu kudu;

    private MediaLibrary mediaLibrary;

    void initLibrary() {
        put(
            "media-digest.bin", mediaIds(
                album(
                    album1,
                    track1a,
                    track1b
                ), album(
                    album2,
                    track2a,
                    track2b,
                    track2c
                )
            )
        );
        storeSongs(track1a, track1b, track2a, track2b, track2c);
    }

    @BeforeEach
    void setUp() {
        s3Accessor = new MemoryS3(s3, this::now);
        archives = S3Archives.create(s3Accessor);
        s3Accessor.put(idsJson, "ids.json");
        mediaLibrary = CloudMediaLibrary.create(s3Accessor, this::now);

        initLibrary();
    }

    @AfterEach
    void tearDown() {
        archives = null;
        leasesDispatcher = null;
    }

    @SafeVarargs
    private void storeSongs(Hash<K128>... tracks) {
        for (var track : tracks) {
            put(track.asUuid().toString() + ".m4a");
        }
    }

    private void tick(int numerator, int denominator, Duration duration) {
        tick(Duration.ofSeconds(numerator * duration.getSeconds() / denominator));
    }

    private void tick(TemporalAmount duration) {
        time.updateAndGet(instant -> instant.plus(duration));
    }

    private Instant now() {
        return time.get();
    }

    private void time(Instant now) {
        time.set(now);
    }

    private void setupDispatcher(Duration sessionDuration, Duration ticketDuration) {
        Authenticator authenticator = authResponse -> Optional.of(new ExtUser(
            authResponse.userID(),
            authResponse.userID()
        ));
        var leasesRegistry = ArchivedLeasesRegistry.create(
            archives,
            ticketDuration,
            this::now
        );
        leasesDispatcher = Yellin.leasesDispatcher(
            s3Accessor,
            this::now,
            sessionDuration,
            ticketDuration,
            authenticator
        );
        kudu = new DefaultKudu(leasesRegistry, mediaLibrary, 16, this::now);
    }

    private void put(String file, BinaryWritable id) {
        try (
            var boas = new ByteArrayOutputStream(); var dos = new DataOutputStream(boas)
        ) {
            id.writeTo(dos);
            dos.close();
            s3Accessor.put(file, boas.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void put(String file) {
        var bytes = new byte[256];
        ThreadLocalRandom.current().nextBytes(bytes);
        s3Accessor.put(file, bytes);
    }

    private static ExtAuthResponse authResponse(String id) {
        var expiresIn = Duration.ofMinutes(1);
        var expirationTime = Instant.now().plus(expiresIn);
        return new ExtAuthResponse(
            id == null ? K128.random().digest() : id,
            K128.random().digest(),
            K128.random().digest(),
            expiresIn,
            BigInteger.valueOf(expirationTime.getEpochSecond())
        );
    }

    private static MediaIds mediaIds(AlbumTrackIds... albumTrackIds) {
        return new MediaIds(Arrays.asList(albumTrackIds));
    }

    @SafeVarargs
    private static AlbumTrackIds album(Hash<K128> albumId, Hash<K128>... tracks) {
        return new AlbumTrackIds(albumId, Arrays.asList(tracks));
    }

    @SuppressWarnings("unused")
    @Nested
    class TimedWithLibrary {

        @BeforeEach
        void setup() {
            time(Instant.EPOCH);
            initLibrary();
        }

        @Test
        void noRentIntime() {
            var firstToken =
                leasesDispatcher.createLease(authResponse(userId))
                    .map(LeasesActivation::token)
                    .orElseThrow();

            tick(5, 4, sessionDuration);

            assertThat(leasesDispatcher.requestLease(new LeasesRequest(
                ACQUIRE,
                new LeasesData(
                    userId,
                    firstToken,
                    album2.digest()
                )
            ))).isEmpty();
        }

        @Test
        void rentAndReauth() {
            time(Instant.EPOCH.plus(Duration.ofDays(1).plusHours(8)));

            var firstToken =
                leasesDispatcher.createLease(authResponse(userId))
                    .map(LeasesActivation::token)
                    .orElseThrow();

            tick(1, 4, leaseDuration);

            assertThat(leasesDispatcher.requestLease(
                new LeasesRequest(ACQUIRE, new LeasesData(userId, firstToken, album2.digest())))
            ).hasValueSatisfying(activation -> {
                assertThat(activation.token()).isEqualTo(firstToken);
                assertThat(activation.trackUUIDs()).describedAs(
                    "Expected three tracks and definitely not album " +
                    "UUID %s",
                    album2
                ).containsExactly(
                    track2a.digest(),
                    track2b.digest(),
                    track2c.digest()
                );
            });

            assertThat(s3Accessor.list("lease-")).hasSize(1);

            tick(1, 2, leaseDuration);

            assertThat(leasesDispatcher.currentLease(authResponse(userId)))
                .hasValueSatisfying(result -> {
                    assertThat(result.token()).isEqualTo(firstToken);
                    assertThat(result.trackUUIDs()).describedAs(
                        "Expected three tracks and definitely not album " +
                        "UUID %s",
                        album2
                    ).containsExactly(
                        track2a.digest(),
                        track2b.digest(),
                        track2c.digest()
                    );
                });

            assertThat(s3Accessor.list("lease-")).hasSize(1);
        }

        @Test
        void rentAndTimeoutPlay() {
            var firstToken =
                leasesDispatcher.createLease(authResponse(userId))
                    .map(LeasesActivation::token)
                    .orElseThrow();
            assertThat(leasesDispatcher.requestLease(
                new LeasesRequest(ACQUIRE, new LeasesData(userId, firstToken, album2.digest()))
            )).hasValueSatisfying(result -> {
                assertThat(result.token()).isEqualTo(firstToken);
                assertThat(result.trackUUIDs()).describedAs(
                    "Expected two tracks and definitely not album UUID %s",
                    album2
                ).containsExactly(
                    track2a.digest(),
                    track2b.digest(),
                    track2c.digest()
                );
            });

            tick(Duration.ofMinutes(leaseDuration.toMinutes() / 2));

            assertThat(leasesDispatcher.currentLease(authResponse(userId))).hasValueSatisfying(result -> {
                assertThat(result.token()).isEqualTo(firstToken);
                assertThat(result.trackUUIDs()).describedAs(
                    "Expected three tracks and definitely not album UUID %s",
                    album2
                ).containsExactly(
                    track2a.digest(),
                    track2b.digest(),
                    track2c.digest()
                );
            });

            tick(leaseDuration);

            assertThat(leasesDispatcher.createLease(authResponse(userId)))
                .hasValueSatisfying(result -> {
                    assertThat(result.token()).isNotEqualTo(firstToken);
                    assertThat(result.trackUUIDs()).describedAs("Expected timed-out rental")
                        .isEmpty();
                });
        }

        void rentAndPlayThenTimeout() {
            var firstToken =
                leasesDispatcher.createLease(authResponse(userId))
                    .map(LeasesActivation::token)
                    .<Hash<K128>>map(Hash::from)
                    .orElseThrow();
            leasesDispatcher.requestLease(new LeasesRequest(
                ACQUIRE, new LeasesData(
                userId,
                firstToken.digest(),
                album2.digest()
            )
            ));
            var track = new Track(track2b, Track.Format.M4A);
            var trackRange = new TrackRange(track, new Range(0L, 10L), firstToken);

            assertThat(kudu.library(firstToken)).isPresent();
            assertThat(kudu.library(K128.random())).isNotPresent();

            tick(Duration.ofDays(1));

            assertThat(kudu.library(firstToken)).isEmpty();
        }

        @BeforeEach
        void setupLibrary() {
            LambdasTest.this.setupDispatcher(sessionDuration, leaseDuration);
        }
    }

    @Nested
    class Untimed {

        @BeforeEach
        void setupDispatcher() {
            LambdasTest.this.setupDispatcher(sessionDuration, leaseDuration);
        }

        @Test
        void rejectStrangers() {
            setupDispatcher();
            assertThat(leasesDispatcher.createLease(authResponse(K128.random().digest()))).isEmpty();
        }

        @Test
        void authUserWithoutRentals() {
            setupDispatcher();
            assertThat(leasesDispatcher.createLease(authResponse(userId)))
                .hasValueSatisfying(result ->
                    assertThat(result.isEmpty()).isTrue());
        }

        @Test
        void authUserShouldProduceRecord() {
            setupDispatcher();
            assertThat(leasesDispatcher.createLease(authResponse(userId))).hasValueSatisfying(result -> {
                assertThat(result.isEmpty()).isTrue();
                assertThat(leasesDispatcher.createLease(authResponse(userId)))
                    .hasValueSatisfying(activation ->
                        assertThat(activation.token()).isEqualTo(result.token()));
            });
        }

        @Nested
        class WithLibrary {

            @BeforeEach
            void setup() {
                initLibrary();
            }

            @Test
            void lookupExisting() {
                assertThat(leasesDispatcher.createLease(authResponse(userId))).hasValueSatisfying(result -> {
                    assertThat(result.trackUUIDs()).isEmpty();
                    var leasesRequest = new LeasesRequest(
                        ACQUIRE,
                        new LeasesData(
                            userId,
                            result.token(),
                            album1.digest()
                        )
                    );
                    assertThat(leasesDispatcher.requestLease(leasesRequest))
                        .hasValueSatisfying(requestResult ->
                            assertThat(requestResult.trackUUIDs()).describedAs(
                                "Expected two tracks and definitely not album UUID %s",
                                album1
                            ).containsExactly(
                                track1a.digest(),
                                track1b.digest()
                            ));
                });
            }

            @Test
            void failedRent() {
                assertThat(leasesDispatcher.createLease(authResponse(userId)))
                    .hasValueSatisfying(result ->
                        assertThat(result.isEmpty()).isTrue());
            }

            @Test
            void rentAndReauth() {
                var firstToken =
                    leasesDispatcher.createLease(authResponse(userId))
                        .map(LeasesActivation::token)
                        .orElseThrow();
                assertThat(leasesDispatcher.requestLease(new LeasesRequest(
                    ACQUIRE,
                    new LeasesData(
                        userId,
                        firstToken,
                        album2.digest()
                    )
                ))).hasValueSatisfying(result -> {
                    assertThat(result.token()).isEqualTo(firstToken);
                    assertThat(result.trackUUIDs()).describedAs(
                        "Expected two tracks and definitely not album " +
                        "UUID %s",
                        album2
                    ).containsExactly(
                        track2a.digest(),
                        track2b.digest(),
                        track2c.digest()
                    );
                });
                assertThat(leasesDispatcher.currentLease(authResponse(userId))).hasValueSatisfying(result -> {
                    assertThat(result.token()).isEqualTo(firstToken);
                    assertThat(result.trackUUIDs()).describedAs(
                        "Expected three tracks and definitely not album " +
                        "UUID %s",
                        album2
                    ).containsExactly(
                        track2a.digest(),
                        track2b.digest(),
                        track2c.digest()
                    );
                });
            }

            @Test
            void rentAndRelease() {
                var firstToken =
                    leasesDispatcher.createLease(authResponse(userId))
                        .map(LeasesActivation::token)
                        .orElseThrow();
                assertThat(leasesDispatcher.requestLease(new LeasesRequest(
                    ACQUIRE,
                    new LeasesData(
                        userId,
                        firstToken,
                        album2.digest()
                    )
                ))).hasValueSatisfying(result -> {
                    assertThat(result.token()).isEqualTo(firstToken);
                    assertThat(result.trackUUIDs()).describedAs(
                        "Expected three tracks and definitely not album " +
                        "UUID %s",
                        album2
                    ).containsExactly(
                        track2a.digest(),
                        track2b.digest(),
                        track2c.digest()
                    );
                });

                assertThat(leasesDispatcher.dismissLease(new LeasesRequest(
                    RELEASE,
                    new LeasesData(
                        userId,
                        firstToken,
                        album2.digest()
                    )
                ))).hasValueSatisfying(result ->
                    assertThat(result.trackUUIDs()).isEmpty());
            }

            @Test
            void rentAndPlay() {
                var firstToken = leasesDispatcher.createLease(authResponse(userId))
                    .map(LeasesActivation::token)
                    .<Hash<K128>>map(Hash::from)
                    .orElseThrow();
                assertThat(
                    leasesDispatcher.requestLease(new LeasesRequest(
                        ACQUIRE,
                        new LeasesData(
                            userId,
                            firstToken.digest(),
                            album2.digest()
                        )
                    ))
                ).hasValueSatisfying(result ->
                    assertThat(result.trackUUIDs()).hasSize(3));

                var track = new Track(track2b, Track.Format.M4A);
                var trackRange = new TrackRange(
                    track,
                    new Range(0L, 10L).withLength(256L),
                    firstToken
                );

                assertThat(kudu.audioBytes(trackRange)).hasValueSatisfying(audioBytes -> {
                    assertThat(audioBytes.chunk()).isEqualTo(new Chunk("m4a", 0L, 10L, 256L));
                    assertThat(audioBytes.bytes()).hasSize(10);
                });
            }
        }
    }
}
