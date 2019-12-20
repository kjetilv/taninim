package mediaserver.media;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Optional;

public class TrackContext implements Serializable {

    private static final Logger log = LoggerFactory.getLogger(TrackContext.class);

    private final Track track;

    private final String position;

    private final String title;

    private final Credits credits;

    private static final long serialVersionUID = -1080380514657171993L;

    public TrackContext(String position, String title, Credits credits) {

        this(null, position, title, credits);
    }

    private TrackContext(Track track, String position, String title, Credits credits) {

        this.track = track;
        this.position = position;
        this.title = title;
        this.credits = credits == null ? new Credits() : credits;
    }

    public TrackContext withTrack(Track track) {

        String name = track.getName().toLowerCase().replaceAll("\\s+", "");
        String title = this.title.toLowerCase().replaceAll("\\s+", "");
        if (!(name.equalsIgnoreCase(title) || name.contains(title) || title.contains(name))) {
            log.debug("{} /= {}", track, this.title);
        }
        return new TrackContext(track, position, this.title, credits);
    }

    public Track getTrack() {

        return track;
    }

    public Optional<Integer> getDisc() {

        if (position != null && position.contains("-")) {
            return firstPart("-");
        }
        if (position != null && position.contains(".")) {
            return firstPart(".");
        }
        return Optional.empty();
    }

    public boolean isHeading() {

        return position == null || position.isBlank();
    }

    public boolean isTrack() {

        return position != null && !position.isBlank();
    }

    public Optional<String> getHeading() {

        return Optional.ofNullable(title);
    }

    public String getPrettyTrackNo() {

        return getTrackNo().map(String::valueOf).orElse("?");
    }

    public Optional<Integer> getTrackNo() {

        return position == null || position.isBlank() ? Optional.empty()
            : position.contains("-") ? secondPart("-")
            : position.contains(".") ? secondPart(".")
            : toInt(position);
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

    private Optional<Integer> firstPart(String str) {

        return toInt(position.substring(0, position.indexOf(str)));
    }

    private static Optional<Integer> toInt(String substring) {

        try {
            return Optional.of(Integer.parseInt(substring));
        } catch (NumberFormatException e) {
            log.warn("Bogus int {}", substring);
            String pruned = substring.replaceAll("[^\\d]?", "");
            return pruned.isBlank() ? Optional.empty() : toInt(pruned);
        }
    }

    private Optional<Integer> secondPart(String s) {

        return toInt(position.substring(position.lastIndexOf(s) + 1));
    }
}
