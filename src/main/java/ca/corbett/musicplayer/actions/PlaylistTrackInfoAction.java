package ca.corbett.musicplayer.actions;

import ca.corbett.extras.EnhancedAction;
import ca.corbett.musicplayer.audio.AudioMetadata;
import ca.corbett.musicplayer.extensions.MusicPlayerExtensionManager;
import ca.corbett.musicplayer.ui.MainWindow;
import ca.corbett.musicplayer.ui.Playlist;
import ca.corbett.musicplayer.ui.TrackEditDialog;
import ca.corbett.musicplayer.ui.TrackInfoDialog;
import ca.corbett.musicplayer.ui.TrackInfoViewDialog;

import javax.swing.JOptionPane;
import java.awt.event.ActionEvent;

/**
 * An action that displays a TrackInfoViewDialog or a TrackEditDialog for the selected track,
 * depending on what is selected. If nothing is selected, a message dialog is shown instead.
 * If the selected track is a wav file, or other audio type that has no ID3 tag support,
 * then a read-only TrackInfoDialog is shown for that track.
 * If the selected track is an mp3 file, a TrackEditDialog is shown, which allows
 * editing of selected ID3 tags for that track.
 * <p>
 * Note that we query the extension manager before showing our built-in dialogs.
 * If any application extension contributes its own track info or edit dialog, that
 * will be shown instead of the default behavior described above.
 * </p>
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class PlaylistTrackInfoAction extends EnhancedAction {

    public PlaylistTrackInfoAction() {
        super("Track info");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        AudioMetadata selectedTrack = Playlist.getInstance().getSelectedTrackMetadata();
        if (selectedTrack == null) {
            JOptionPane.showMessageDialog(MainWindow.getInstance(), "Nothing selected.");
            return;
        }

        // See if any registered extension can do this for us:
        TrackInfoDialog dialog = MusicPlayerExtensionManager.getInstance().getTrackInfoDialog(selectedTrack);

        // If not, we'll do it ourselves:
        if (dialog == null) {
            dialog = selectedTrack.isMp3() ? new TrackEditDialog(selectedTrack) : new TrackInfoViewDialog(
                selectedTrack);
        }
        
        dialog.setVisible(true);
    }
}
