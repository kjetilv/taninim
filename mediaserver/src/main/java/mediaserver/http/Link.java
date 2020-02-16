package mediaserver.http;

public class Link<T> {

    private final T target;

    private final String add;

    private final String remove;

    private final String focus;

    public Link(T target, String add, String remove, String focus) {

        this.target = target;
        this.add = add;
        this.remove = remove;
        this.focus = focus;
    }

    public T getTarget() {

        return target;
    }

    public String getAdd() {

        return add;
    }

    public String getRemove() {

        return remove;
    }

    public String getFocus() {

        return focus;
    }
}
