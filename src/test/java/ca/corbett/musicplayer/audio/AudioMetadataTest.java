package ca.corbett.musicplayer.audio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AudioMetadataTest {
    @Test
    public void getDurationFormatted_withVeryShortValue_shouldSucceed() {
        // GIVEN a single digit value in seconds:
        AudioMetadata meta = AudioMetadata.fromRawValues("", "", "", "", 4);

        // WHEN we format it:
        String formatted = meta.getDurationFormatted();

        // THEN we should see it look nice:
        assertEquals("00:04", formatted);
    }

    @Test
    public void getDurationFormatted_withShortValue_shouldSucceed() {
        // GIVEN a value in seconds less than one minute:
        AudioMetadata meta = AudioMetadata.fromRawValues("", "", "", "", 45);

        // WHEN we format it:
        String formatted = meta.getDurationFormatted();

        // THEN we should see it look nice:
        assertEquals("00:45", formatted);
    }

    @Test
    public void getDurationFormatted_withMediumValue_shouldSucceed() {
        // GIVEN a value in seconds less than one hour:
        AudioMetadata meta = AudioMetadata.fromRawValues("", "", "", "", 3599);

        // WHEN we format it:
        String formatted = meta.getDurationFormatted();

        // THEN we should see it look nice:
        assertEquals("59:59", formatted);
    }

    @Test
    public void getDurationFormatted_withHugeValue_shouldSucceed() {
        // GIVEN an unreasonably large value in seconds:
        AudioMetadata meta = AudioMetadata.fromRawValues("", "", "", "", 9999);

        // WHEN we format it:
        String formatted = meta.getDurationFormatted();

        // THEN we should see it look nice:
        assertEquals("2:46:39", formatted);
    }

}