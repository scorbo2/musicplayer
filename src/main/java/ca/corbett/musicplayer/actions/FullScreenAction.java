package ca.corbett.musicplayer.actions;

import ca.corbett.musicplayer.ui.VisualizationWindow;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;

public class FullScreenAction extends AbstractAction {
    @Override
    public void actionPerformed(ActionEvent e) {
        VisualizationWindow.getInstance().goFullScreen();
    }
}
