package mediaserver.media;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import mediaserver.util.DAC;

final class TrackGroup implements Serializable {

    private final String name;

    private final List<TrackContext> trackContexts;

    TrackGroup(String name, List<TrackContext> trackContexts) {
        this.name = name;
        this.trackContexts = trackContexts == null || trackContexts.isEmpty()
            ? Collections.emptyList()
            : List.copyOf(trackContexts);
    }

    public String getName() {
        return name;
    }

    @DAC
    public List<TrackContext> getTrackContexts() {
        return trackContexts;
    }

    private static final long serialVersionUID = -1742521380844081944L;
}
