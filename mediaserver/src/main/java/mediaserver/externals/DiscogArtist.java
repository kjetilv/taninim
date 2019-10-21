package mediaserver.externals;

import java.net.URI;

public class DiscogArtist {

    public Long id;

    public String name;

    public String role;

    public URI uri;

    public Long getId() {

        return id;
    }

    public void setId(Long id) {

        this.id = id;
    }

    public String getName() {

        return name;
    }

    public void setName(String name) {

        this.name = name;
    }

    public String getRole() {

        return role;
    }

    public void setRole(String role) {

        this.role = role;
    }

    public URI getUri() {

        return uri;
    }

    public void setUri(URI uri) {

        this.uri = uri;
    }
}
