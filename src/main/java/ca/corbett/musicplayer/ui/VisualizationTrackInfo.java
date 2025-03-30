package ca.corbett.musicplayer.ui;

/**
 * Simple container class to hold information about the currently playing track.
 *
 * @author scorbo2
 * @since 2017-12-06
 */
public final class VisualizationTrackInfo {

    public String title;
    public String artist;
    public String album;
    public int currentTime; // in seconds
    public int totalTime; // in seconds

    public void reset() {
        title = null;
        artist = null;
        album = null;
        currentTime = 0;
        totalTime = 0;
    }
}
