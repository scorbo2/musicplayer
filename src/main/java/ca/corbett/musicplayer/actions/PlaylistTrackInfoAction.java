package ca.corbett.musicplayer.actions;

import ca.corbett.musicplayer.extensions.MusicPlayerExtensionManager;
import ca.corbett.musicplayer.ui.MainWindow;
import ca.corbett.musicplayer.ui.Playlist;
import ca.corbett.musicplayer.ui.TrackInfoDialog;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import java.awt.event.ActionEvent;
import java.io.File;

public class PlaylistTrackInfoAction extends AbstractAction {
    @Override
    public void actionPerformed(ActionEvent e) {
        File selectedTrack = Playlist.getInstance().getSelectedTrackFile();
        if (selectedTrack == null) {
            JOptionPane.showMessageDialog(MainWindow.getInstance(), "Nothing selected.");
            return;
        }

        // See if any registered extension can do this for us:
        TrackInfoDialog dialog = MusicPlayerExtensionManager.getInstance().getTrackInfoDialog(selectedTrack);

        // If not, we'll do it ourselves:
        if (dialog == null) {
            dialog = new TrackInfoDialog(selectedTrack);
        }
        
        dialog.setVisible(true);
    }
}
