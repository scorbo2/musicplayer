package ca.corbett.musicplayer.actions;

import ca.corbett.musicplayer.AppConfig;
import ca.corbett.musicplayer.extensions.MusicPlayerExtensionManager;
import ca.corbett.musicplayer.ui.MainWindow;
import ca.corbett.musicplayer.ui.Playlist;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PlaylistOpenAction extends AbstractAction {
    private final JFileChooser fileChooser;

    public PlaylistOpenAction() {
        fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setMultiSelectionEnabled(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        fileChooser.resetChoosableFileFilters();
        for (FileNameExtensionFilter filter : MusicPlayerExtensionManager.getInstance().getPlaylistFileExtensionFilters()) {
            fileChooser.addChoosableFileFilter(filter);
        }
        fileChooser.setCurrentDirectory(AppConfig.getInstance().getLastBrowseDir());
        if (fileChooser.showOpenDialog(MainWindow.getInstance()) == JFileChooser.APPROVE_OPTION) {
            List<File> playlistFiles = new ArrayList<>();
            Collections.addAll(playlistFiles, fileChooser.getSelectedFiles());
            Playlist.getInstance().loadPlaylists(playlistFiles);
        }

        // Make a note of the directory that we ended up in,
        // so other file choosers across the app can
        // start there:
        AppConfig.getInstance().setLastBrowseDir(fileChooser.getCurrentDirectory());
        AppConfig.getInstance().save();
    }
}
