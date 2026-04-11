package ca.corbett.musicplayer.actions;

import ca.corbett.extras.EnhancedAction;
import ca.corbett.musicplayer.ui.AudioPanel;

import java.awt.event.ActionEvent;

/**
 * This action is safe to invoke no matter what the current audio state is.
 * If audio is currently playing, this action will pause it.
 * If audio was already paused, this action will resume it.
 * If no audio was playing, this action will start playing the current track (if something is loaded).
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class PauseAction extends EnhancedAction {

    public PauseAction() {
        super("Pause");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        switch (AudioPanel.getInstance().getPanelState()) {
            case PLAYING:
            case PAUSED:
                AudioPanel.getInstance().pause();
                break;

            case IDLE:
                AudioPanel.getInstance().play();
                break;
        }
    }
}
