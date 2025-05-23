package ca.corbett.musicplayer.extensions;

import ca.corbett.extensions.AppExtension;
import ca.corbett.musicplayer.Actions;
import ca.corbett.musicplayer.ui.AppTheme;
import ca.corbett.musicplayer.ui.AudioPanelIdleAnimation;
import ca.corbett.musicplayer.ui.TrackInfoDialog;
import ca.corbett.musicplayer.ui.VisualizationManager;

import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
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

    /**
     * If your extension supports playlist loading and saving in a custom
     * format, you can return the FileNameExtensionFilter here. A value
     * of null means your extension does not support this. If you register
     * an extension here, your extension will be invoked to perform
     * the load and/or save as needed, based on the file extension of
     * the file to be read or written.
     */
    public FileNameExtensionFilter getCustomPlaylistExtensionFilter() {
        return null;
    }

    /**
     * If your extension reported that it could handle saving playlists
     * with a specific file extension via getCustomPlaylistExtensionFilter(), then
     * you may be called upon here to handle the saving of a given playlist
     * to a file of that type.
     *
     * @param outputFile The destination file for the playlist.
     * @param playlist   The list of files to save.
     * @throws IOException If something goes wrong during the save.
     */
    public void savePlaylist(File outputFile, List<File> playlist) throws IOException {
    }

    /**
     * If your extension reported that it could handle loading playlists
     * with a specific file extension via getCustomPlaylistExtensionFilter(), then
     * you may be called upon here to handle the loading of a playlist
     * from a file of that type.
     *
     * @param inputFile The playlist file on disk.
     * @return A list of files that you loaded from the input file.
     * @throws IOException If something goes wrong during the load
     */
    public List<File> loadPlaylist(File inputFile) throws IOException {
        return new ArrayList<>();
    }

    /**
     * Allows the extension to supply a list of custom application themes
     * that the user can choose from in AppConfig. An empty list is
     * returned by default, indicating no custom themes.
     *
     * @return a List of application themes supplied by this extension
     */
    public List<AppTheme.Theme> getCustomThemes() {
        return new ArrayList<>();
    }

    /**
     * Allows the extension to supply a list of custom idle animations
     * that the user can choose from in AppConfig. An empty list is
     * returned by default, indicating no custom animations.
     *
     * @return A List of idle animations supplied by this extension
     */
    public List<AudioPanelIdleAnimation.Animation> getCustomIdleAnimations() {
        return new ArrayList<>();
    }

    /**
     * Allows the extension to supply a custom Visualizer for full-screen
     * visualization of tracks.
     *
     * @return A List of Visualizer instances. Might be empty.
     */
    public List<VisualizationManager.Visualizer> getCustomVisualizers() {
        return new ArrayList<>();
    }

    /**
     * Invoked when the application receives a keyboard shortcut. The extension
     * can choose to do something with it or not. Processing the keyboard event
     * does not stop when an extension acts on a shortcut, so in theory multiple
     * extensions could respond to the same keyboard shortcut.
     *
     * @param keyEvent the KeyEvent that triggered this message.
     * @return true if this extension handled the shortcut (default false).
     */
    public boolean handleKeyEvent(KeyEvent keyEvent) {
        return false;
    }

    /**
     * Returns a TrackInfoDialog (or some derived instance) for the given
     * File, if this extension is capable of displaying detailed track
     * information for an audio file.
     *
     * @return a TrackInfoDialog instance, or null if this extension doesn't supply one.
     */
    public TrackInfoDialog getTrackInfoDialog(File trackFile) {
        return null;
    }
}
