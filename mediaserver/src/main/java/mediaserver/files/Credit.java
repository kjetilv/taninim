package mediaserver.files;

import java.io.Serializable;
import java.net.URI;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

public class Credit implements Serializable {

    private final URI uri;

    private final ExternalType externalType;

    private final String sourceType;

    private final String name;

    private static final long serialVersionUID = 1917638609632123791L;

    public Credit(
        String name,
        URI uri,
        String sourceType,
        ExternalType externalType
    ) {

        this.name = name;
        this.uri = uri;
        this.externalType = externalType;
        this.sourceType = sourceType == null || sourceType.isBlank() ? "" : sourceType;
    }

    public String getSourceType() {

        return sourceType == null || sourceType.isBlank() ? null : sourceType;
    }

    public boolean isPerformer() {

        return externalType == null;
    }

    public Artist getArtist() {

        return Artist.get(name);
    }

    public ExternalType getExternalType() {

        return externalType;
    }

    public String getName() {

        return name;
    }

    public Collection<Credit> getCompositeCredits() {

        return getArtist().getCompositeArtists().stream()
            .map(artist ->
                new Credit(artist.getName(), uri, sourceType, externalType))
            .collect(Collectors.toList());
    }

    public boolean isEmpty() {

        return sourceType.isBlank();
    }

    enum ExternalType {

        arranged_by,

        producer,

        co_producer,

        produced_by,

        co_produced,

        mixed_by,

        recorded_by,

        mastered_by,

        remastered_by,

        engineer,

        executive,

        concept_by,

        design,

        cover_by,

        artwork,

        illustration,

        photography,

        typography,

        painting,

        legal,

        liner_notes;

        boolean matches(String type) {

            return normalized(type.toLowerCase()).startsWith(normalized(name()));
        }

        String normalized(String name) {

            return name.toLowerCase()
                .replace('-', ' ')
                .replace('_', ' ')
                .replace(':', ' ');
        }
    }

    @Override
    public int hashCode() {

        return Objects.hash(name, uri, externalType, sourceType);
    }

    @Override
    public boolean equals(Object o) {

        return this == o ||
            o instanceof Credit &&
                Objects.equals(name, ((Credit) o).name) &&
                Objects.equals(uri, ((Credit) o).uri) &&
                externalType == ((Credit) o).externalType &&
                Objects.equals(sourceType, ((Credit) o).sourceType);
    }

    @Override
    public String toString() {

        return getClass().getSimpleName() +
            "[" + name + ": " + sourceType + "/" + externalType + "]";
    }
}
