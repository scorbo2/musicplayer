package ca.corbett.musicplayer.actions;

import ca.corbett.musicplayer.AppConfig;
import ca.corbett.musicplayer.ui.MainWindow;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;

/**
 * TODO we also need a way to launch the extension manager
 *      would be nice to do that from the settings dialog
 *      so we don't have to have another action button just for that.
 */
public class SettingsAction extends AbstractAction {
    @Override
    public void actionPerformed(ActionEvent e) {
        if (AppConfig.getInstance().showPropertiesDialog(MainWindow.getInstance())) {
            // User OK'd the dialog, reload the UI:
            ReloadUIAction.getInstance().actionPerformed(null);
        }
    }
}
