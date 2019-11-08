package mediaserver.files;

import java.io.Serializable;

public class TrackContext implements Serializable  {

    private final String position;

    private final String title;

    private final Credits credits;

    private static final long serialVersionUID = -1080380514657171993L;

    public TrackContext(String position, String title, Credits credits) {

        this.position = position;
        this.title = title;
        this.credits = credits == null ? new Credits() : credits;
    }

    public String getPosition() {

        return position;
    }

    public String getTitle() {

        return title;
    }

    public Credits getCredits() {

        return credits;
    }
}
