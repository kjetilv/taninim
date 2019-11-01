package mediaserver.files;

public class TrackContext {

    private final String position;

    private final String title;

    private final Credits credits;

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
