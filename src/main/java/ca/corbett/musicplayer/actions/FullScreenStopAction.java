package ca.corbett.musicplayer.actions;

import ca.corbett.extras.EnhancedAction;
import ca.corbett.musicplayer.ui.VisualizationWindow;

import java.awt.event.ActionEvent;

public class FullScreenStopAction extends EnhancedAction {

    public FullScreenStopAction() {
        super("Exit full screen");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        VisualizationWindow.getInstance().stopFullScreen();
    }
}
