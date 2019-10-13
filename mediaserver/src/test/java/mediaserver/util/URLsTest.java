package mediaserver.util;

import mediaserver.gui.QPar;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class URLsTest {

    @Test
    public void testPars() {
        Map<QPar, String> params = URLs.queryParams("artist=foo&album=bar&zip=zot");
        assertThat(params.size(), is(2));
        assertThat(params.get(QPar.ARTIST), is("foo"));
        assertThat(params.get(QPar.ALBUM), is("bar"));
    }
}
