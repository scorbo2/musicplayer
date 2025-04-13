package ca.corbett.musicplayer.audio;

import ca.corbett.extras.MessageUtil;
import ca.corbett.musicplayer.Version;
import ca.corbett.musicplayer.extensions.MusicPlayerExtension;
import ca.corbett.musicplayer.extensions.MusicPlayerExtensionManager;
import ca.corbett.musicplayer.ui.MainWindow;

import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Responsible for loading and saving playlists - the playlist format
 * is very simple and is the same one used in MusicPlayer 1.x (described below).
 * <p>
 * <b>Line 1:</b> MusicPlayer playlist version:X.Y<br>
 * The first line of the file contains a colon to delimit the application
 * name (ignored by loading code) from the version number in major dot minor format.
 * The playlist loading code will refuse to parse any playlist generated with
 * a version of the app that is newer than the current one, in case the format
 * changes in future. But, since the format hasn't changed since the first version,
 * any older version playlist will load no problem.
 * </p>
 * <p>
 * <b>Subsequent lines</b><br>
 * Every subsequent line in the file is the absolute path of a file that was
 * part of the playlist at the time it was saved. No validation is done at the time
 * the playlist is loaded - if a referenced file no longer exists or does not
 * contain valid audio data, the problem won't be known until that file is played.
 * </p>
 * <p>
 * <b>NEW IN VERSION 2.x - extension support</b><br>
 * Extension methods are now available for playlist saving and loading. We will
 * query the extension manager here to see if any extension offers the ability
 * to save or load playlists. Out of the box, the app supports the format
 * described above, with a file extension of ".mplist". If your extension supports
 * some other playlist format, you can register the file extension, and your
 * extension will be invoked to handle saving/loading automatically, based on
 * the file extension of the file to be saved or loaded. This allows you to write
 * an extension to support importing a playlist from whatever file format you have.
 * </p>
 *
 * @author scorbo2
 * @since 2025-03-27
 */
public class PlaylistUtil {

    private static final Logger logger = Logger.getLogger(PlaylistUtil.class.getName());
    private static MessageUtil messageUtil;

    /**
     * This is the built-in playlist file format that we support out of the box.
     * If no extension registers an additional format, then this is the only
     * format that we support.
     */
    public static final FileNameExtensionFilter MPLIST =
            new FileNameExtensionFilter("MusicPlayer playlists (*.mplist)", "mplist");

    /**
     * Loads a playlist from the given playlist file and returns a list of all tracks that
     * were listed in that file. The load strategy is driven by the file extension of the
     * given file. The built-in supported format is identified by a .mplist extension.
     * If any other extension is given, we will check with our extension manager to see
     * if we have a registered extension that can load playlists in that format.
     * <p>
     * If any error occurs, the details are logged and an error dialog is
     * shown. In that case, an empty list will be returned.
     * </p>
     *
     * @param playlistFile A file containing a stored playlist.
     * @return A list of files parsed out of that playlist (might be empty).
     */
    public static List<File> loadPlaylist(File playlistFile) {
        // If it's our built-in format, just load it:
        if (playlistFile.getName().toLowerCase().endsWith(".mplist")) {
            return parseMPList(playlistFile);
        }

        // Otherwise, delegate loading to whichever extension supports this format:
        try {
            MusicPlayerExtension extension = MusicPlayerExtensionManager.getInstance().findExtensionForPlaylistFormat(playlistFile);
            if (extension == null) {
                throw new Exception("There are no extensions that can load playlists in this format: " + playlistFile.getName());
            }
            return extension.loadPlaylist(playlistFile);
        } catch (Exception e) {
            getMessageUtil().error("Error loading playlist", "Unable to load playlist: " + e.getMessage(), e);
        }
        return new ArrayList<>();
    }

    /**
     * Loads each of the given playlists and returns the combined list of all tracks that they contain.
     * No uniqueness checking is done, so if more than one of the playlist contains the same tracks,
     * there will be duplicates in the resulting list.
     *
     * @param playlistFiles A list of playlist files in any supported format.
     * @return A combined list of all the tracks that were present in all the input playlist files.
     */
    public static List<File> loadPlaylists(List<File> playlistFiles) {
        List<File> list = new ArrayList<>();
        for (File f : playlistFiles) {
            list.addAll(loadPlaylist(f));
        }
        return list;
    }

    /**
     * Saves the given list of files to a playlist described by targetFile.
     * The save strategy is driven by the file extension on the targetFile.
     * The built-in format is identified with a .mplist extension.
     * Any other extension will be delegated to our extension manager,
     * to find an extension that can save playlists in that format.
     * <p>
     * If an error occurs, the details are logged and an error dialog
     * is shown. In that case, the playlist is not saved to disk.
     * </p>
     * <p>
     * Overwrite behaviour is extension-specific! The built-in
     * save code will overwrite any existing playlist if the
     * given targetFile exists. But extensions are free to
     * decide what to do if the given file already exists.
     * </p>
     *
     * @param playlist   A list of tracks to save to the given playlist.
     * @param targetFile The destination save file.
     */
    public static void savePlaylist(List<File> playlist, File targetFile) {
        // If it's our built-in format, just save it:
        if (targetFile.getName().toLowerCase().endsWith(".mplist")) {
            saveMPList(playlist, targetFile);
            return;
        }

        // Otherwise, delegate saving to whichever extension supports this format:
        try {
            MusicPlayerExtension extension = MusicPlayerExtensionManager.getInstance().findExtensionForPlaylistFormat(targetFile);
            if (extension == null) {
                throw new Exception("There are no extensions that can save playlists in this format: " + targetFile.getName());
            }
            extension.savePlaylist(targetFile, playlist);
        } catch (Exception e) {
            getMessageUtil().error("Error saving playlist", "Unable to save playlist: " + e.getMessage(), e);
        }
    }

    /**
     * Invoked internally to parse out a MusicPlayer 1.x style playlist from the
     * given file. The mplist file format has not changed since the first version,
     * so it is still supported in 2.x.
     *
     * @param mpListFile A file containing an mplist playlist.
     * @return A list of files parsed from the given mpListFile.
     */
    protected static List<File> parseMPList(File mpListFile) {
        List<File> list = new ArrayList<>();

        // This format did not change from 1.0 to 1.5, so let's keep it the same for 2.x:
        try (BufferedReader reader = new BufferedReader(new FileReader(mpListFile))) {
            // The first line is the application name and version:
            String line = reader.readLine();
            String[] parts = line.split(":");
            if (parts.length != 2) {
                throw new IOException("Playlist is malformed: " + mpListFile.getAbsolutePath());
            }
            try {
                float playlistVersion = Float.parseFloat(parts[1]);
                float currentVersion = Float.parseFloat(Version.VERSION);
                if (playlistVersion > currentVersion) {
                    throw new IOException("Incompatible playlist version: " + parts[1] + " (current version is " + Version.VERSION + ")");
                }
            } catch (NumberFormatException nfe) {
                throw new IOException("Unknown playlist version: " + parts[1]);
            }

            // Parse the remainder of the file:
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                File f = new File(line);
                if (f.exists() && f.canRead()) {
                    list.add(f);
                } else {
                    logger.log(Level.WARNING, "Skipping unreadable playlist entry: {0}", f.getAbsolutePath());
                }
            }
        } catch (IOException ex) {
            getMessageUtil().error("Unable to read playlist", "Problem reading playlist: " + ex.getMessage(), ex);
        }

        return list;
    }

    /**
     * Saves the given list of files to the target file using the built-in
     * mplist file format. The mplist format has not changed since the very
     * first version of MusicPlayer, so it is still supported in 2.x.
     *
     * @param playlist         A list of files to write to the target file.
     * @param targetMPListFile The target file. Overwritten if it exists.
     */
    protected static void saveMPList(List<File> playlist, File targetMPListFile) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(targetMPListFile))) {
            writer.write(Version.NAME + " playlist version:" + Version.VERSION);
            writer.newLine();
            for (File f : playlist) {
                writer.write(f.getAbsolutePath());
                writer.newLine();
            }
            writer.flush();
        } catch (IOException ioe) {
            getMessageUtil().error("Problem saving playlist", "Unable to save playlist: " + ioe.getMessage(), ioe);
        }
    }

    private static MessageUtil getMessageUtil() {
        if (messageUtil == null) {
            messageUtil = new MessageUtil(MainWindow.getInstance(), logger);
        }

        return messageUtil;
    }
}
