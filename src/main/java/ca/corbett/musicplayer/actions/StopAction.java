package ca.corbett.musicplayer.actions;

import ca.corbett.extras.EnhancedAction;
import ca.corbett.musicplayer.ui.AudioPanel;

import java.awt.event.ActionEvent;

public class StopAction extends EnhancedAction {

    public StopAction() {
        super("Stop");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        AudioPanel.getInstance().stop();
    }
}
