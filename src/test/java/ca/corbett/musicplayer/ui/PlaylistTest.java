package ca.corbett.musicplayer.ui;

import ca.corbett.musicplayer.audio.AudioMetadata;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PlaylistTest {

    private AudioMetadata makeMeta(String title, String album, String author, String genre, String path, int duration) throws Exception {
        return AudioMetadata.fromRawValues(title, album, author, genre,
                (path == null) ? null : new File(path), duration);
    }

    @Test
    public void sortList_multiKey_artistThenTitle_shouldSortCorrectly() throws Exception {
        AudioMetadata a1 = makeMeta("Song A", "Album1", "Alpha", "", "/tmp/a1.mp3", 10);
        AudioMetadata a2 = makeMeta("Song B", "Album1", "Alpha", "", "/tmp/a2.mp3", 10);
        AudioMetadata b1 = makeMeta("Song C", "Album2", "Beta", "", "/tmp/b1.mp3", 10);

        List<AudioMetadata> list = new ArrayList<>(Arrays.asList(b1, a2, a1));

        List<Playlist.SortKey> keys = Arrays.asList(
                Playlist.SortKey.asc(Playlist.SortAttribute.Artist),
                Playlist.SortKey.asc(Playlist.SortAttribute.Title)
        );

        Playlist.getInstance().sortList(list, keys);

        assertEquals("Alpha", list.get(0).getAuthor());
        assertEquals("Song A", list.get(0).getTitle());
        assertEquals("Alpha", list.get(1).getAuthor());
        assertEquals("Song B", list.get(1).getTitle());
        assertEquals("Beta", list.get(2).getAuthor());
    }

    @Test
    public void sortList_descendingArtist_shouldSortReverse() throws Exception {
        AudioMetadata a = makeMeta("One", "Album", "A", "", "/tmp/one.mp3", 5);
        AudioMetadata b = makeMeta("Two", "Album", "B", "", "/tmp/two.mp3", 5);
        AudioMetadata c = makeMeta("Three", "Album", "C", "", "/tmp/three.mp3", 5);

        List<AudioMetadata> list = new ArrayList<>(Arrays.asList(a, b, c));

        List<Playlist.SortKey> keys = List.of(
                Playlist.SortKey.desc(Playlist.SortAttribute.Artist)
        );

        Playlist.getInstance().sortList(list, keys);

        assertEquals("C", list.get(0).getAuthor());
        assertEquals("B", list.get(1).getAuthor());
        assertEquals("A", list.get(2).getAuthor());
    }

    @Test
    public void sortList_nullOrEmptyInputs_shouldNotThrowAndLeaveListUnchanged() throws Exception {
        AudioMetadata a = makeMeta("One", "Album", "A", "", "/tmp/one.mp3", 5);
        AudioMetadata b = makeMeta("Two", "Album", "B", "", "/tmp/two.mp3", 5);

        List<AudioMetadata> original = new ArrayList<>(Arrays.asList(a, b));


        // null toSort
        assertDoesNotThrow(() -> Playlist.getInstance().sortList(null, List.of(Playlist.SortKey.asc(Playlist.SortAttribute.Title))));

        // null sortKeys
        List<AudioMetadata> copy1 = new ArrayList<>(original);
        assertDoesNotThrow(() -> Playlist.getInstance().sortList(copy1, null));
        assertEquals(original, copy1);

        // empty sortKeys
        List<AudioMetadata> copy2 = new ArrayList<>(original);
        assertDoesNotThrow(() -> Playlist.getInstance().sortList(copy2, new ArrayList<>()));
        assertEquals(original, copy2);

        // empty toSort
        List<AudioMetadata> empty = new ArrayList<>();
        assertDoesNotThrow(() -> Playlist.getInstance().sortList(empty, List.of(Playlist.SortKey.asc(Playlist.SortAttribute.Title))));
        assertEquals(0, empty.size());
    }
}