package ca.corbett.musicplayer.actions;

import ca.corbett.musicplayer.ui.MainWindow;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;

public class PlaylistAction extends AbstractAction {
    @Override
    public void actionPerformed(ActionEvent e) {
        MainWindow.getInstance().togglePlaylistVisible(true);
    }
}
