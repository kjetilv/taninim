package mediaserver.externals;

import java.net.URI;

@SuppressWarnings("InstanceVariableMayNotBeInitialized")
public final class DiscogImage {

    private URI uri;

    private URI uri150;

    private int height;

    private int width;

    private String type;

    public URI getUri() {

        return uri;
    }

    public void setUri(URI uri) {

        this.uri = uri;
    }

    public URI getUri150() {

        return uri150;
    }

    public void setUri150(URI uri150) {

        this.uri150 = uri150;
    }

    public int getHeight() {

        return height;
    }

    public void setHeight(int height) {

        this.height = height;
    }

    public int getWidth() {

        return width;
    }

    public void setWidth(int width) {

        this.width = width;
    }

    public String getType() {

        return type;
    }

    public void setType(String type) {

        this.type = type;
    }
}
