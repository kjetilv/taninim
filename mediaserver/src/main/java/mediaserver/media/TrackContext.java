package mediaserver.media;

import java.io.Serializable;
import java.util.Optional;
import java.util.regex.Pattern;

import mediaserver.util.DAC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TrackContext implements Serializable {

    private static final Logger log = LoggerFactory.getLogger(TrackContext.class);

    private final Track track;

    private final String position;

    private final String title;

    private final Credits credits;

    TrackContext(String position, String title, Credits credits) {
        this(null, position, title, credits);
    }

    private TrackContext(Track track, String position, String title, Credits credits) {
        this.track = track;
        this.position = position;
        this.title = title;
        this.credits = credits == null ? new Credits() : credits;
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

    @DAC
    public String getPrettyTrackNo() {
        return getTrackNo().map(String::valueOf).orElse("?");
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

    TrackContext withTrack(Track track) {
        String name = WS.matcher(track.getName().toLowerCase()).replaceAll(NONE);
        String title = WS.matcher(this.title.toLowerCase()).replaceAll(NONE);
        if (!(name.equalsIgnoreCase(title) || name.contains(title) || title.contains(name))) {
            log.trace("{} /= {}", track, this.title);
        }
        return new TrackContext(track, position, this.title, credits);
    }

    Optional<Integer> getTrackNo() {
        return position == null || position.isBlank() ? Optional.empty()
            : position.contains("-") ? secondPart("-")
                : position.contains(".") ? secondPart(".")
                    : toInt(position);
    }

    private Optional<Integer> firstPart(String str) {
        return toInt(position.substring(0, position.indexOf(str)));
    }

    private Optional<Integer> secondPart(String s) {
        return toInt(position.substring(position.lastIndexOf(s) + 1));
    }

    private static final long serialVersionUID = -1080380514657171993L;

    private static final Pattern WS = Pattern.compile("\\s+");

    private static final String NONE = "";

    private static final Pattern NO_NUMS = Pattern.compile("[^\\d]?");

    private static Optional<Integer> toInt(String substring) {
        try {
            return Optional.of(Integer.parseInt(substring));
        } catch (NumberFormatException e) {
            String pruned = NO_NUMS.matcher(substring).replaceAll(NONE);
            Optional<Integer> integer = pruned.isBlank() ? emergency(substring) : toInt(pruned);
            log.debug("Bogus int {}, fallback {}", substring, integer.orElse(null));
            return integer;
        }
    }
    
    private static Optional<Integer> emergency(String substring) {
        if ("a".equalsIgnoreCase(substring)) {
            return Optional.of(1);
        }
        if ("b".equalsIgnoreCase(substring)) {
            return Optional.of(2);
        }
        return Optional.empty();
    }
}
