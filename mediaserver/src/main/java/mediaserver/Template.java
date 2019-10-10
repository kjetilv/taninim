package mediaserver;

import mediaserver.util.IO;
import mediaserver.util.MostlyOnce;
import org.stringtemplate.v4.ST;

import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

public class Template {

    private ST st;

    private Supplier<String> result;

    private Supplier<byte[]> bytes;

    public Template(IO io, String resource) {
        st = st(io, resource);
        result = MostlyOnce.get(() ->
            st.render());
        bytes = MostlyOnce.get(() ->
            result.get().getBytes(StandardCharsets.UTF_8));
    }

    public byte[] bytes() {
        return this.bytes.get();
    }

    public int getContentLength() {
        return bytes.get().length;
    }

    Template add(String name, Object value) {
        st.add(name, value);
        return this;
    }

    private static ST st(IO io, String resource) {
        return io.read(resource).map(data ->
            new ST(data, '{', '}')
        ).orElseThrow(() ->
            new IllegalArgumentException("Invalid template: " + resource));
    }

    public String toString() {
        return result.get();
    }
}
