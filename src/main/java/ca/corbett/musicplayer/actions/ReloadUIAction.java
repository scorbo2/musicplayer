package ca.corbett.musicplayer.actions;

import ca.corbett.musicplayer.ui.UIReloadable;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * If your UI class depends on properties in AppConfig in order to render
 * itself (for example, for configurable theme colors or whatnot), you
 * can register yourself with this class to receive notice whenever it's
 * time to reload the UI. You can then respond by redrawing whatever
 * components might have been affected by property changes.
 *
 * @author scorbo2
 * @since 2025-03-25
 */
public class ReloadUIAction extends AbstractAction {

    private static final Logger logger = Logger.getLogger(ReloadUIAction.class.getName());

    private static ReloadUIAction instance;

    private final Set<UIReloadable> reloadables = new HashSet<>();

    private ReloadUIAction() {
    }

    public static ReloadUIAction getInstance() {
        if (instance == null) {
            instance = new ReloadUIAction();
        }

        return instance;
    }

    public void registerReloadable(UIReloadable reloadable) {
        reloadables.add(reloadable);
    }

    public void unregisterReloadable(UIReloadable reloadable) {
        reloadables.remove(reloadable);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        logger.info("Reloading UI");
        for (UIReloadable reloadable : reloadables) {
            reloadable.reloadUI();
        }
    }
}
