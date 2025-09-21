package taninim.lambdatest;

import module java.base;
import module java.net.http;
import module org.junit.jupiter.api;
import module taninim.fb;
import module taninim.kudu;
import module taninim.taninim;
import module taninim.yellin;
import module uplift.json;
import module uplift.kernel;
import module uplift.s3;
import module uplift.uuid;

import static org.assertj.core.api.Assertions.assertThat;
import static taninim.yellin.LeasesRequest.Op.ACQUIRE;
import static taninim.yellin.LeasesRequest.Op.RELEASE;

@SuppressWarnings("MagicNumber")
class LambdasTest {

    private final Duration sessionDuration = Duration.ofDays(1);

    private final Duration leaseDuration = Duration.ofHours(1);

    private final AtomicReference<Instant> time = new AtomicReference<>(Instant.EPOCH);

    private final Uuid album1 = Uuid.random();

    private final Uuid track1a = Uuid.random();

    private final Uuid track1b = Uuid.random();

    private final Uuid album2 = Uuid.random();

    private final Uuid track2a = Uuid.random();

    private final Uuid track2b = Uuid.random();

    private final Uuid track2c = Uuid.random();

    private final String userId = Uuid.random().digest();

    private final String idsJson = """
        {
          "acl": [
            {
              "ser": "%s"
            }
          ]
        }
        """.formatted(userId);

    private final Map<String, MemoryS3.S3Data> s3 = new ConcurrentHashMap<>();

    private S3Accessor s3Accessor;

    private Archives archives;

    private LeasesDispatcher leasesDispatcher;

    private Kudu kudu;

    private MediaLibrary mediaLibrary;

    void initLibrary() {
        put("media-digest.bin", mediaIds(album(album1, track1a, track1b), album(album2, track2a, track2b, track2c)));
        storeSongs(track1a, track1b, track2a, track2b, track2c);
    }

    @BeforeEach
    void setUp() {
        s3Accessor = new MemoryS3(s3, this::now);
        archives = new S3Archives(s3Accessor);
        s3Accessor.put(idsJson, "ids.json");
        mediaLibrary = new CloudMediaLibrary(s3Accessor, this::now);

        initLibrary();
    }

    @AfterEach
    void tearDown() {
        archives = null;
        leasesDispatcher = null;
    }

    private void storeSongs(Uuid... tracks) {
        for (Uuid track : tracks) {
            put(track.uuid().toString() + ".m4a");
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
        FbAuthenticator fbAuthenticator = authResponse -> Optional.of(new ExtUser(
            authResponse.userID(),
            authResponse.userID()
        ));
        LeasesRegistry leasesRegistry = new ArchivedLeasesRegistry(
            archives,
            ticketDuration,
            this::now
        );
        leasesDispatcher = Yellin.leasesDispatcher(
            s3Accessor,
            this::now,
            sessionDuration,
            ticketDuration,
            fbAuthenticator
        );
        kudu = new DefaultKudu(leasesRegistry, mediaLibrary, 16, this::now);
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

    private void put(String file) {
        byte[] bytes = new byte[256];
        ThreadLocalRandom.current().nextBytes(bytes);
        s3Accessor.put(file, bytes);
    }

    private static ExtAuthResponse authResponse(String id) {
        Duration expiresIn = Duration.ofMinutes(1);
        Instant expirationTime = Instant.now().plus(expiresIn);
        return new ExtAuthResponse(
            id == null ? Uuid.random().digest() : id,
            Uuid.random().digest(),
            Uuid.random().digest(),
            expiresIn,
            BigInteger.valueOf(expirationTime.getEpochSecond())
        );
    }

    private static MediaIds mediaIds(AlbumTrackIds... albumTrackIds) {
        return new MediaIds(Arrays.asList(albumTrackIds));
    }

    private static AlbumTrackIds album(Uuid albumId, Uuid... tracks) {
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
            Uuid firstToken =
                leasesDispatcher.createLease(authResponse(userId))
                    .map(LeasesActivation::token)
                    .orElseThrow();

            tick(5, 4, sessionDuration);

            assertThat(leasesDispatcher.requestLease(new LeasesRequest(
                ACQUIRE,
                new LeasesData(
                    userId,
                    firstToken,
                    album2
                )
            ))).isEmpty();
        }

        @Test
        void rentAndReauth() {
            time(Instant.EPOCH.plus(Duration.ofDays(1).plusHours(8)));

            Uuid firstToken =
                leasesDispatcher.createLease(authResponse(userId))
                    .map(LeasesActivation::token)
                    .orElseThrow();

            tick(1, 4, leaseDuration);

            assertThat(leasesDispatcher.requestLease(
                new LeasesRequest(ACQUIRE, new LeasesData(userId, firstToken, album2)))
            ).hasValueSatisfying(activation -> {
                assertThat(activation.token()).isEqualTo(firstToken);
                assertThat(activation.trackUUIDs()).describedAs(
                    "Expected three tracks and definitely not album " +
                    "UUID %s",
                    album2
                ).containsExactly(
                    track2a,
                    track2b,
                    track2c
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
                        track2a,
                        track2b,
                        track2c
                    );
                });

            assertThat(s3Accessor.list("lease-")).hasSize(1);
        }

        @Test
        void rentAndTimeoutPlay() {
            Uuid firstToken =
                leasesDispatcher.createLease(authResponse(userId))
                    .map(LeasesActivation::token)
                    .orElseThrow();
            assertThat(leasesDispatcher.requestLease(
                new LeasesRequest(ACQUIRE, new LeasesData(userId, firstToken, album2))
            )).hasValueSatisfying(result -> {
                assertThat(result.token()).isEqualTo(firstToken);
                assertThat(result.trackUUIDs()).describedAs(
                    "Expected two tracks and definitely not album UUID %s",
                    album2
                ).containsExactly(
                    track2a,
                    track2b,
                    track2c
                );
            });

            tick(Duration.ofMinutes(leaseDuration.toMinutes() / 2));

            assertThat(leasesDispatcher.currentLease(authResponse(userId))).hasValueSatisfying(result -> {
                assertThat(result.token()).isEqualTo(firstToken);
                assertThat(result.trackUUIDs()).describedAs(
                    "Expected three tracks and definitely not album UUID %s",
                    album2
                ).containsExactly(
                    track2a,
                    track2b,
                    track2c
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
            Uuid firstToken =
                leasesDispatcher.createLease(authResponse(userId))
                    .map(LeasesActivation::token)
                    .orElseThrow();
            leasesDispatcher.requestLease(new LeasesRequest(ACQUIRE, new LeasesData(userId, firstToken, album2)));
            Track track = new Track(track2b, Track.Format.M4A);
            TrackRange trackRange = new TrackRange(track, new Range(0L, 10L), firstToken);

            assertThat(kudu.library(firstToken)).isPresent();
            assertThat(kudu.library(Uuid.random())).isNotPresent();

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
            assertThat(leasesDispatcher.createLease(authResponse(Uuid.random().digest()))).isEmpty();
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
                    LeasesRequest leasesRequest = new LeasesRequest(
                        ACQUIRE,
                        new LeasesData(
                            userId,
                            result.token(),
                            album1
                        )
                    );
                    assertThat(leasesDispatcher.requestLease(leasesRequest))
                        .hasValueSatisfying(requestResult ->
                            assertThat(requestResult.trackUUIDs()).describedAs(
                                "Expected two tracks and definitely not album UUID %s",
                                album1
                            ).containsExactly(track1a, track1b));
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
                Uuid firstToken =
                    leasesDispatcher.createLease(authResponse(userId))
                        .map(LeasesActivation::token)
                        .orElseThrow();
                assertThat(leasesDispatcher.requestLease(new LeasesRequest(
                    ACQUIRE,
                    new LeasesData(
                        userId,
                        firstToken,
                        album2
                    )
                ))).hasValueSatisfying(result -> {
                    assertThat(result.token()).isEqualTo(firstToken);
                    assertThat(result.trackUUIDs()).describedAs(
                        "Expected two tracks and definitely not album " +
                        "UUID %s",
                        album2
                    ).containsExactly(
                        track2a,
                        track2b,
                        track2c
                    );
                });
                assertThat(leasesDispatcher.currentLease(authResponse(userId))).hasValueSatisfying(result -> {
                    assertThat(result.token()).isEqualTo(firstToken);
                    assertThat(result.trackUUIDs()).describedAs(
                        "Expected three tracks and definitely not album " +
                        "UUID %s",
                        album2
                    ).containsExactly(
                        track2a,
                        track2b,
                        track2c
                    );
                });
            }

            @Test
            void rentAndRelease() {
                Uuid firstToken =
                    leasesDispatcher.createLease(authResponse(userId))
                        .map(LeasesActivation::token)
                        .orElseThrow();
                assertThat(leasesDispatcher.requestLease(new LeasesRequest(
                    ACQUIRE,
                    new LeasesData(
                        userId,
                        firstToken,
                        album2
                    )
                ))).hasValueSatisfying(result -> {
                    assertThat(result.token()).isEqualTo(firstToken);
                    assertThat(result.trackUUIDs()).describedAs(
                        "Expected three tracks and definitely not album " +
                        "UUID %s",
                        album2
                    ).containsExactly(
                        track2a,
                        track2b,
                        track2c
                    );
                });

                assertThat(leasesDispatcher.dismissLease(new LeasesRequest(
                    RELEASE,
                    new LeasesData(
                        userId,
                        firstToken,
                        album2
                    )
                ))).hasValueSatisfying(result ->
                    assertThat(result.trackUUIDs()).isEmpty());
            }

            @Test
            void rentAndPlay() {
                Uuid firstToken = leasesDispatcher.createLease(authResponse(userId))
                    .map(LeasesActivation::token)
                    .orElseThrow();
                assertThat(
                    leasesDispatcher.requestLease(new LeasesRequest(
                        ACQUIRE,
                        new LeasesData(
                            userId,
                            firstToken,
                            album2
                        )
                    ))
                ).hasValueSatisfying(result ->
                    assertThat(result.trackUUIDs()).hasSize(3));

                Track track = new Track(track2b, Track.Format.M4A);
                TrackRange trackRange = new TrackRange(track, new Range(0L, 10L).withLength(256L), firstToken);

                assertThat(kudu.audioBytes(trackRange)).hasValueSatisfying(audioBytes -> {
                    assertThat(audioBytes.chunk()).isEqualTo(new Chunk("m4a", 0L, 10L, 256L));
                    assertThat(audioBytes.bytes()).hasSize(10);
                });
            }
        }
    }
}
