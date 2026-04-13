package ca.corbett.musicplayer.ui;

import ca.corbett.extras.MessageUtil;
import ca.corbett.extras.SingleInstanceManager;
import ca.corbett.extras.audio.PlaybackThread;
import ca.corbett.extras.image.ImageUtil;
import ca.corbett.extras.io.KeyStrokeManager;
import ca.corbett.extras.logging.LogConsole;
import ca.corbett.extras.properties.KeyStrokeProperty;
import ca.corbett.musicplayer.AppConfig;
import ca.corbett.musicplayer.Main;
import ca.corbett.musicplayer.Version;
import ca.corbett.musicplayer.actions.AboutAction;
import ca.corbett.musicplayer.actions.ExitAction;
import ca.corbett.musicplayer.actions.FullScreenAction;
import ca.corbett.musicplayer.actions.FullScreenStopAction;
import ca.corbett.musicplayer.actions.NextAction;
import ca.corbett.musicplayer.actions.PauseAction;
import ca.corbett.musicplayer.actions.PlaylistOpenAction;
import ca.corbett.musicplayer.actions.PlaylistRemoveAllAction;
import ca.corbett.musicplayer.actions.PlaylistRemoveOneAction;
import ca.corbett.musicplayer.actions.PlaylistSaveAction;
import ca.corbett.musicplayer.actions.PrevAction;
import ca.corbett.musicplayer.actions.ReloadUIAction;
import ca.corbett.musicplayer.actions.SettingsAction;
import ca.corbett.musicplayer.actions.StopAction;
import ca.corbett.musicplayer.audio.AudioMetadata;
import ca.corbett.musicplayer.audio.AudioUtil;
import ca.corbett.musicplayer.extensions.MusicPlayerExtensionManager;
import ca.corbett.updates.UpdateManager;
import ca.corbett.updates.UpdateSources;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents the main window of the application, and wraps all our UI components.
 *
 * @author scorbo2
 * @since 2025-03-23
 */
public class MainWindow extends JFrame implements UIReloadable, AudioPanelListener {

    private static final Logger logger = Logger.getLogger(MainWindow.class.getName());

    public static final int DEFAULT_WIDTH = 420;
    public static final int DEFAULT_HEIGHT = 350;

    private static final int MIN_WIDTH = 420;
    private static final int MIN_HEIGHT = 320;

    private static MainWindow instance;
    private MessageUtil messageUtil;
    private final Timer resizeTimer;
    private UpdateManager updateManager;
    private boolean isSingleInstanceModeEnabled;
    private final KeyStrokeManager keyStrokeManager;
    private AudioMetadata currentMetadata;

    private MainWindow() {
        super(Version.FULL_NAME);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT));
        setMinimumSize(new Dimension(MIN_WIDTH, MIN_HEIGHT));
        setIconImage(loadIconResource("/ca/corbett/musicplayer/images/logo.png", 64, 64));
        setLayout(new BorderLayout());
        this.keyStrokeManager = new KeyStrokeManager(this);
        resizeTimer = new Timer(250, e -> saveWindowState());
        resizeTimer.setRepeats(false);
        resizeTimer.setCoalesce(false);
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                resizeTimer.restart();
            }
        });

        currentMetadata = null;
        add(AudioPanel.getInstance(), BorderLayout.NORTH);
        add(Playlist.getInstance(), BorderLayout.CENTER);
        AudioPanel.getInstance().addAudioPanelListener(this);
        AudioMetadata.addChangeListener(this::metadataChanged);
        registerKeyStrokes(keyStrokeManager);
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
            enableDragAndDrop();
            ReloadUIAction.getInstance().registerReloadable(this);
            isSingleInstanceModeEnabled = AppConfig.getInstance().isSingleInstanceEnabled();
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
                UpdateSources updateSources = UpdateSources.fromFile(Version.UPDATE_SOURCES_FILE);
                updateManager = new UpdateManager(updateSources);
                updateManager.registerShutdownHook(MainWindow::cleanup);
                Version.getAboutInfo().updateManager = updateManager;
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

    /**
     * Enables drag-and-drop of audio files or playlists from the filesystem onto this window.
     */
    private void enableDragAndDrop() {
        DropTarget dropTarget = new DropTarget(this, new DropTargetAdapter() {
            @Override
            public void dragOver(DropTargetDragEvent dtde) {
                if (isValidFileDrag(dtde)) {
                    dtde.acceptDrag(DnDConstants.ACTION_COPY);
                }
                else {
                    dtde.rejectDrag();
                }
            }

            @Override
            public void drop(DropTargetDropEvent dtde) {
                dtde.acceptDrop(DnDConstants.ACTION_COPY);

                Transferable transferable = dtde.getTransferable();
                if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    try {
                        @SuppressWarnings("unchecked")
                        List<File> files = (List<File>)transferable.getTransferData(DataFlavor.javaFileListFlavor);

                        for (File file : files) {
                            if (AudioUtil.isValidAudioFile(file)) {
                                Playlist.getInstance().addItem(file);
                            }
                            else if (AudioUtil.isValidPlaylist(file)) {
                                Playlist.getInstance().appendPlaylist(file);
                            }
                        }

                        revalidate();
                        repaint();
                        dtde.dropComplete(true);
                    }
                    catch (UnsupportedFlavorException | IOException e) {
                        logger.warning("Ignoring unsupported drag and drop operation.");
                        dtde.dropComplete(false);
                    }
                }
                else {
                    dtde.dropComplete(false);
                }
            }

            private boolean isValidFileDrag(DropTargetDragEvent dtde) {
                return dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor);// We'll validate actual files on drop
            }
        });

        setDropTarget(dropTarget);
    }

    /**
     * Accepts a list of audio files or playlists to be added to the current playlist.
     * This is invoked at startup if we were given any command line arguments, and
     * can also be invoked at runtime if we are running in single-instance mode and
     * receive arguments from a new instance.
     */
    public void processStartArgs(List<String> args) {
        // Bring the main window to the front:
        // (If running in single instance mode, we want to make sure the user sees it.)
        bringToFront();

        // If we were given no args, we're done:
        // But note that we do this AFTER bringing the window to the front.
        // If you try to launch a second instance when the first instance is up,
        // but is obscured by some other window, we want to bring the single instance to the front.
        // Otherwise it may seem like nothing happened.
        if (args == null || args.isEmpty()) {
            return;
        }

        // Current list size will be the selection index of the first added item:
        int firstAddedIndex = Playlist.getInstance().getItemCount();

        // Add all given tracks:
        boolean addedAtLeastOne = false;
        for (String arg : args) {
            // Strip wrapping single quotes if present:
            // (some OSes/shells may add these, tested on Linux Mint with Cinnamon, and it's a problem there):
            if (arg.startsWith("'") && arg.endsWith("'") && arg.length() > 1) {
                arg = arg.substring(1, arg.length() - 1);
            }

            // Now we can process the argument as usual:
            File candidate = new File(arg);
            if (AudioUtil.isValidAudioFile(candidate)) {
                Playlist.getInstance().addItem(candidate);
                logger.info("Added file from startup argument: " + arg);
                addedAtLeastOne = true;
            }
            else if (AudioUtil.isValidPlaylist(candidate)) {
                Playlist.getInstance().appendPlaylist(candidate);
                addedAtLeastOne = true;
                logger.info("Added playlist from startup argument: " + arg);
            }
            else {
                logger.warning("Unable to process start argument: " + arg);
            }
        }

        // If we didn't add anything, we're done:
        if (!addedAtLeastOne) {
            return;
        }

        // Otherwise, select and start playing the first added item:
        // Arbitrary decision: if we were already playing something, interrupt it and play the new stuff.
        Playlist.getInstance().selectAndPlay(firstAddedIndex);
    }

    public static MainWindow getInstance() {
        if (instance == null) {
            instance = new MainWindow();

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
                    cleanup();
                }

                /**
                 * Invoked when you programmatically dispose() of the window. Note that the
                 * user manually closing the window via the OS does NOT invoke this handler
                 * (at least in my testing on linux mint).
                 */
                @Override
                public void windowClosed(WindowEvent e) {
                    cleanup();
                }
            });
        }
        return instance;
    }

    /**
     * Performs shutdown and cleanup tasks prior to the application exiting.
     */
    private static void cleanup() {
        MainWindow.getInstance().keyStrokeManager.dispose();
        new StopAction().actionPerformed(null);
        AudioLoadCoordinator.getInstance().shutdown();
        try {
            // If we're already on the UI thread, we can just stop fullscreen mode directly:
            if (SwingUtilities.isEventDispatchThread()) {
                VisualizationWindow.getInstance().stopFullScreen();
            }

            else {
                // Otherwise, we have to be a bit careful about it.
                // We can't invokeLater(), because the UpdateManager won't wait for it to finish.
                // So, we will block until fullscreen mode is properly stopped, if it is active:
                SwingUtilities.invokeAndWait(() -> VisualizationWindow.getInstance().stopFullScreen());
            }
        }
        catch (Exception e) {
            logger.log(Level.WARNING, "Error while stopping fullscreen during cleanup: " + e.getMessage(), e);
        }
        MusicPlayerExtensionManager.getInstance().deactivateAll();
        SingleInstanceManager.getInstance().release();
        logger.info("Application cleanup finished. Exiting normally.");
    }

    /**
     * Who would've thunk that bringing a window to the front would be so
     * platform-dependent and require all sorts of goofy hacks?
     */
    private void bringToFront() {
        logger.fine("MusicPlayer single instance: bringing main window to front.");
        final boolean isLinux = System.getProperty("os.name").toLowerCase().contains("linux");
        setState(JFrame.NORMAL); // unminimize if needed
        if (isLinux) {
            setAlwaysOnTop(true); // cheesy trick to make this work on linux
        }
        toFront();
        requestFocus();
        if (isLinux) {
            setAlwaysOnTop(false); // linux mint cinnamon seems to ignore toFront() unless we do this
        }
    }

    private MessageUtil getMessageUtil() {
        if (messageUtil == null) {
            messageUtil = new MessageUtil(getInstance(), logger);
        }
        return messageUtil;
    }

    /**
     * Invoked from ReloadUIAction when the UI is to reload itself -
     * we use this to enable/disable single instance mode depending on user selection.
     */
    @Override
    public void reloadUI() {
        // Update our keystrokes, if any of them changed:
        registerKeyStrokes(keyStrokeManager);

        boolean newValue = AppConfig.getInstance().isSingleInstanceEnabled();
        
        // If the user did not change the setting, do nothing
        if (newValue == isSingleInstanceModeEnabled) {
            return;
        }
        
        // Update our cached value:
        isSingleInstanceModeEnabled = newValue;
        
        // If single instance mode is now enabled, try to acquire the lock:
        if (newValue) {
            logger.info("Enabling single instance mode.");
            SingleInstanceManager instanceManager = SingleInstanceManager.getInstance();
            if (!instanceManager.tryAcquireLock(a -> MainWindow.getInstance().processStartArgs(a),
                                                Main.SINGLE_INSTANCE_PORT)) {
                // Another instance is already running, let's inform the user:
                getMessageUtil().error("Single Instance Mode",
                                       "Another instance of MusicPlayer is already running.\n" +
                                           "Unable to enable single instance mode.");
            }
        }

        // Otherwise, if single instance mode is now disabled, release the lock if we have it:
        else {
            logger.info("Disabling single instance mode.");
            SingleInstanceManager.getInstance().release();
        }
    }

    /**
     * Called when the AudioPanel state changes (IDLE, PLAYING, PAUSED).
     * When the panel goes to IDLE, we revert the window title to the default.
     * When the panel starts playing, we update the title in case audioLoaded() wasn't called.
     */
    @Override
    public void stateChanged(AudioPanel sourcePanel, AudioPanel.PanelState state) {
        if (state == AudioPanel.PanelState.IDLE) {
            metadataChanged(null);
        } else if (state == AudioPanel.PanelState.PLAYING) {
            // Update title when playing starts, in case track was already loaded
            currentMetadata = sourcePanel.getAudioData() != null ? sourcePanel.getAudioData().getMetadata() : null;
            updateTitleFromAudioData(sourcePanel);
        }
    }

    /**
     * Register our keystrokes with the given KeyStrokeManager.
     * This method is safe to invoke even if it has already been invoked.
     * <p>
     * Technical note: KeyStrokeManager allows us to expose keystrokes to the
     * user for customization. This method is wired up such that it is invoked
     * whenever the user hits OK on the application properties dialog.
     * Currently, we don't expose any keystrokes for user customization,
     * but if we ever do, they will automatically be re-registered here
     * when the user changes them.
     * </p>
     */
    public static void registerKeyStrokes(KeyStrokeManager keyStrokeManager) {
        keyStrokeManager.clear();

        // Register all the ones that are not configurable:
        keyStrokeManager.registerHandler(KeyStrokeManager.parseKeyStroke("Left"), new PrevAction());
        keyStrokeManager.registerHandler(KeyStrokeManager.parseKeyStroke("Right"), new NextAction());
        keyStrokeManager.registerHandler(KeyStrokeManager.parseKeyStroke("Space"), new PauseAction());
        keyStrokeManager.registerHandler(KeyStrokeManager.parseKeyStroke("V"), new FullScreenAction());
        keyStrokeManager.registerHandler(KeyStrokeManager.parseKeyStroke("Esc"), new FullScreenStopAction());
        keyStrokeManager.registerHandler(KeyStrokeManager.parseKeyStroke("Ctrl+P"), new SettingsAction());
        keyStrokeManager.registerHandler(KeyStrokeManager.parseKeyStroke("Ctrl+A"), new AboutAction());
        keyStrokeManager.registerHandler(KeyStrokeManager.parseKeyStroke("Ctrl+O"), new PlaylistOpenAction());
        keyStrokeManager.registerHandler(KeyStrokeManager.parseKeyStroke("Ctrl+Q"), new ExitAction());
        keyStrokeManager.registerHandler(KeyStrokeManager.parseKeyStroke("Ctrl+S"), new PlaylistSaveAction());
        keyStrokeManager.registerHandler(KeyStrokeManager.parseKeyStroke("Del"), new PlaylistRemoveOneAction());
        keyStrokeManager.registerHandler(KeyStrokeManager.parseKeyStroke("Ctrl+Del"), new PlaylistRemoveAllAction());

        // Now register whatever AppConfig knows about (this includes extension-supplied KeyStrokes, if any):
        for (KeyStrokeProperty prop : AppConfig.getInstance().getKeyStrokeProperties()) {
            // If there's no Action attached, or if there is no keystroke assigned to it, skip it:
            // (this is a valid scenario! the user can "un-map" a keystroke if they don't want one for a given action).
            if (prop.getAction() == null || prop.getKeyStroke() == null) {
                continue;
            }

            // Register it! This will automatically update the shortcut attached to any menu items as well:
            // Note that KeyStrokeManager can handle duplicate registrations, so it's not technically
            // a problem if one of these conflicts with one of ours above.
            keyStrokeManager.registerHandler(prop.getKeyStroke(), prop.getAction());
        }
    }

    /**
     * Updates the window title using the formatted metadata from our AudioPanel's audio data.
     */
    private void updateTitleFromAudioData(AudioPanel sourcePanel) {
        if (sourcePanel != null
                && sourcePanel.getAudioData() != null
                && sourcePanel.getAudioData().getMetadata() != null) {
            metadataChanged(sourcePanel.getAudioData().getMetadata());
        }
        else {
            metadataChanged(null);
        }
    }

    /**
     * Called when an audio clip is loaded into the AudioPanel.
     * We update the window title to show the formatted track metadata.
     */
    @Override
    public void audioLoaded(AudioPanel sourcePanel, VisualizationTrackInfo trackInfo) {
        currentMetadata = sourcePanel.getAudioData() != null ? sourcePanel.getAudioData().getMetadata() : null;
        updateTitleFromAudioData(sourcePanel);
    }

    /**
     * We need to respond when our current audio metadata changes, so that we
     * can keep the window title updated.
     *
     * @param newData AudioMetadata to use. Can be null, meaning no track is loaded.
     */
    private void metadataChanged(AudioMetadata newData) {
        if (newData == null) {
            setTitle(Version.FULL_NAME);
            return;
        }

        // If this is the same metadata we already have, update our title.
        // Otherwise, we'll just ignore this.
        if (newData.hasSameSourceFile(currentMetadata)) {
            setTitle(newData.getFormatted());
        }
    }
}
