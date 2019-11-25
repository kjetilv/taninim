package mediaserver.gui;

import mediaserver.util.IO;
import mediaserver.util.MostlyOnce;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stringtemplate.v4.NoIndentWriter;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STErrorListener;
import org.stringtemplate.v4.misc.ErrorType;
import org.stringtemplate.v4.misc.STMessage;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Supplier;

public class Template {

    private static final Logger log = LoggerFactory.getLogger(Template.class);

    private ST st;

    private Supplier<String> result;

    private Supplier<byte[]> bytes;

    private final String resource;

    public Template(IO io, String resource) {

        this.resource = resource;
        this.st = st(io, resource);
        this.result = MostlyOnce.get(() ->
        {
            StringWriter out = new StringWriter();
            st.write(new NoIndentWriter(out), new LoggingErrorListener());
            return out.getBuffer().toString();
        });
        this.bytes = MostlyOnce.get(() ->
            result.get().getBytes(StandardCharsets.UTF_8));
    }

    public byte[] bytes() {

        return this.bytes.get();
    }

    Template add(QPar param, Object value) {

        return add(param.getName(), value);
    }

    Template add(String name, Object value) {

        if (value != null) {
            st.add(name, value);
        }
        return this;
    }

    private static ST st(IO io, String resource) {

        return io.read(resource).map(data ->
            new ST(data, '{', '}')
        ).orElseThrow(() ->
            new IllegalArgumentException("Invalid template: " + resource));
    }

    private static class LoggingErrorListener implements STErrorListener {

        private static Collection<Object> MISSING_PROPS = new ConcurrentSkipListSet<>();

        @Override
        public void compileTimeError(STMessage msg) {

            log.warn("Failed to compile: {}", msg, msg.cause);
        }

        @Override
        public void runTimeError(STMessage msg) {
            if (msg.error == ErrorType.NO_SUCH_ATTRIBUTE) {
                if (MISSING_PROPS.add(msg.arg)) {
                    log.debug("Missing property: {}", msg, msg.cause);
                }
            } else {
                log.warn("Failed at runtime: {}", msg, msg.cause);
            }
        }

        @Override
        public void IOError(STMessage msg) {

            log.warn("Failed at I/O: {}", msg, msg.cause);
        }

        @Override
        public void internalError(STMessage msg) {

            log.error("Internal error: {}", msg, msg.cause);
        }
    }

    @Override
    public String toString() {

        return getClass().getSimpleName() + "[" + resource + "]";
    }
}
