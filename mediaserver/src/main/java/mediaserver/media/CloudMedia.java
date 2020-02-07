package mediaserver.media;

import mediaserver.externals.ACL;
import mediaserver.externals.S3Client;
import mediaserver.externals.S3Connector;
import mediaserver.sessions.Ids;
import mediaserver.util.IO;
import mediaserver.util.Sourced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class CloudMedia {

    private static final Logger log = LoggerFactory.getLogger(CloudMedia.class);

    private static final String MEDIA_SER = "media.ser";

    private CloudMedia() {

    }

    public static void main(String[] args) {

        main(args[0], args[1], args[2]);
    }

    public static List<File> localFiles(String directory, String suffix) {

        return getFiles(new File(directory), suffix).collect(Collectors.toList());
    }

    public static void uploadMissingRemote(
        S3Client s3,
        Map<String, Long> remoteSizes,
        File localFile,
        Predicate<Track> alsoInclude
    ) {

        Track track = new Track(localFile);
        String remoteFlac = track.getUuid() + ".flac";
        String remoteM4a = track.getUuid() + ".m4a";

        Predicate<Track> neverHeardOfIt = alsoInclude.negate();

        Optional<Long> remoteFlacSize =
            Optional.ofNullable(remoteSizes.get(remoteFlac))
                .filter(size ->
                    size == localFile.length() || neverHeardOfIt.test(track));

        File localCompressedFile = track.getCompressedFile();
        Optional<Long> remoteM4aSize =
            Optional.ofNullable(remoteSizes.get(remoteM4a))
                .filter(size -> size == localCompressedFile.length() || neverHeardOfIt.test(track));

        remoteFlacSize.ifPresentOrElse(
            size -> {
//                log.info("Already present with {} bytes: {}/{}/{} / {}",
//                    size, track.getArtist().getName(), track.getAlbum(), track.getName(),
//                    remoteFlac)
            },
            () -> {
                log.info("Uploading {} bytes: {}/{}/{} => {}",
                    localFile.length(), track.getArtist().getName(), track.getAlbum(), track.getName(),
                    remoteFlac);
                s3.put(localFile, remoteFlac);
            });

        remoteM4aSize.ifPresentOrElse(
            size -> {
//                log.debug("Already present with {} bytes: {}/{}/{} / {}",
//                    size, track.getArtist().getName(), track.getAlbum(), track.getName(),
//                    remoteM4a);
            },
            () -> {
                String remoteCompressed = remoteFlac.replaceAll(".flac", ".m4a");
                log.info("Uploading {} bytes: {}/{}/{} => {}",
                    localCompressedFile.length(), track.getArtist().getName(), track.getAlbum(), track.getName(),
                    remoteM4a);
                s3.put(localCompressedFile, remoteCompressed);
            });
    }

    public static Stream<File> getFiles(File file, String suffix) {

        return getFiles(file, f -> f.getName().endsWith(suffix));
    }

    public static Stream<File> getFiles(File file, Predicate<File> predicate) {

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

    public static void uploadMedia(File file, S3Client s3) {

        s3.put(file, MEDIA_SER);
    }

    public static Optional<Instant> lastUpdatedMedia() {

        return lastUpdate(MEDIA_SER);
    }

    public static Optional<Instant> lastUpdatedIds() {

        return lastUpdate(Ids.IDS_RESOURCE);
    }

    public static Optional<Instant> lastUpdate(String object) {

        return S3Connector.get().flatMap(s3 -> {
            try {
                return s3.lastModifiedRemote(object);
            } catch (Exception e) {
                log.warn("Failed", e);
            }
            return Optional.empty();
        });
    }

    public static Media download() {

        InputStream inputStream = stream(MEDIA_SER);
        log.info("Downloading media... ");
        Media media = deserialize(inputStream);
        log.info("Downloaded media {}", media);
        return media;
    }

    public static boolean updatedFromRemote(String name) {

        return S3Connector.get().map(s3 -> {
            Sourced<String> localResource = IO.readUTF8(name);
            if (localResource.sourceType() == Sourced.Type.SOURCES) {
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

    public static ACL acl() {

        InputStream inputStream = stream(Ids.IDS_RESOURCE);
        log.info("Downloading ids... ");
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

    private static void updateLocal(Path localPath, String resource) {

        List<String> lines = new BufferedReader(new InputStreamReader(stream(resource), StandardCharsets.UTF_8))
            .lines()
            .collect(Collectors.toList());
        log.info("Updating local file {} with remote content", localPath);
        try {
            Files.write(localPath, lines);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write to " + localPath, e);
        }
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

    private static void main(String root, String library, String resources, String... albumIncludes) {

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
            localFiles(root, ".flac").forEach(localFile ->
                uploadMissingRemote(
                    s3,
                    remoteSizes,
                    localFile,
                    track ->
                        Arrays.stream(albumIncludes).anyMatch(inc ->
                            track.getAlbum().toLowerCase().contains(inc))));

            List<String> removables = redundantRemotes(".flac", root, remoteSizes);
            if (!removables.isEmpty()) {
                s3.remove(removables);
            }
//            String m4aRoot = new File(new File(root).getParentFile().getParentFile(), "M4A").getAbsolutePath();
//            List<String> removableM4as = redundantRemotes(".m4a", m4aRoot, remoteSizes);
//            if (!removables.isEmpty()) {
//                removeRedundantRemotes(s3, removableM4as);
//            }
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

    private static List<String> redundantRemotes(String suffix, String arg, Map<String, Long> remoteSizes) {

        Collection<String> localTracks =
            localFiles(arg, suffix).stream()
                .map(Track::new)
                .map(track ->
                    track.getUuid() + suffix)
                .collect(Collectors.toSet());
        return remoteSizes.keySet().stream()
            .filter(remoteName ->
                remoteName.endsWith(suffix))
            .filter(remoteName ->
                !localTracks.contains(remoteName))
            .collect(Collectors.toList());
    }

    private static File serialize(Media media) {

        try {
            Path tmp = Files.createTempFile
                ("media-" + UUID.randomUUID(), "ser");
            try (
                FileOutputStream fos = new FileOutputStream(tmp.toFile());
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                ObjectOutputStream oos = new ObjectOutputStream(bos)
            ) {
                oos.writeObject(media);
            }
            return tmp.toFile();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate upload", e);
        }
    }
}
