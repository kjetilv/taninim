package mediaserver.externals;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static org.xmlpull.v1.XmlPullParser.*;

/**
 * This class parses an iOS plist with a dict element into a hashmap.
 */
public final class IOSMapParser {

    private static final String KEY = "key";

    private static final String STRING = "string";

    private static final String INTEGER = "integer";

    private static final String DICT = "dict";

    private static final String ARRAY = "array";

    private IOSMapParser() {

    }

    public static Map<String, ?> convert(InputStream inputStream) {

        XmlPullParser parser = parser(inputStream);
        LinkedList<Map<String, Object>> maps = new LinkedList<>();
        boolean skip = false;

        try {
            String tag = null;
            String key = null;

            while (true) {

                int node = parser.next();

                if (skip) {
                    if (node == END_TAG && is(ARRAY, parser.getName())) {
                        skip = false;
                    } else {
                        continue;
                    }
                }

                if (node == END_DOCUMENT) {
                    return maps.getFirst();
                }

                if (node == START_TAG) {
                    String name = parser.getName();
                    if (Stream.of(STRING, INTEGER).anyMatch(name::equalsIgnoreCase)) {
                        tag = name;
                        continue;
                    }

                    if (is(KEY, name)) {
                        tag = name;
                        key = null;
                        continue;
                    }

                    if (is(DICT, name)) {
                        Map<String, Object> value = new LinkedHashMap<>();
                        if (!maps.isEmpty()) {
                            addValue(maps, key, value);
                        }
                        maps.add(value);
                        tag = null;
                        key = null;
                        continue;
                    }

                    if (is(ARRAY, name)) {
                        skip = true;
                    }
                }

                if (node == END_TAG) {
                    if (is(DICT, parser.getName())) {
                        Map<String, Object> map = maps.removeLast();
                        if (maps.isEmpty()) {
                            return map;
                        }
                    }
                    continue;
                }

                if (node == TEXT) {
                    String text = Objects.requireNonNull(parser.getText(), "text");
                    if (is(KEY, tag)) {
                        key = text;
                        tag = null;
                        continue;
                    }
                    if (is(STRING, tag)) {
                        addValue(maps, key, text);
                        tag = null;
                        continue;
                    }
                    if (is(INTEGER, tag)) {
                        addValue(maps, key, Long.parseLong(text));
                        key = null;
                        tag = null;
                    }
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse", e);
        }
    }

    private static boolean is(String string, String tag) {

        return string.equalsIgnoreCase(tag);
    }

    private static void addValue(LinkedList<Map<String, Object>> mapPath, String key, Object value) {

        mapPath.getLast().put(Objects.requireNonNull(key, "key => " + value), value);
    }

    private static XmlPullParser parser(InputStream inputStream) {

        try {
            XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
            parser.setInput(inputStream, StandardCharsets.UTF_8.name());
            return parser;
        } catch (XmlPullParserException e) {
            throw new IllegalStateException("Could not set up parser", e);
        }
    }
}
