package taninim.fb;

import module java.base;
import com.restfb.FacebookClient;
import com.restfb.JsonMapper;

record SimpleMapper() implements JsonMapper {

    @SuppressWarnings("unchecked")
    @Override
    public <T> T toJavaObject(String json, Class<T> type) {
        return (T) ExtUserRW.INSTANCE.stringReader().read(json);
    }

    @Override
    public <T> List<T> toJavaList(String json, Class<T> type) {
        throw new UnsupportedOperationException("Not supported: To java list: " + type);
    }

    @Override
    public String toJson(Object object) {
        throw new UnsupportedOperationException("Not supported: To json " + object);
    }

    @Override
    public String toJson(Object object, boolean ignoreNullValuedProperties) {
        throw new UnsupportedOperationException("Not supported: To json " + object);
    }

    @Override
    public void setFacebookClient(FacebookClient facebookClient) {
    }
}
