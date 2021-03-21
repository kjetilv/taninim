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

    @DAC
    public String DateAdded;

    @DAC
    public String DateModified;

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

    public String getDateAdded() {
        return DateAdded;
    }

    public void setDateAdded(String dateAdded) {
        DateAdded = dateAdded;
    }

    public String getDateModified() {
        return DateModified;
    }

    public void setDateModified(String dateModified) {
        DateModified = dateModified;
    }
}
