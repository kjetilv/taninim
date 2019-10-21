package mediaserver.files;

import java.net.URI;
import java.util.Objects;

public class Credit {

    private final URI uri;

    private final ExternalType externalType;

    private final String musicalType;

    private final String name;

    public Credit(
        String name,
        URI uri,
        String musicalType,
        ExternalType externalType
    ) {

        this.name = name;
        this.uri = uri;
        this.externalType = externalType;
        this.musicalType = musicalType;
    }

    public String getMusicalType() {

        return musicalType;
    }

    public ExternalType getExternalType() {

        return externalType;
    }

    public String getName() {

        return name;
    }

    enum ExternalType {

        composer,

        composed,

        written,

        arranged,

        producer,

        produced,

        engineer,

        executive,

        design,

        photography,

        typography,

        painting;

        boolean matches(String type) {

            return normalized(type.toLowerCase()).startsWith(normalized(name()));
        }

        String normalized(String name) {

            return name.toLowerCase()
                .replace('-', ' ')
                .replace(':', ' ');
        }
    }

    @Override
    public int hashCode() {

        return Objects.hash(name, uri, externalType, musicalType);
    }

    @Override
    public boolean equals(Object o) {

        return this == o ||
            o instanceof Credit &&
                Objects.equals(name, ((Credit) o).name) &&
                Objects.equals(uri, ((Credit) o).uri) &&
                externalType == ((Credit) o).externalType &&
                Objects.equals(musicalType, ((Credit) o).musicalType);
    }
}
