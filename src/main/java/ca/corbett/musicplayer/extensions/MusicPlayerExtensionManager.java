package ca.corbett.musicplayer.extensions;

import ca.corbett.extensions.ExtensionManager;
import ca.corbett.extras.properties.AbstractProperty;
import ca.corbett.extras.properties.LabelProperty;
import ca.corbett.extras.properties.Properties;
import ca.corbett.musicplayer.Actions;
import ca.corbett.musicplayer.Version;
import ca.corbett.musicplayer.audio.PlaylistUtil;
import ca.corbett.musicplayer.extensions.builtin.ExtraAnimations;
import ca.corbett.musicplayer.extensions.builtin.ExtraThemes;
import ca.corbett.musicplayer.extensions.builtin.ExtraVisualizers;
import ca.corbett.musicplayer.extensions.builtin.QuickLoadExtension;
import ca.corbett.musicplayer.ui.AppTheme;
import ca.corbett.musicplayer.ui.AudioPanelIdleAnimation;
import ca.corbett.musicplayer.ui.VisualizationManager;

import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Manages extensions for musicplayer, and provides wrapper methods to make it
 * easy for the application code to interrogate extensions as to their capabilities.
 *
 * @author scorbo2
 * @since 2025-03-26
 */
public class MusicPlayerExtensionManager extends ExtensionManager<MusicPlayerExtension> {

    private static final Logger logger = Logger.getLogger(MusicPlayerExtensionManager.class.getName());

    public static final String SCAN_DIR_PROP = "ca.corbett.musicplayer.extensions.dir";
    private static MusicPlayerExtensionManager instance;

    protected MusicPlayerExtensionManager() {
        // Load all the built-in extensions:
        addExtension(new ExtraThemes(), true);
        addExtension(new ExtraAnimations(), true);
        addExtension(new ExtraVisualizers(), true);
        addExtension(new QuickLoadExtension(), true);

        // Let's try to scan for dynamically loaded extensions:
        String scanDir = System.getProperty(SCAN_DIR_PROP);
        if (scanDir != null) {
            File extensionDir = new File(scanDir);
            if (extensionDir.exists() && extensionDir.canRead() && extensionDir.isDirectory()) {
                int count = getLoadedExtensionCount();
                loadExtensions(extensionDir, MusicPlayerExtension.class, Version.NAME, Version.VERSION);
                if (getLoadedExtensionCount() > count) {
                    logger.info("Successfully loaded " + (getLoadedExtensionCount() - count) + " dynamic extensions");
                } else {
                    logger.info("Extension manager: found no dynamic extensions in extension dir: " + extensionDir.getAbsolutePath());
                }
            } else {
                logger.warning("Supplied extension scan dir does not exist or is not readable: " + extensionDir.getAbsolutePath());
            }
        }
    }

    public static MusicPlayerExtensionManager getInstance() {
        if (instance == null) {
            instance = new MusicPlayerExtensionManager();
        }

        return instance;
    }

    /**
     * Overridden here just for logging purposes.
     */
    @Override
    public void activateAll() {
        super.activateAll();
        logger.info("Extension manager initialized with " + getEnabledLoadedExtensions().size() + " active extensions.");
    }

    /**
     * Overridden here just for logging purposes.
     */
    @Override
    public void deactivateAll() {
        super.deactivateAll();
        logger.info("Extension manager: all extensions have been shut down.");
    }

    /**
     * Overridden here so we can add a read-only view of the extension scan dir
     * and some instructions for how to change the value. I want to add these instructions
     * dead last so that they show up at the end of the properties dialog
     * no matter how many extensions are loaded.
     *
     * @return A List of all properties exposed by all enabled extensions.
     */
    @Override
    public List<AbstractProperty> getAllEnabledExtensionProperties() {
        List<AbstractProperty> props = super.getAllEnabledExtensionProperties();

        Font labelFont = new Font(Font.DIALOG, Font.PLAIN, 12);
        StaticLabelProperty label1 = new StaticLabelProperty("Extensions.Configuration.label1", "The following directory will be scanned for extension jars:");
        label1.setFont(labelFont);
        StaticLabelProperty label2 = new StaticLabelProperty("Extensions.Configuration.scanDir", System.getProperty(SCAN_DIR_PROP) == null ? "(not set)" : System.getProperty(SCAN_DIR_PROP));
        label2.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));
        StaticLabelProperty label3 = new StaticLabelProperty("Extensions.Configuration.label2",
                "<html>If the directory exists and is readable, it is scanned at startup<br>" +
                        "automatically. You can set it using the following system property,<br>" +
                        "but it requires an application restart!<br><br>" +
                        "<pre>" + SCAN_DIR_PROP + "</pre></html>");
        label3.setFont(labelFont);

        props.add(label1);
        props.add(label2);
        props.add(label3);
        return props;
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

    /**
     * Returns a list of custom themes from all registered and enabled extensions that
     * supply at least one.
     *
     * @return a List of application themes supplied by extensions
     */
    public List<AppTheme.Theme> getCustomThemes() {
        List<AppTheme.Theme> themes = new ArrayList<>();

        for (MusicPlayerExtension extension : getEnabledLoadedExtensions()) {
            List<AppTheme.Theme> list = extension.getCustomThemes();
            if (list != null && !list.isEmpty()) {
                themes.addAll(list);
            }
        }

        return themes;
    }

    /**
     * Returns a combined list of all idle animations supplied by all extensions that
     * supply at least one.
     *
     * @return a List of idle animations supplied by all extensions.
     */
    public List<AudioPanelIdleAnimation.Animation> getCustomIdleAnimations() {
        List<AudioPanelIdleAnimation.Animation> animations = new ArrayList<>();

        for (MusicPlayerExtension extension : getEnabledLoadedExtensions()) {
            List<AudioPanelIdleAnimation.Animation> list = extension.getCustomIdleAnimations();
            if (list != null && !list.isEmpty()) {
                animations.addAll(list);
            }
        }

        return animations;
    }

    /**
     * Returns a list of all Visualizer instances from all extensions that supply at least one.
     *
     * @return a List of all Visualizers supplied by all extensions.
     */
    public List<VisualizationManager.Visualizer> getCustomVisualizers() {
        List<VisualizationManager.Visualizer> visualizers = new ArrayList<>();

        for (MusicPlayerExtension extension : getEnabledLoadedExtensions()) {
            List<VisualizationManager.Visualizer> list = extension.getCustomVisualizers();
            if (list != null && !list.isEmpty()) {
                visualizers.addAll(list);
            }
        }

        return visualizers;
    }

    /**
     * Invoked when the application receives a keyboard shortcut. All extensions will be given
     * a chance to respond to the KeyEvent. Processing does not stop if one extension reports
     * that it did something with the KeyEvent, so in theory, multiple extensions could respond
     * to the same KeyEvent.
     *
     * @param keyEvent The KeyEvent that triggered this message.
     * @return true if any registered extension handled the KeyEvent.
     */
    public boolean handleKeyEvent(KeyEvent keyEvent) {
        boolean handled = false;
        for (MusicPlayerExtension extension : getEnabledLoadedExtensions()) {
            handled = handled || extension.handleKeyEvent(keyEvent);
        }
        return handled;
    }

    /**
     * KLUDGE KLUDGE KLUDGE - HACK ALERT.
     * This is just to prevent our static labels from saving themselves to our
     * property file, which is really goofy. I will fix it in
     * <a href="https://github.com/scorbo2/swing-extras/issues/8">swing-extras issue 8</a>.
     */
    public static class StaticLabelProperty extends LabelProperty {
        public StaticLabelProperty(String name, String label) {
            super(name, label);
        }

        @Override
        public void saveToProps(Properties props) {
        }

        @Override
        public void loadFromProps(Properties props) {
        }
    }
}

