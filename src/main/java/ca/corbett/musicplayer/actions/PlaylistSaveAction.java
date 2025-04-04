package ca.corbett.musicplayer.actions;

import ca.corbett.musicplayer.AppConfig;
import ca.corbett.musicplayer.extensions.MusicPlayerExtensionManager;
import ca.corbett.musicplayer.ui.MainWindow;
import ca.corbett.musicplayer.ui.Playlist;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.event.ActionEvent;

/**
 * Shows a dialog for saving the current playlist to disk.
 * We'll consult the extension manager to see if any extensions have
 * registered additional playlist save formats beyond our built-in one.
 * If so, we'll delegate to the extension to actually save the
 * current playlist.
 *
 * @author scorbo2
 * @since 2025-03-27
 */
public class PlaylistSaveAction extends AbstractAction {
    private final JFileChooser fileChooser;

    public PlaylistSaveAction() {
        fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setMultiSelectionEnabled(false);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        fileChooser.resetChoosableFileFilters();
        for (FileNameExtensionFilter filter : MusicPlayerExtensionManager.getInstance().getPlaylistFileExtensionFilters()) {
            fileChooser.addChoosableFileFilter(filter);
        }
        fileChooser.setCurrentDirectory(AppConfig.getInstance().getLastBrowseDir());
        if (fileChooser.showSaveDialog(MainWindow.getInstance()) == JFileChooser.APPROVE_OPTION) {
            Playlist.getInstance().savePlaylist(fileChooser.getSelectedFile());
        }

        // Make a note of the directory that we ended up in,
        // so other file choosers across the app can
        // start there:
        AppConfig.getInstance().setLastBrowseDir(fileChooser.getCurrentDirectory());
        AppConfig.getInstance().save();
    }
}
