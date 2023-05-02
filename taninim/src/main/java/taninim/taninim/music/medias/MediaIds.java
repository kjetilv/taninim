package taninim.taninim.music.medias;

import java.io.DataInput;
import java.io.DataOutput;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import com.github.kjetilv.uplift.kernel.io.BinaryWritable;
import com.github.kjetilv.uplift.kernel.io.BytesIO;
import com.github.kjetilv.uplift.kernel.util.Maps;
import com.github.kjetilv.uplift.kernel.uuid.Uuid;

public record MediaIds(List<AlbumTrackIds> albumTrackIds) implements BinaryWritable {

    public static MediaIds from(DataInput input) {
        try {
            List<AlbumTrackIds> albumTrackIds = IntStream.range(0, input.readInt())
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

    public Map<Uuid, List<Uuid>> albumTracks() {
        return Maps.toMap(albumTrackIds, AlbumTrackIds::title, AlbumTrackIds::tracks);
    }
}
