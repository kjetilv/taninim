package taninim.taninim.music.medias;

import java.io.DataInput;
import java.io.DataOutput;
import java.util.Collections;
import java.util.List;

import com.github.kjetilv.uplift.kernel.io.BinaryWritable;
import com.github.kjetilv.uplift.kernel.io.BytesIO;
import com.github.kjetilv.uplift.kernel.uuid.Uuid;

import static java.util.Objects.requireNonNull;

public record AlbumTrackIds(
    Uuid title,
    List<Uuid> tracks
) implements BinaryWritable {

    static AlbumTrackIds from(DataInput input) {
        try {
            Uuid title = Uuid.read(input);
            return new AlbumTrackIds(title, BytesIO.readUuids(input));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public AlbumTrackIds(Uuid title, List<Uuid> tracks) {
        this.title = requireNonNull(title, "title");
        this.tracks = tracks == null || tracks.isEmpty() ? Collections.emptyList() : tracks;
    }

    @Override
    public int writeTo(DataOutput dos) {
        return BytesIO.writeUuid(dos, title) + BytesIO.writeUuids(dos, tracks);
    }
}
