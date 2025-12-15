package ca.corbett.musicplayer.audio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AudioMetadataTest {
    @Test
    public void getDurationFormatted_withVeryShortValue_shouldSucceed() {
        // GIVEN a single digit value in seconds:
        AudioMetadata meta = AudioMetadata.fromRawValues("", "", "", "", null, 4);

        // WHEN we format it:
        String formatted = meta.getDurationFormatted();

        // THEN we should see it look nice:
        assertEquals("00:04", formatted);
    }

    @Test
    public void getDurationFormatted_withShortValue_shouldSucceed() {
        // GIVEN a value in seconds less than one minute:
        AudioMetadata meta = AudioMetadata.fromRawValues("", "", "", "", null, 45);

        // WHEN we format it:
        String formatted = meta.getDurationFormatted();

        // THEN we should see it look nice:
        assertEquals("00:45", formatted);
    }

    @Test
    public void getDurationFormatted_withMediumValue_shouldSucceed() {
        // GIVEN a value in seconds less than one hour:
        AudioMetadata meta = AudioMetadata.fromRawValues("", "", "", "", null, 3599);

        // WHEN we format it:
        String formatted = meta.getDurationFormatted();

        // THEN we should see it look nice:
        assertEquals("59:59", formatted);

        // Also make sure invoking getDurationFormatted() multiple times returns the same value:
        assertEquals("59:59", meta.getDurationFormatted());
    }

    @Test
    public void getDurationFormatted_withHugeValue_shouldSucceed() {
        // GIVEN an unreasonably large value in seconds:
        AudioMetadata meta = AudioMetadata.fromRawValues("", "", "", "", null, 9999);

        // WHEN we format it:
        String formatted = meta.getDurationFormatted();

        // THEN we should see it look nice:
        assertEquals("2:46:39", formatted);
    }

    @Test
    public void getDurationFormatted_withNegativeValue_shouldTreatAsPositive() {
        // GIVEN an obviously wrong negative duration value:
        AudioMetadata meta = AudioMetadata.fromRawValues("", "", "", "", null, -85);

        // WHEN we format it:
        String formatted = meta.getDurationFormatted();

        // THEN we should see it flipped to a positive value:
        assertEquals("01:25", formatted);
    }

    @Test
    public void getFormatted_withTypicalValues_shouldSucceed() {
        // GIVEN typical metadata values:
        AudioMetadata meta = AudioMetadata.fromRawValues("Song Title",
                                                         "Album Name",
                                                         "Artist Name",
                                                         "Genre Name",
                                                         null,
                                                         245);

        // WHEN we format it:
        String formatted = meta.getFormatted("%a - %t [%b] (%g) {%D}");

        // THEN we should see the expected formatted string:
        String expected = "Artist Name - Song Title [Album Name] (Genre Name) {04:05}";
        assertEquals(expected, formatted);
    }

    @Test
    public void getFormatted_withMissingValues_shouldUseDefaults() {
        // GIVEN some missing metadata values:
        AudioMetadata meta = AudioMetadata.fromRawValues("",
                                                         "",
                                                         null,
                                                         "   ",
                                                         null,
                                                         0);

        // WHEN we format it:
        String formatted = meta.getFormatted("%a - %t [%b] (%g) {%D}");

        // THEN we should see the expected formatted string with defaults:
        String expected = "unknown - unknown [unknown] (unknown) {00:00}";
        assertEquals(expected, formatted);
    }

    @Test
    public void getFormatted_withInvalidFormatChars_shouldIgnoreThem() {
        // GIVEN typical metadata values:
        AudioMetadata meta = AudioMetadata.fromRawValues("Title",
                                                         "Album",
                                                         "Artist",
                                                         "Genre",
                                                         null,
                                                         123);

        // WHEN we format it with some invalid format characters:
        String formatted = meta.getFormatted("%x %y %a %z %t %1 %D %%");

        // THEN we should see only the valid format characters replaced:
        String expected = "  Artist  Title  02:03 %";
        assertEquals(expected, formatted);
    }

    @Test
    public void getFormatted_withNullSourceFile_shouldReturnDefault() {
        // GIVEN typical metadata values but a null source file:
        AudioMetadata meta = AudioMetadata.fromRawValues("Title",
                                                         "Album",
                                                         "Artist",
                                                         "Genre",
                                                         null,
                                                         200);

        // WHEN we format it with %f:
        String formatted = meta.getFormatted("File: (%f) Path: (%F)");

        // THEN we should see the default placeholder for missing file:
        String expected = "File: (unknown) Path: (unknown)";
        assertEquals(expected, formatted);
    }

    @Test
    public void getFormatted_withSourceFile_shouldReturnFileNames() {
        // GIVEN typical metadata values and a valid source file:
        AudioMetadata meta = AudioMetadata.fromRawValues("Title",
                                                         "Album",
                                                         "Artist",
                                                         "Genre",
                                                         new java.io.File("/path/to/audiofile.mp3"),
                                                         200);

        // WHEN we format it with %f and %F:
        String formatted = meta.getFormatted("File: (%f) Path: (%F)");

        // THEN we should see the actual file name and path:
        String expected = "File: (audiofile.mp3) Path: (/path/to/audiofile.mp3)";
        assertEquals(expected, formatted);
    }
}