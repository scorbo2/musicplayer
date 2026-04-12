package ca.corbett.musicplayer.actions;

import ca.corbett.extras.EnhancedAction;
import ca.corbett.musicplayer.ui.MainWindow;

import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;

/**
 * Invoke this action to perform an orderly exit of the application.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class ExitAction extends EnhancedAction {

    public ExitAction() {
        super("Exit");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Tell MainWindow to close itself, which will trigger the shutdown sequence:
        MainWindow.getInstance().dispatchEvent(new WindowEvent(MainWindow.getInstance(), WindowEvent.WINDOW_CLOSING));
    }
}
