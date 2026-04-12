package ca.corbett.musicplayer.actions;

import ca.corbett.extras.EnhancedAction;
import ca.corbett.musicplayer.ui.Playlist;

import java.awt.event.ActionEvent;

public class ShuffleAction extends EnhancedAction {

    public ShuffleAction() {
        super("Toggle shuffle");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Playlist.getInstance().toggleShuffle();
    }
}
