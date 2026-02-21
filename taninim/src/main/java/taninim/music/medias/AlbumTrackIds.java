package taninim.music.medias;

import module java.base;
import com.github.kjetilv.uplift.hash.Hash;
import com.github.kjetilv.uplift.kernel.io.BinaryWritable;
import com.github.kjetilv.uplift.kernel.io.BytesIO;

import static com.github.kjetilv.uplift.hash.HashKind.K128;
import static java.util.Objects.requireNonNull;

public record AlbumTrackIds(
    Hash<K128> title,
    List<Hash<K128>> tracks
) implements BinaryWritable {

    static AlbumTrackIds from(DataInput input) {
        try {
            var title = Hash.of(input, K128);
            return new AlbumTrackIds(title, BytesIO.readHashes128(input));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public AlbumTrackIds(Hash<K128> title, List<Hash<K128>> tracks) {
        this.title = requireNonNull(title, "title");
        this.tracks = tracks == null || tracks.isEmpty() ? Collections.emptyList() : tracks;
    }

    @Override
    public int writeTo(DataOutput dos) {
        return BytesIO.writeHash128(dos, title) + BytesIO.writeHashes128(dos, tracks);
    }
}
