package mediaserver.gui;

import java.util.Collection;

public class PlayableGroup {

    private final String name;

    private final Collection<Playable> playables;

    public PlayableGroup(String name, Collection<Playable> playables) {

        this.name = name == null || name.isBlank() ? null : name.trim();
        this.playables = playables;
    }

    public String getName() {

        return name;
    }

    public Collection<Playable> getPlayables() {

        return playables;
    }

    @Override
    public String toString() {

        return getClass().getSimpleName() + "[" + name + ": " + playables.size() + " tracks]";
    }
}
