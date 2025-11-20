package ca.corbett.musicplayer.ui;

import ca.corbett.extras.MessageUtil;
import ca.corbett.extras.audio.PlaybackThread;
import ca.corbett.extras.image.ImageUtil;
import ca.corbett.extras.io.FileSystemUtil;
import ca.corbett.extras.logging.LogConsole;
import ca.corbett.musicplayer.AppConfig;
import ca.corbett.musicplayer.Version;
import ca.corbett.musicplayer.actions.StopAction;
import ca.corbett.musicplayer.extensions.MusicPlayerExtensionManager;
import ca.corbett.updates.UpdateManager;
import ca.corbett.updates.UpdateSources;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents the main window of the application, and wraps all our UI components.
 *
 * @author scorbo2
 * @since 2025-03-23
 */
public class MainWindow extends JFrame {

    private static final Logger logger = Logger.getLogger(MainWindow.class.getName());

    public static final int DEFAULT_WIDTH = 420;
    public static final int DEFAULT_HEIGHT = 350;

    private static final int MIN_WIDTH = 420;
    private static final int MIN_HEIGHT = 320;

    private static MainWindow instance;
    private MessageUtil messageUtil;
    private final Timer resizeTimer;
    private UpdateManager updateManager;

    private MainWindow() {
        super(Version.FULL_NAME);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT));
        setMinimumSize(new Dimension(MIN_WIDTH, MIN_HEIGHT));
        setIconImage(loadIconResource("/ca/corbett/musicplayer/images/logo.png", 64, 64));
        setLayout(new BorderLayout());
        resizeTimer = new Timer(250, e -> saveWindowState());
        resizeTimer.setRepeats(false);
        resizeTimer.setCoalesce(false);
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                resizeTimer.restart();
            }
        });

        add(AudioPanel.getInstance(), BorderLayout.NORTH);
        add(Playlist.getInstance(), BorderLayout.CENTER);
    }

    /**
     * Overridden here so we can load our app config settings and
     * start the "idle" animation.
     *
     * @param visible If true, the MainWindow will be shown and the app will begin.
     */
    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            PlaybackThread.setUpdateIntervalMs(250); // let's go slightly faster than default
            MusicPlayerExtensionManager.getInstance().activateAll();
            loadWindowState();
            AudioPanelIdleAnimation.getInstance().go();
            VisualizationWindow.getInstance(); // forces initialization of fullscreen stuff so it's ready to go later.
            LogConsole.getInstance().setIconImage(loadIconResource("/ca/corbett/musicplayer/images/logo.png", 64, 64));
            parseUpdateSources();
        }
    }

    /**
     * Loads and returns an image icon resource, scaling up or down to the given size if needed.
     *
     * @param resourceName The path to the resource file containing the image.
     * @param width        The desired width of the image.
     * @param height       The desired height of the image.
     * @return An image, loaded and scaled, or null if the resource was not found.
     */
    public static BufferedImage loadIconResource(String resourceName, int width, int height) {
        BufferedImage image = null;
        try {
            URL url = MainWindow.class.getResource(resourceName);
            if (url == null) {
                throw new IOException("Image resource not found: " + resourceName);
            }
            image = ImageUtil.loadImage(url);

            // If the width or height don't match, scale it up or down as needed:
            if (image.getWidth() != width || image.getHeight() != height) {
                image = ImageUtil.generateThumbnailWithTransparency(image, width, height);
            }
        } catch (IOException ioe) {
            logger.log(Level.SEVERE, "Error loading image: " + ioe.getMessage(), ioe);
        }

        return image;
    }

    /**
     * Stupid swing nonsense... need to force a repaint sometimes to force components to
     * appear/disappear when added/removed.
     *
     * @param jiggy The thing with which we are to get jiggy.
     */
    public static void rejigger(final Component jiggy) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                jiggy.invalidate();
                jiggy.revalidate();
                jiggy.repaint();
            }
        });
    }

    public UpdateManager getUpdateManager() {
        return updateManager;
    }

    private void saveWindowState() {
        AppConfig.getInstance().setWindowWidth(getWidth());
        AppConfig.getInstance().setWindowHeight(getHeight());
        AppConfig.getInstance().save();
    }

    private void loadWindowState() {
        setSize(new Dimension(AppConfig.getInstance().getWindowWidth(), AppConfig.getInstance().getWindowHeight()));
    }

    private void parseUpdateSources() {
        if (Version.UPDATE_SOURCES_FILE != null) {
            try {
                Gson gson = new GsonBuilder().create();
                UpdateSources updateSources = gson.fromJson(
                    FileSystemUtil.readFileToString(Version.UPDATE_SOURCES_FILE),
                                              UpdateSources.class);
                updateManager = new UpdateManager(updateSources);
                logger.info("Update sources provided. Dynamic extension discovery is enabled.");
            }
            catch (Exception e) {
                logger.log(Level.SEVERE,
                           "Unable to parse update sources. Extension download will not be available. Error: "
                               + e.getMessage(),
                           e);
            }
        }
        else {
            logger.log(Level.INFO, "No update sources provided. Dynamic extension discovery disabled.");
        }
    }

    public static MainWindow getInstance() {
        if (instance == null) {
            instance = new MainWindow();

            // Set up our global key listener once:
            KeyboardManager.addGlobalKeyListener(instance);

            // Add our WindowAdapter once:
            instance.addWindowListener(new WindowAdapter() {
                /**
                 * Invoked when the user manually closes a window by clicking its X button
                 * or using a keyboard shortcut like Ctrl+Q or whatever. This event handler
                 * is NOT invoked when you manually dispose() the window (at least in my
                 * testing on linux mint).
                 */
                @Override
                public void windowClosing(WindowEvent e) {
                    new StopAction().actionPerformed(null);
                    VisualizationWindow.getInstance().stopFullScreen();
                    MusicPlayerExtensionManager.getInstance().deactivateAll();
                    logger.info("Application windowClosing(): finished cleanup.");
                }

                /**
                 * Invoked when you programmatically dispose() of the window. Note that the
                 * user manually closing the window via the OS does NOT invoke this handler
                 * (at least in my testing on linux mint).
                 */
                @Override
                public void windowClosed(WindowEvent e) {
                    VisualizationWindow.getInstance().stopFullScreen();
                    MusicPlayerExtensionManager.getInstance().deactivateAll();
                    logger.info("Application windowClosed(): finished cleanup.");
                }
            });
        }
        return instance;
    }

    private MessageUtil getMessageUtil() {
        if (messageUtil == null) {
            messageUtil = new MessageUtil(getInstance(), logger);
        }
        return messageUtil;
    }
}
