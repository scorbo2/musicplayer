package ca.corbett.musicplayer.actions;

import ca.corbett.extras.about.AboutDialog;
import ca.corbett.musicplayer.Version;
import ca.corbett.musicplayer.ui.MainWindow;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;

/**
 * Shows the About dialog to display information about the application.
 *
 * @author scorbo2
 * @since 2025-03-23
 */
public class AboutAction extends AbstractAction {
    @Override
    public void actionPerformed(ActionEvent e) {
        new AboutDialog(MainWindow.getInstance(), Version.getAboutInfo()).setVisible(true);
    }
}
