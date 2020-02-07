package mediaserver.templates;

import mediaserver.util.MostlyOnce;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stringtemplate.v4.NoIndentWriter;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STErrorListener;
import org.stringtemplate.v4.misc.ErrorType;
import org.stringtemplate.v4.misc.STMessage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

public final class Template {

    private static final Logger log = LoggerFactory.getLogger(Template.class);

    private final String name;

    private final ST st;

    private final Supplier<byte[]> bytes;

    public Template(String name, String source) {

        this.name = name;
        this.st = new ST(source, '{', '}');
        this.bytes = MostlyOnce.get(this::toBytes);
    }

    public byte[] bytes() {

        return this.bytes.get();
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public Template add(TPar param, Optional<?> value) {

        return value.map(val -> add(param, val)).orElse(this);
    }

    public Template add(TPar param, Object value) {

        if (value != null) {
            st.add(Objects.requireNonNull(param, "param").getName(), value);
        }
        return this;
    }

    private byte[] toBytes() {

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (Writer out = new OutputStreamWriter(bytes, StandardCharsets.UTF_8)) {
            st.write(new NoIndentWriter(out), new LoggingErrorListener(this.name));
        } catch (IOException e) {
            throw new IllegalStateException(this + " failed to write template: " + st, e);
        }
        return bytes.toByteArray();
    }

    private static class LoggingErrorListener implements STErrorListener {

        private final String name;

        public LoggingErrorListener(String name) {

            this.name = name;
        }

        @Override
        public void compileTimeError(STMessage msg) {

            log.warn("{}: Failed to compile: {}", name, msg, msg.cause);
        }

        @Override
        public void runTimeError(STMessage msg) {

            if (msg.error != ErrorType.NO_SUCH_ATTRIBUTE) {
                log.warn("{}: Failed at runtime: {}", name, msg, msg.cause);
            }
        }

        @Override
        public void IOError(STMessage msg) {

            log.warn("{}: Failed at I/O: {}", name, msg, msg.cause);
        }

        @Override
        public void internalError(STMessage msg) {

            log.error("{}: Internal error: {}", name, msg, msg.cause);
        }
    }

    @Override
    public String toString() {

        return getClass().getSimpleName() + "[" + name + "]";
    }
}
