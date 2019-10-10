package mediaserver;


import mediaserver.util.URLs;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

public class IOTest {

    @Test
    public void pars() {
        Map<String, String> params = URLs.queryParams("foo=bar&zop=zit");
        assertEquals(2, params.size());
        assertEquals("bar", params.get("foo"));
        assertEquals("zit", params.get("zop"));
    }
}
