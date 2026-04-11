package ca.corbett.musicplayer.actions;

import ca.corbett.extras.EnhancedAction;
import ca.corbett.musicplayer.ui.Playlist;

import java.awt.event.ActionEvent;

public class RepeatAction extends EnhancedAction {

    public RepeatAction() {
        super("Toggle repeat");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Playlist.getInstance().toggleRepeat();
    }
}
