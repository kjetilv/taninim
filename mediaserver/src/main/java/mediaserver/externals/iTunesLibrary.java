package mediaserver.externals;

import java.util.Map;

public class iTunesLibrary {

    public Map<String, iTunesTrack> Tracks;

    public Map<String, iTunesTrack> getTracks() {

        return Tracks;
    }

    public void setTracks(Map<String, iTunesTrack> tracks) {

        Tracks = tracks;
    }
}
