package ca.corbett.musicplayer.actions;

import ca.corbett.musicplayer.AppConfig;
import ca.corbett.musicplayer.ui.MainWindow;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;

public class SettingsAction extends AbstractAction {
    @Override
    public void actionPerformed(ActionEvent e) {
        AppConfig.getInstance().showPropertiesDialog(MainWindow.getInstance());
    }
}
