package mediaserver.util;

import mediaserver.http.QPar;
import org.junit.Test;

import java.util.Collection;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class URLsTest {

    @Test
    public void testPars() {

        Map<QPar, Collection<String>> params = URLs.queryParams("artist=foo&album=bar&zip=zot");
        assertThat(params.size(), is(2));
        assertThat(params.get(QPar.artist).iterator().next(), is("foo"));
        assertThat(params.get(QPar.album).iterator().next(), is("bar"));
    }
}
