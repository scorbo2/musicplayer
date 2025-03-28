package ca.corbett.musicplayer.extensions;

import ca.corbett.extensions.AppExtension;
import ca.corbett.musicplayer.Actions;

import javax.swing.filechooser.FileNameExtensionFilter;
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
}
