package mediaserver.gui;

import mediaserver.http.QPar;
import mediaserver.util.MostlyOnce;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stringtemplate.v4.NoIndentWriter;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STErrorListener;
import org.stringtemplate.v4.misc.ErrorType;
import org.stringtemplate.v4.misc.STMessage;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Supplier;

public final class Template {

    private static final Logger log = LoggerFactory.getLogger(Template.class);

    private final String name;

    private ST st;

    private Supplier<byte[]> bytes;

    public Template(String name, String source) {

        this.name = name;
        this.st = new ST(source, '{', '}');
        this.bytes = MostlyOnce.get(() -> {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (Writer out = new OutputStreamWriter(bytes, StandardCharsets.UTF_8)) {
                st.write(new NoIndentWriter(out), new LoggingErrorListener());
            } catch (IOException e) {
                throw new IllegalStateException(this + " failed to write template: " + st, e);
            }
            return bytes.toByteArray();
        });
    }

    public byte[] bytes() {

        return this.bytes.get();
    }

    public Template add(QPar param, Object value) {

        return add(param.getName(), value);
    }

    public Template add(String name, Object value) {

        if (value != null) {
            st.add(name, value);
        }
        return this;
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

        return getClass().getSimpleName() + "[" + name + "]";
    }
}
