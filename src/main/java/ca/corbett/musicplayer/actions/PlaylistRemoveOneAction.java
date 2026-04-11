package ca.corbett.musicplayer.actions;

import ca.corbett.extras.EnhancedAction;
import ca.corbett.musicplayer.ui.Playlist;

import java.awt.event.ActionEvent;

public class PlaylistRemoveOneAction extends EnhancedAction {

    public PlaylistRemoveOneAction() {
        super("Remove selected track");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Playlist.getInstance().removeSelected();
    }
}
