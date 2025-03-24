package ca.corbett.musicplayer.actions;

import ca.corbett.musicplayer.ui.AudioPanel;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;

public class StopAction extends AbstractAction {
    @Override
    public void actionPerformed(ActionEvent e) {
        AudioPanel.getInstance().stop();
    }
}
