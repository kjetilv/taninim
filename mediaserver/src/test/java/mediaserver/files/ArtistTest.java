package mediaserver.files;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertTrue;

public class ArtistTest {

    @Test
    public void split_artist_and() {

        assertTrue(Artist.get("John & Yoko").getCompositeArtists().containsAll(
            Arrays.asList(Artist.get("John"), Artist.get("Yoko"))));
    }

    @Test
    public void split_artist_slash() {

        assertTrue(Artist.get("John / Yoko").getCompositeArtists().containsAll(
            Arrays.asList(Artist.get("John"), Artist.get("Yoko"))));
    }

    @Test
    public void split_artist_comma() {

        assertTrue(Artist.get("John, Yoko").getCompositeArtists().containsAll(
            Arrays.asList(Artist.get("John"), Artist.get("Yoko"))));
    }
}
