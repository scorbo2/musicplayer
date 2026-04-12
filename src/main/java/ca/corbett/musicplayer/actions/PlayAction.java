package ca.corbett.musicplayer.actions;

import ca.corbett.extras.EnhancedAction;
import ca.corbett.musicplayer.ui.AudioPanel;

import java.awt.event.ActionEvent;

public class PlayAction extends EnhancedAction {

    public PlayAction() {
        super("Play");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        AudioPanel.getInstance().play();
    }
}
