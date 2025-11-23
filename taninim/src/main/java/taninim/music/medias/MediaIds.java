package taninim.music.medias;

import com.github.kjetilv.uplift.hash.Hash;
import com.github.kjetilv.uplift.kernel.io.BinaryWritable;
import com.github.kjetilv.uplift.kernel.io.BytesIO;
import taninim.util.Maps;

import java.io.DataInput;
import java.io.DataOutput;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static com.github.kjetilv.uplift.hash.HashKind.K128;

public record MediaIds(List<AlbumTrackIds> albumTrackIds) implements BinaryWritable {

    public static MediaIds from(DataInput input) {
        try {
            var albumTrackIds = IntStream.range(0, input.readInt())
                .mapToObj(__ -> AlbumTrackIds.from(input))
                .toList();
            return new MediaIds(albumTrackIds);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public MediaIds() {
        this(null);
    }

    @Override
    public int writeTo(DataOutput dos) {
        return BytesIO.writeWritables(dos, albumTrackIds);
    }

    public Map<Hash<K128>, List<Hash<K128>>> albumTracks() {
        return Maps.toMap(albumTrackIds, AlbumTrackIds::title, AlbumTrackIds::tracks);
    }
}
