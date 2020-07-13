package mediaserver.gui;

import java.util.Collection;

import mediaserver.util.DAC;

class PlayableGroup {

    private final String name;

    private final Collection<Playable> playables;

    PlayableGroup(String name, Collection<Playable> playables) {
        this.name = name == null || name.isBlank() ? null : name.trim();
        this.playables = playables;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + name + ": " + playables.size() + " tracks]";
    }

    public String getName() {
        return name;
    }

    @DAC
    public Collection<Playable> getPlayables() {
        return playables;
    }
}
