package ca.corbett.musicplayer.actions;

import ca.corbett.extras.EnhancedAction;
import ca.corbett.musicplayer.ui.Playlist;

import java.awt.event.ActionEvent;

public class PlaylistRemoveAllAction extends EnhancedAction {

    public PlaylistRemoveAllAction() {
        super("Clear playlist");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Playlist.getInstance().clear();
    }
}
