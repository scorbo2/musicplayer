package ca.corbett.musicplayer.actions;

import ca.corbett.extras.EnhancedAction;
import ca.corbett.musicplayer.ui.AudioPanel;

import java.awt.event.ActionEvent;

public class PrevAction extends EnhancedAction {

    public PrevAction() {
        super("Previous track");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        AudioPanel.getInstance().prev();
    }
}
