package mediaserver.media;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import mediaserver.externals.ACL;
import mediaserver.externals.S3Client;
import mediaserver.externals.S3Connector;
import mediaserver.sessions.Ids;
import mediaserver.util.IO;
import mediaserver.util.Sourced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CloudMedia {

    private static final Logger log = LoggerFactory.getLogger(CloudMedia.class);

    private CloudMedia() {
    }

    public static void main(String[] args) {
        run(args[0], args[1], args[2]);
    }

    public static Optional<Instant> lastUpdatedMedia() {
        return lastMediaUpdate();
    }

    public static Media download() {
        InputStream inputStream = stream(MEDIA_SER);
        log.info("Downloading media... ");
        Media media = deserialize(inputStream);
        log.info("Downloaded media {}", media);
        return media;
    }

    public static ACL acl() {
        log.info("Downloading ids... ");
        InputStream inputStream = stream(Ids.IDS_RESOURCE);
        ACL ids = IO.read(ACL.class, Ids.IDS_RESOURCE, inputStream);
        log.info("Downloaded ids {}", ids);
        return ids;
    }

    public static void updateLocals(String... strings) {
        Stream.of(strings)
            .forEach(resource -> {
                if (updatedFromRemote(resource)) {
                    log.info("Updated local: {}", resource);
                } else {
                    log.info("Local is current: {}", resource);
                }
            });
    }

    private static final String MEDIA_SER = "media.ser";

    private static final String DOT_FLAC = ".flac";

    private static final String DOT_M4A = ".m4a";

    private static final Pattern PAT_FLAC = Pattern.compile(DOT_FLAC);

    private static List<File> localFiles(String directory) {
        return getFiles(new File(directory)).collect(Collectors.toList());
    }

    private static void uploadMissingRemote(
        S3Client s3,
        Map<String, Long> remoteSizes,
        File localFile,
        Predicate<Track> alsoInclude
    ) {
        Track track = new Track(localFile);
        String remoteFlac = track.getUuid() + DOT_FLAC;
        String remoteM4a = track.getUuid() + DOT_M4A;
        Predicate<Track> neverHeardOfIt = alsoInclude.negate();
        Optional<Long> remoteFlacSize =
            Optional.ofNullable(remoteSizes.get(remoteFlac))
                .filter(size ->
                    size == localFile.length() || neverHeardOfIt.test(track));
        long localCompressedSize = track.getCompressedSize();
        Optional<Long> remoteM4aSize =
            Optional.ofNullable(remoteSizes.get(remoteM4a))
                .filter(size -> size == localCompressedSize || neverHeardOfIt.test(track));
        remoteFlacSize.ifPresentOrElse(
            size -> {
//                log.info("Already present with {} bytes: {}/{}/{} / {}",
//                    size, track.getArtist().getName(), track.getAlbum(), track.getName(),
//                    remoteFlac)
            },
            () -> {
                logUploading(track, remoteFlac, localFile.length());
                s3.put(localFile, remoteFlac);
            });
        remoteM4aSize.ifPresentOrElse(
            size -> {
//                log.debug("Already present with {} bytes: {}/{}/{} / {}",
//                    size, track.getArtist().getName(), track.getAlbum(), track.getName(),
//                    remoteM4a);
            },
            () -> {
                String remoteCompressed = PAT_FLAC.matcher(remoteFlac).replaceAll(DOT_M4A);
                logUploading(track, remoteM4a, localCompressedSize);
                s3.put(track.getCompressedFile().toFile(), remoteCompressed);
            });
    }

    private static void logUploading(Track track, String remoteFlac, long length) {
        log.info("Uploading {} bytes: {}/{}/{} => {}",
            length, track.getArtist().getName(), track.getAlbum(), track.getName(), remoteFlac);
    }

    private static Stream<File> getFiles(File file) {
        return getFiles(file, f -> f.getName().endsWith(CloudMedia.DOT_FLAC));
    }

    private static Stream<File> getFiles(File file, Predicate<? super File> predicate) {
        if (file.isFile()) {
            if (predicate.test(file)) {
                return Stream.of(file);
            }
            return Stream.empty();
        }
        return Optional.ofNullable(file.listFiles()).stream().flatMap(Arrays::stream)
            .flatMap(f ->
                getFiles(f, predicate));
    }

    private static void uploadMedia(File file, S3Client s3) {
        s3.put(file, MEDIA_SER);
    }

    private static Optional<Instant> lastMediaUpdate() {
        return S3Connector.get().flatMap(s3 -> {
            try {
                return s3.lastModifiedRemote(CloudMedia.MEDIA_SER);
            } catch (Exception e) {
                log.warn("Failed", e);
            }
            return Optional.empty();
        });
    }

    private static boolean updatedFromRemote(String name) {
        return S3Connector.get().map(s3 -> {
            Sourced<String> localResource = IO.readUTF8(name);
            if (localResource.sourceType() == IO.Type.SOURCES) {
                Path localPath = localPath(localResource);
                Instant lastModifiedTimeLocal = lastModifiedLocal(localPath);
                Optional<Instant> remoteUpdate = s3.lastModifiedRemote(name)
                    .filter(lastModifiedRemote ->
                        lastModifiedRemote.isAfter(lastModifiedTimeLocal));
                if (remoteUpdate.isPresent()) {
                    updateLocal(localPath, name);
                }
                return remoteUpdate.isPresent();
            }
            return false;
        }).orElse(false);
    }

    private static void updateLocal(Path localPath, String resource) {
        List<String> lines;
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(stream(resource), StandardCharsets.UTF_8))) {
            lines = bufferedReader.lines().collect(Collectors.toList());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to close " + resource, e);
        }
        log.info("Updating local file {} with remote content", localPath);
        try {
            Files.write(localPath, lines);
        } catch (IOException e) {
            failWrite(localPath, e);
        }
    }

    private static void failWrite(Path localPath, IOException e) {
        throw new IllegalStateException("Failed to write to " + localPath, e);
    }

    private static Path localPath(Sourced<String> localResource) {
        return new File(localResource.getUrl().getFile()).toPath();
    }

    private static Instant lastModifiedLocal(Path localPath) {
        try {
            return Files.getLastModifiedTime(localPath).toInstant();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to stat local time", e);
        }
    }

    private static InputStream stream(String name) {
        return S3Connector.get().map(s3 -> {
            try {
                return s3.stream(name);
            } catch (Exception e) {
                throw new IllegalStateException("Could not download media file", e);
            }
        }).orElseThrow(() ->
            new IllegalStateException("No S3 connection"));
    }

    private static Media deserialize(InputStream inputStream) {
        try (
            BufferedInputStream bis = new BufferedInputStream(inputStream);
            ObjectInputStream ois = new ObjectInputStream(bis)
        ) {
            return (Media) ois.readObject();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read media", e);
        }
    }

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    private static void run(String root, String library, String resources, String... albumIncludes) {
        Media media = Media.local(
            new File(root).toPath(),
            new File(library).toPath(),
            new File(resources).toPath());
        File mediaFile = serialize(media);
        S3Connector.get().ifPresent(s3 -> {
            updateLocals(Ids.IDS_RESOURCE, PlaylistYaml.CURATED_RESOURCE);
            System.out.println("Refreshing media");
            uploadMedia(mediaFile, s3);
            System.out.println("Refreshing ids");
            uploadIds(s3);
            System.out.println("Refreshing curations");
            uploadCurations(s3);
            Map<String, Long> remoteSizes = s3.remoteSizes();
            localFiles(root).forEach(localFile ->
                uploadMissingRemote(
                    s3,
                    remoteSizes,
                    localFile,
                    track ->
                        Arrays.stream(albumIncludes).anyMatch(inc ->
                            track.getAlbum().toLowerCase().contains(inc))));
            List<String> removables = redundantRemotes(root, remoteSizes);
            if (!removables.isEmpty()) {
                s3.remove(removables);
            }
        });
    }

    private static void uploadCurations(S3Client s3) {
        uploadResource(s3, PlaylistYaml.CURATED_RESOURCE);
    }

    private static void uploadIds(S3Client s3) {
        uploadResource(s3, Ids.IDS_RESOURCE);
    }

    private static void uploadResource(S3Client s3, String res) {
        Optional.ofNullable(Thread.currentThread().getContextClassLoader().getResource(res))
            .ifPresentOrElse(
                resource ->
                    s3.put(new File(resource.getFile()), res),
                () ->
                    log.warn("Found no {} file", res));
    }

    private static List<String> redundantRemotes(String arg, Map<String, Long> remoteSizes) {
        Collection<String> localTracks =
            localFiles(arg).stream()
                .map(Track::new)
                .map(track -> track.getUuid() + DOT_FLAC)
                .collect(Collectors.toSet());
        return remoteSizes.keySet().stream()
            .filter(remoteName ->
                remoteName.endsWith(DOT_FLAC))
            .filter(remoteName ->
                !localTracks.contains(remoteName))
            .collect(Collectors.toList());
    }

    private static File serialize(Media media) {
        try {
            Path tmp = Files.createTempFile
                ("media-" + UUID.randomUUID(), "ser");
            try (
                OutputStream fos = new FileOutputStream(tmp.toFile());
                OutputStream bos = new BufferedOutputStream(fos);
                ObjectOutput oos = new ObjectOutputStream(bos)
            ) {
                oos.writeObject(media);
            }
            return tmp.toFile();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate upload", e);
        }
    }
}
