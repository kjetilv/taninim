package taninim.music;

import module java.base;

public interface Archives {

    void storeRecord(ArchivedRecord archivedRecord);

    Optional<ArchivedRecord> retrieveRecord(String path);

    Stream<String> retrievePaths(String prefix, Predicate<? super String> filter);

    void clearRecords(Collection<String> paths);
}
