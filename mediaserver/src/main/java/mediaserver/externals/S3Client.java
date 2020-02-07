package mediaserver.externals;

import java.io.File;
import java.io.InputStream;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public interface S3Client {

    InputStream stream(String name, Long offset, Long length);

    InputStream stream(String name);

    void put(String contents, String remoteName);

    void put(File localFile, String remoteName);

    void remove(Collection<String> objects);

    Map<String, Long> remoteSizes();

    Optional<Instant> lastModifiedRemote(String name);

    Optional<Long> length(String name);

    void put(InputStream inputStream, long length, String remoteName);
}
