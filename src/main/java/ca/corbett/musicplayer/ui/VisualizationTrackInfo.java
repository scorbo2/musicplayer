package ca.corbett.musicplayer.ui;

import java.io.File;

/**
 * Simple container class to hold information about the currently playing track.
 *
 * @author scorbo2
 * @since 2017-12-06
 */
public class VisualizationTrackInfo {

    protected File sourceFile;
    protected String title;
    protected String artist;
    protected String album;
    protected int currentTimeSeconds;
    protected int totalTimeSeconds;

    public void reset() {
        sourceFile = null;
        title = null;
        artist = null;
        album = null;
        currentTimeSeconds = 0;
        totalTimeSeconds = 0;
    }

    public File getSourceFile() {
        return sourceFile;
    }

    public void setSourceFile(File sourceFile) {
        this.sourceFile = sourceFile;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public int getCurrentTimeSeconds() {
        return currentTimeSeconds;
    }

    public void setCurrentTimeSeconds(int currentTimeSeconds) {
        this.currentTimeSeconds = currentTimeSeconds;
    }

    public int getTotalTimeSeconds() {
        return totalTimeSeconds;
    }

    public void setTotalTimeSeconds(int totalTimeSeconds) {
        this.totalTimeSeconds = totalTimeSeconds;
    }
}
