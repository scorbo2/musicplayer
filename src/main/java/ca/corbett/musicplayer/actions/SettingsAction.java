package ca.corbett.musicplayer.actions;

import ca.corbett.extras.EnhancedAction;
import ca.corbett.musicplayer.AppConfig;
import ca.corbett.musicplayer.ui.MainWindow;

import java.awt.event.ActionEvent;

public class SettingsAction extends EnhancedAction {

    public SettingsAction() {
        super("Settings");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (AppConfig.getInstance().showPropertiesDialog(MainWindow.getInstance())) {
            // User OK'd the dialog, reload the UI:
            ReloadUIAction.getInstance().actionPerformed(null);
        }
    }
}
