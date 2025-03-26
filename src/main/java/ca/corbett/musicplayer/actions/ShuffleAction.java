package ca.corbett.musicplayer.actions;

import ca.corbett.musicplayer.ui.Playlist;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;

public class ShuffleAction extends AbstractAction {
    @Override
    public void actionPerformed(ActionEvent e) {
        Playlist.getInstance().toggleShuffle();
    }
}
