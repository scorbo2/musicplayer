package ca.corbett.musicplayer.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TrackInfoDialogTest {

    @Test
    public void testGetSizeString() {
        assertEquals("1.15 GB", TrackInfoDialog.getSizeString(1234567890));
        assertEquals("117.74 MB", TrackInfoDialog.getSizeString(123456789));
        assertEquals("11.77 MB", TrackInfoDialog.getSizeString(12345678));
        assertEquals("1.18 MB", TrackInfoDialog.getSizeString(1234567));
        assertEquals("120.56 KB", TrackInfoDialog.getSizeString(123456));
        assertEquals("12.06 KB", TrackInfoDialog.getSizeString(12345));
        assertEquals("1.21 KB", TrackInfoDialog.getSizeString(1234));
        assertEquals("123 bytes", TrackInfoDialog.getSizeString(123));
    }

}