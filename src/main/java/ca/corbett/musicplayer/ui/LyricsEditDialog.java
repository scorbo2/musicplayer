package ca.corbett.musicplayer.ui;

import ca.corbett.musicplayer.audio.AudioMetadata;

import javax.swing.JDialog;

/**
 * An abstract base class for viewing/editing lyrics.
 * The application provides a very basic built-in lyrics editor.
 * The intention here is that extensions can supply their own lyrics
 * editing dialog, which will be used instead, if the extension is enabled.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since MusicPlayer 3.3
 */
public abstract class LyricsEditDialog extends JDialog {

    protected final AudioMetadata track;

    public LyricsEditDialog(AudioMetadata track) {
        super(MainWindow.getInstance(), "Lyrics editor - " + track.getTitle(), true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.track = track;
    }
}
