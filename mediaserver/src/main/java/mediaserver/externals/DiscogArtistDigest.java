package mediaserver.externals;

import java.net.URI;

@SuppressWarnings("InstanceVariableMayNotBeInitialized")
public final class DiscogArtistDigest {

    private Long id;

    private String name;

    private String role;

    private URI uri;

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
