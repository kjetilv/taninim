package taninim.fb;

import com.github.kjetilv.uplift.json.events.JsonReader;
import com.restfb.FacebookClient;
import com.restfb.JsonMapper;

import java.util.List;

record SimpleMapper() implements JsonMapper {

    @SuppressWarnings("unchecked")
    @Override
    public <T> T toJavaObject(String json, Class<T> type) {
        return (T) USER_READER.read(json);
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

    private static final JsonReader<String, ExtUser> USER_READER = ExtUserRW.INSTANCE.stringReader();

    private static <T> T fail() {
        throw new UnsupportedOperationException("Not supported");
    }
}
