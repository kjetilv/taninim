package mediaserver.http;

import java.util.regex.Pattern;

import mediaserver.hash.Hashable;
import mediaserver.hash.Namable;

public class Link<T extends Hashable & Namable> {

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

    public String getName() {
        return WS.matcher(target.getName()).replaceAll("&nbsp;");
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

    private static final Pattern WS = Pattern.compile("\\s+");
}
