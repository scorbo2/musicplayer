package ca.corbett.musicplayer.actions;

import ca.corbett.musicplayer.AppConfig;
import ca.corbett.musicplayer.ui.MainWindow;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;

public class ExtensionsManagerAction extends AbstractAction {
    @Override
    public void actionPerformed(ActionEvent e) {
        if (AppConfig.getInstance().showExtensionDialog(MainWindow.getInstance())) {
            // User OK'd the dialog, reload the UI:
            AppConfig.getInstance().reinitialize();
            ReloadUIAction.getInstance().actionPerformed(null);
        }
    }
}
