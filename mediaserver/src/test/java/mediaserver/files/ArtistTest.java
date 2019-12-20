package mediaserver.files;

import mediaserver.media.Artist;
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

    @Test
    public void split_artist_and_stuff() {

        assertTrue(Artist.get("John, Yoko and Ringo").getCompositeArtists().containsAll(
            Arrays.asList(Artist.get("John"), Artist.get("Yoko"), Artist.get("Ringo"))));
    }

    @Test
    public void split_artist_oxford_comma() {

        assertTrue(Artist.get("John, Yoko, and Oxford Comma").getCompositeArtists().containsAll(
            Arrays.asList(Artist.get("John"), Artist.get("Yoko"), Artist.get("Oxford Comma"))));
    }
}
