package ca.corbett.musicplayer.extensions;

import ca.corbett.extensions.ExtensionManager;
import ca.corbett.musicplayer.Actions;
import ca.corbett.musicplayer.audio.PlaylistUtil;

import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
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

    /**
     * Returns a list of supported playlist file extensions - this represents
     * the file formats that we can save/load playlists to/from.
     */
    public List<FileNameExtensionFilter> getPlaylistFileExtensionFilters() {
        List<FileNameExtensionFilter> list = new ArrayList<>();

        // Add the built-in one first:
        list.add(PlaylistUtil.MPLIST);

        // Now gather any additional ones from extensions:
        for (MusicPlayerExtension extension : getEnabledLoadedExtensions()) {
            FileNameExtensionFilter filter = extension.getCustomPlaylistExtensionFilter();
            if (filter != null) {
                list.add(filter);
            }
        }

        return list;
    }

    /**
     * Returns the first extension that claims it can load/save playlists of the given
     * file type, or null if no such extension makes that claim. If more than one extension
     * can support the given file format, we return the first one based on extension
     * load order.
     */
    public MusicPlayerExtension findExtensionForPlaylistFormat(File targetFile) {
        for (MusicPlayerExtension extension : getEnabledLoadedExtensions()) {
            FileNameExtensionFilter filter = extension.getCustomPlaylistExtensionFilter();
            if (filter != null && filter.accept(targetFile)) {
                return extension;
            }
        }
        return null;
    }
}
