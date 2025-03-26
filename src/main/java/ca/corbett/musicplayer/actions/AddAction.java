package ca.corbett.musicplayer.actions;

import ca.corbett.extras.io.FileSystemUtil;
import ca.corbett.musicplayer.ui.MainWindow;
import ca.corbett.musicplayer.ui.Playlist;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Shows a file chooser dialog to add items to the playlist, either
 * as individual files, or entire directories.
 *
 * @author scorbo2
 * @since 2025-03-23
 */
public class AddAction extends AbstractAction {

    private final JFileChooser fileChooser;

    public AddAction() {
        fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() ||
                        f.getName().toLowerCase().endsWith(".wav") ||
                        f.getName().toLowerCase().endsWith(".mp3");
            }

            @Override
            public String getDescription() {
                return "Media files (*.wav, *.mp3)";
            }
        });
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (fileChooser.showDialog(MainWindow.getInstance(), "Open") == JFileChooser.APPROVE_OPTION) {
            for (File file : fileChooser.getSelectedFiles()) {
                if (file.isDirectory()) {
                    List<String> extList = new ArrayList<>();
                    extList.add("mp3");
                    extList.add("wav");
                    List<File> list = FileSystemUtil.findFiles(file, true, extList);
                    for (File f : list) {
                        Playlist.getInstance().addItem(f);
                    }
                } else {
                    Playlist.getInstance().addItem(file);
                }
            }
        }
    }
}
