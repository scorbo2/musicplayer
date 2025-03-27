package ca.corbett.musicplayer.extensions;

import ca.corbett.extensions.AppExtension;
import ca.corbett.musicplayer.Actions;

import java.util.ArrayList;
import java.util.List;

/**
 * A starting point for extensions within the MusicPlayer app.
 * Extend this class and implement whichever method(s) are relevant
 * to your app, then bundle your code up into a jar file and put it
 * in the extension directory and your extension will automatically
 * be picked up when MusicPlayer is restarted.
 *
 * @author scorbo2
 * @since 2025-03-26
 */
public abstract class MusicPlayerExtension implements AppExtension {

    /**
     * Override this method if your extension provides custom
     * actions for the media player. These will be inserted into
     * the toolbar at the bottom of the media player. Note that you
     * have to supply the resource path of an image icon to be
     * used for the button. You can bundle a 48x48 icon in your
     * jar's resources and pass the resource location to
     * your MPAction. See the MPAction class javadocs
     * for an example, and refer to the icons bundled with
     * the musicplayer base jar for icon examples.
     *
     * @return A List of MPActions. A null or empty list will be ignored.
     */
    public List<Actions.MPAction> getMediaPlayerActions() {
        return new ArrayList<>();
    }

    /**
     * Override this method if your extension provides custom
     * actions for the playlist. These will be inserted into the
     * toolbar at the bottom of the playlist view.  Note that you
     * have to supply the resource path of an image icon to be
     * used for the button. You can bundle a 48x48 icon in your
     * jar's resources and pass the resource location to
     * your MPAction. See the MPAction class javadocs
     * for an example, and refer to the icons bundled with
     * the musicplayer base jar for icon examples.
     *
     * @return A List of MPActions. A null or empty list will be ignored.
     */
    public List<Actions.MPAction> getPlaylistActions() {
        return new ArrayList<>();
    }

}
