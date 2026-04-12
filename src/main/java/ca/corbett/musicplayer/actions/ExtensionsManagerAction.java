package ca.corbett.musicplayer.actions;

import ca.corbett.extras.EnhancedAction;
import ca.corbett.musicplayer.AppConfig;
import ca.corbett.musicplayer.ui.MainWindow;

import java.awt.event.ActionEvent;

public class ExtensionsManagerAction extends EnhancedAction {

    public ExtensionsManagerAction() {
        super("Extensions manager");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (AppConfig.getInstance().showExtensionDialog(MainWindow.getInstance(),
                                                        MainWindow.getInstance().getUpdateManager())) {
            // User OK'd the dialog, reinitialize AppConfig:
            // (this will force a properties reload and then a UI reload):
            AppConfig.getInstance().reinitialize();
        }
    }
}
