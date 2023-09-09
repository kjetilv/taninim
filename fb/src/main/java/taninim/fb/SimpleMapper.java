package taninim.fb;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import com.restfb.FacebookClient;
import com.restfb.JsonMapper;

record SimpleMapper(Function<? super String, ? extends Map<?, ?>> reader) implements JsonMapper {

    @SuppressWarnings("unchecked")
    @Override
    public <T> T toJavaObject(String json, Class<T> type) {
        try {
            Map<String, Object> map = (Map<String, Object>) reader.apply(json);
            return (T) new ExtUser(get(map, "name"), get(map, "id"));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read response: " + json, e);
        }
    }

    @Override
    public <T> List<T> toJavaList(String json, Class<T> type) {
        return fail();
    }

    @Override
    public String toJson(Object object) {
        return fail();
    }

    @Override
    public String toJson(Object object, boolean ignoreNullValuedProperties) {
        return fail();
    }

    @Override
    public void setFacebookClient(FacebookClient facebookClient) {
    }

    private static <T> T fail() {
        throw new UnsupportedOperationException("Not supported");
    }

    private static String get(Map<String, Object> map, String key) {
        return Optional.ofNullable(map.get(key))
            .map(Object::toString)
            .orElseThrow(() -> new IllegalArgumentException("No " + key + " in " + map));
    }
}
