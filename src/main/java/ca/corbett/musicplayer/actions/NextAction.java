package ca.corbett.musicplayer.actions;

import ca.corbett.extras.EnhancedAction;
import ca.corbett.musicplayer.ui.AudioPanel;

import java.awt.event.ActionEvent;

public class NextAction extends EnhancedAction {

    public NextAction() {
        super("Next track");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        AudioPanel.getInstance().next();
    }
}
