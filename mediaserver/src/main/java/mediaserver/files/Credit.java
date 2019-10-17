package mediaserver.files;

import java.util.Objects;

public class Credit {

    private final Type type;

    private final String otherType;

    private final String name;

    public Credit(Type type, String otherType, String name) {

        this.type = type;
        this.otherType = otherType;
        this.name = name;
    }

    public String getOtherType() {

        return otherType;
    }

    public Type getType() {

        return type;
    }

    public String getName() {

        return name;
    }

    enum Type {

        composer,

        composed_by,

        written_by,

        arranged_by,

        producer,

        engineer,

        executive_producer,

        design,

        painting;

        boolean matches(String type) {

            return normalized(type.toLowerCase())
                .startsWith(normalized(name()));
        }

        String normalized(String name) {

            return name.toLowerCase()
                .replace('-', ' ')
                .replace(':', ' ');
        }
    }

    @Override
    public int hashCode() {

        return Objects.hash(type, otherType);
    }

    @Override
    public boolean equals(Object o) {

        return this == o ||
            o instanceof Credit &&
                type == ((Credit) o).type &&
                Objects.equals(otherType, ((Credit) o).otherType);
    }
}
