package mediaserver.gui;

import mediaserver.util.IO;
import mediaserver.util.MostlyOnce;
import org.stringtemplate.v4.ST;

import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

public class Template {

    private ST st;

    private Supplier<String> result;

    private Supplier<byte[]> bytes;

    private final Map<QPar, Object> values = new EnumMap<>(QPar.class);

    private final String resource;

    public Template(IO io, String resource) {
        this.resource = resource;
        st = st(io, resource);
        result = MostlyOnce.get(() ->
            st.render());
        bytes = MostlyOnce.get(() ->
            result.get().getBytes(StandardCharsets.UTF_8));
    }

    public byte[] bytes() {
        return this.bytes.get();
    }

    Template add(QPar param, Object value) {
        values.put(param, value);
        st.add(param.getName(), value);
        return this;
    }

    private static ST st(IO io, String resource) {
        return io.read(resource).map(data ->
            new ST(data, '{', '}')
        ).orElseThrow(() ->
            new IllegalArgumentException("Invalid template: " + resource));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + resource + " vals:" + values.keySet() + "]";
    }
}
