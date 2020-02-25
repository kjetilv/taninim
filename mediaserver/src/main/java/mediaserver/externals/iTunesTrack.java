package mediaserver.externals;

import mediaserver.util.DAC;

public final class iTunesTrack {

    @DAC
    public String Artist;

    @DAC
    public String Album;

    @DAC
    public String Name;

    @DAC
    public String Comments;

    public String getArtist() {

        return Artist;
    }

    public void setArtist(String artist) {

        Artist = artist;
    }

    public String getAlbum() {

        return Album;
    }

    public void setAlbum(String album) {

        Album = album;
    }

    public String getName() {

        return Name;
    }

    public void setName(String name) {

        Name = name;
    }

    public String getComments() {

        return Comments;
    }

    public void setComments(String comments) {

        Comments = comments;
    }
}
