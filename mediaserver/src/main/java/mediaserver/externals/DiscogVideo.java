package mediaserver.externals;

import java.net.URI;

public class DiscogVideo {

    private String description;

    private String title;

    private URI uri;

    public String getDescription() {

        return description;
    }

    public void setDescription(String description) {

        this.description = description;
    }

    public String getTitle() {

        return title;
    }

    public void setTitle(String title) {

        this.title = title;
    }

    public URI getUri() {

        return uri;
    }

    public void setUri(URI uri) {

        this.uri = uri;
    }
}
