package ca.corbett.musicplayer.extensions.builtin;

import ca.corbett.extensions.AppExtensionInfo;
import ca.corbett.extras.properties.AbstractProperty;
import ca.corbett.extras.properties.DirectoryProperty;
import ca.corbett.extras.properties.LabelProperty;
import ca.corbett.musicplayer.AppConfig;
import ca.corbett.musicplayer.Version;
import ca.corbett.musicplayer.extensions.MusicPlayerExtension;

import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides an extension for MusicPlayer 2 that allows quick load
 * and save of playlists similar to what was built into the old
 * MusicPlayer 1 series. That is, instead of going through a file
 * chooser dialog and browsing around directories looking for
 * your saved playlist file, we will have a configurable
 * playlist save dir, and this extension will show a very simple
 * popup dialog just showing a flat list of playlists in that
 * directory, and you can just up and down arrow through it
 * and hit enter to very quickly load one. Similarly, hit s
 * to bring up a very simple playlist save dialog that doesn't
 * force you to browse around to choose a save location.
 *
 * @author scorbo2
 * @since 2025-04-01 (based on code from MusicPlayer 1.5)
 */
public class QuickLoadExtension extends MusicPlayerExtension {
    private final AppExtensionInfo info;

    public static final String DIR_PROP = "Quickload.General.quickDir";
    public static final String DEFAULT_DIR = ".MusicPlayer";

    public QuickLoadExtension() {
        info = new AppExtensionInfo.Builder("Playlist quickload")
                .setShortDescription("Quickload and quicksave your playlists!")
                .setLongDescription("Hit L from the main window or from the visualization window " +
                        "to bring up a very simple playlist open dialog, or hit S to bring up a " +
                        "very simple playlist save dialog. This supplements the existing " +
                        "playlist open and save dialogs but is easier to use. You can choose " +
                        "the playlist save directory in application settings.")
                .setVersion(Version.VERSION)
                .setAuthor("Steve Corbett")
                .setTargetAppVersion(Version.VERSION)
                .setTargetAppName(Version.NAME)
                .setReleaseNotes("[2025-04-01] for the MusicPlayer 2.0 release")
                .addCustomField("Shortcuts", "L = load, S = save")
                .build();
    }

    @Override
    public AppExtensionInfo getInfo() {
        return info;
    }

    @Override
    public List<AbstractProperty> getConfigProperties() {
        List<AbstractProperty> props = new ArrayList<>();

        props.add(LabelProperty.createLabel("Quickload.General.label",
                "<html>The following directory will be used for quickloading<br>" +
                        "and quicksaving playlists. Press L or S from the main window<br>" +
                        "to do this. Note: changing the directory here doesn't move<br>" +
                        "any playlists you've already saved there!</html>"));

        File defaultDir = new File(System.getProperty("user.home"), DEFAULT_DIR);
        props.add(new DirectoryProperty("Quickload.General.quickDir", "Directory:", false, defaultDir));

        return props;
    }

    public static File getQuickDir() {
        return ((DirectoryProperty) AppConfig.getInstance().getPropertiesManager().getProperty(QuickLoadExtension.DIR_PROP)).getDirectory();
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }

    @Override
    public boolean handleKeyEvent(KeyEvent keyEvent) {
        if (keyEvent.getKeyCode() == KeyEvent.VK_L) {
            QuickLoadDialog.getInstance().setVisible(true);
            return true;
        } else if (keyEvent.getKeyCode() == KeyEvent.VK_S) {
            QuickSaveDialog.getInstance().setVisible(true);
            return true;
        }

        return false;
    }
}
