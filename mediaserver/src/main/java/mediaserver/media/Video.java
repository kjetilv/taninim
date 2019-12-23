package mediaserver.media;

import java.io.Serializable;
import java.net.URI;

public final class Video implements Serializable {

    private final String title;

    private final String description;

    private final URI uri;

    private static final long serialVersionUID = 6596987424481176891L;

    public Video(String title, String description, URI uri) {

        this.title = title;
        this.description = description;
        String id = uri.toString().substring(uri.toString().lastIndexOf("=") + 1);
        this.uri = URI.create("https://www.youtube.com/embed/" + id);
    }

    public String getTitle() {

        return title;
    }

    public String getDescription() {

        return description;
    }

    public URI getUri() {

        return uri;
    }

    @Override
    public String toString() {

        return getClass().getSimpleName() + "[" + title + "]";
    }
}
