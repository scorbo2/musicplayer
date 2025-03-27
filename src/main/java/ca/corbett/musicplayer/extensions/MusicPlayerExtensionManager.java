package ca.corbett.musicplayer.extensions;

import ca.corbett.extensions.ExtensionManager;
import ca.corbett.musicplayer.Actions;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages extensions for musicplayer, and provides wrapper methods to make it
 * easy for the application code to interrogate extensions as to their capabilities.
 *
 * @author scorbo2
 * @since 2025-03-26
 */
public class MusicPlayerExtensionManager extends ExtensionManager<MusicPlayerExtension> {

    private static MusicPlayerExtensionManager instance;

    protected MusicPlayerExtensionManager() {

    }

    public static MusicPlayerExtensionManager getInstance() {
        if (instance == null) {
            instance = new MusicPlayerExtensionManager();
        }

        return instance;
    }

    /**
     * Returns a combined List of all media player actions supplied by
     * all enabled and loaded extensions. These will be returned in the
     * order in which extensions were loaded.
     *
     * @return A List of MPAction instances. May be empty but not null.
     */
    public List<Actions.MPAction> getMediaPlayerActions() {
        List<Actions.MPAction> allActions = new ArrayList<>();
        for (MusicPlayerExtension extension : getEnabledLoadedExtensions()) {
            List<Actions.MPAction> list = extension.getMediaPlayerActions();
            if (list != null && !list.isEmpty()) {
                allActions.addAll(list);
            }
        }
        return allActions;
    }

    /**
     * Returns a combined List of all playlist actions supplied by
     * all enabled and loaded extensions. These will be returned in the
     * order in which extensions were loaded.
     *
     * @return A List of MPAction instances. May be empty but not null.
     */
    public List<Actions.MPAction> getPlaylistActions() {
        List<Actions.MPAction> allActions = new ArrayList<>();
        for (MusicPlayerExtension extension : getEnabledLoadedExtensions()) {
            List<Actions.MPAction> list = extension.getPlaylistActions();
            if (list != null && !list.isEmpty()) {
                allActions.addAll(list);
            }
        }
        return allActions;
    }
}
