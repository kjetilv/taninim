package mediaserver.externals;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import static org.xmlpull.v1.XmlPullParser.END_DOCUMENT;
import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;
import static org.xmlpull.v1.XmlPullParser.TEXT;

/**
 * This class parses an iOS plist with a dict element into a hashmap.
 */
public final class IOSMapParser {

    private IOSMapParser() {

    }

    @SuppressWarnings({ "ContinueStatement", "AssignmentToNull" })
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

                    if (node == END_TAG && ARRAY.equalsIgnoreCase(parser.getName())) {
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

                    if (KEY.equalsIgnoreCase(name)) {
                        tag = name;
                        key = null;
                        continue;
                    }

                    if (DICT.equalsIgnoreCase(name)) {
                        Map<String, Object> value = new LinkedHashMap<>();
                        if (!maps.isEmpty()) {
                            addValue(maps, key, value);
                        }
                        maps.add(value);
                        tag = null;
                        key = null;
                        continue;
                    }

                    if (ARRAY.equalsIgnoreCase(name)) {
                        skip = true;
                    }
                }

                if (node == END_TAG) {

                    if (DICT.equalsIgnoreCase(parser.getName())) {
                        Map<String, Object> map = maps.removeLast();
                        if (maps.isEmpty()) {
                            return map;
                        }
                    }
                    continue;
                }

                if (node == TEXT) {
                    String text = Objects.requireNonNull(parser.getText(), "text");

                    if (KEY.equalsIgnoreCase(tag)) {
                        key = text;
                        tag = null;
                        continue;
                    }

                    if (STRING.equalsIgnoreCase(tag)) {
                        addValue(maps, key, text);
                        tag = null;
                        continue;
                    }

                    if (INTEGER.equalsIgnoreCase(tag)) {
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

    private static final String KEY = "key";

    private static final String STRING = "string";

    private static final String INTEGER = "integer";

    private static final String DICT = "dict";

    private static final String ARRAY = "array";

    private static void addValue(Deque<? extends Map<String, Object>> mapPath, String key, Object value) {

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
