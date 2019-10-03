package mediaserver.dto;

public class AudioAlbum {

    private String artist;

    private String name;

    private String uuid;

    private AudioTrack[] files;

    public String getName() {
        return name;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public AudioTrack[] getFiles() {
        return files;
    }

    public void setFiles(AudioTrack[] files) {
        this.files = files;
    }
}
