package mediaserver.dto;

import java.util.Collection;

public class AudioAlbum {

    private String artist;

    private String name;

    private String uuid;

    private Collection<AudioTrack> tracks;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Collection<AudioTrack> getTracks() {
        return tracks;
    }

    public void setTracks(Collection<AudioTrack> tracks) {
        this.tracks = tracks;
    }
}
