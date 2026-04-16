package ca.corbett.musicplayer.ui;

import ca.corbett.musicplayer.audio.AudioMetadata;

import javax.swing.JDialog;

/**
 * An abstract base class for dialogs that can display information
 * about an audio track. Descendant classes must handle the details
 * of allowing viewing (and possibly editing) of track metadata.
 * This class is an extension point - extensions can supply
 * custom implementations to replace the built-in ones that
 * the application provides.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public abstract class TrackInfoDialog extends JDialog {

    protected static final String DEFAULT_DIALOG_TITLE = "Track info";
    protected final AudioMetadata metadata;

    public TrackInfoDialog(AudioMetadata metadata) {
        super(MainWindow.getInstance(), DEFAULT_DIALOG_TITLE, true);
        this.metadata = metadata;
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(MainWindow.getInstance());
    }
}
