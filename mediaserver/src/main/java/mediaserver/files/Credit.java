package mediaserver.files;

import java.net.URI;
import java.util.Objects;

public class Credit {

    private final URI uri;

    private final ExternalType externalType;

    private final String sourceType;

    private final String name;

    public Credit(
        String name,
        URI uri,
        String sourceType,
        ExternalType externalType
    ) {

        this.name = name;
        this.uri = uri;
        this.externalType = externalType;
        this.sourceType = sourceType;
    }

    public String getSourceType() {

        return sourceType;
    }

    public boolean isPerformer() {

        return externalType == null;
    }

    public ExternalType getExternalType() {

        return externalType;
    }

    public String getName() {

        return name;
    }

    enum ExternalType {

        composer,

        composed_by,

        music_by,

        written_by,

        arranged_by,

        producer,

        co_producer,

        produced_by,

        co_produced,

        mixed_by,

        recorded_by,

        mastered_by,

        engineer,

        executive,

        concept_by,

        design,

        artwork,

        illustration,

        photography,

        typography,

        painting,

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
}
