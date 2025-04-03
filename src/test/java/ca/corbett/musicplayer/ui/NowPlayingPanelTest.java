package ca.corbett.musicplayer.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NowPlayingPanelTest {

    @Test
    public void formatSeconds_withVeryShortValue_shouldSucceed() {
        // GIVEN a single digit value in seconds:
        int seconds = 4;

        // WHEN we format it:
        String formatted = NowPlayingPanel.formatSeconds(seconds);

        // THEN we should see it look nice:
        assertEquals("00:04", formatted);
    }

    @Test
    public void formatSeconds_withShortValue_shouldSucceed() {
        // GIVEN a value in seconds less than one minute:
        int seconds = 45;

        // WHEN we format it:
        String formatted = NowPlayingPanel.formatSeconds(seconds);

        // THEN we should see it look nice:
        assertEquals("00:45", formatted);
    }

    @Test
    public void formatSeconds_withMediumValue_shouldSucceed() {
        // GIVEN a value in seconds less than one hour:
        int seconds = 3599;

        // WHEN we format it:
        String formatted = NowPlayingPanel.formatSeconds(seconds);

        // THEN we should see it look nice:
        assertEquals("59:59", formatted);
    }

    @Test
    public void formatSeconds_withHugeValue_shouldSucceed() {
        // GIVEN an unreasonably large value in seconds:
        int seconds = 99999;

        // WHEN we format it:
        String formatted = NowPlayingPanel.formatSeconds(seconds);

        // THEN we should see it look nice:
        assertEquals("27:46:39", formatted);
    }

}