package mediaserver.files;

public class Artist extends AbstractNameHashable {

    private static final long serialVersionUID = -6584400668175206925L;

    private Artist(String name) {

        super(name);
    }

    public static Artist get(String name) {

        return AbstractNameHashable.get(Artist::new, name);
    }
}
