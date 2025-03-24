package ca.corbett.musicplayer.actions;

import ca.corbett.extras.about.AboutDialog;
import ca.corbett.musicplayer.Version;
import ca.corbett.musicplayer.ui.MainWindow;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;

public class AboutAction extends AbstractAction {
    @Override
    public void actionPerformed(ActionEvent e) {
        new AboutDialog(MainWindow.getInstance(), Version.getAboutInfo()).setVisible(true);
    }
}
