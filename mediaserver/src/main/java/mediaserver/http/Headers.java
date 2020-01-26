package mediaserver.http;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface Headers extends Consumer<BiConsumer<CharSequence, CharSequence>> {

}
