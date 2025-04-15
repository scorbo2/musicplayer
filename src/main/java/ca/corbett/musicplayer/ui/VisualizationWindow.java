package ca.corbett.musicplayer.ui;

import ca.corbett.extras.image.ImagePanel;
import ca.corbett.extras.image.ImagePanelConfig;
import ca.corbett.musicplayer.AppConfig;
import ca.corbett.musicplayer.Version;
import ca.corbett.musicplayer.actions.ReloadUIAction;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.DisplayMode;
import java.awt.Graphics;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A full screen window for showing some kind of visualization while music plays.
 * Makes use of the VisualizationThread to handle animation of stuff.
 * The actual visualizations are mostly handle via extension classes.
 *
 * @author scorbo2
 * @since 2017-12-05
 */
public class VisualizationWindow implements UIReloadable {

    private static final Logger logger = Logger.getLogger(VisualizationWindow.class.getName());

    public enum DISPLAY {
        PRIMARY("Primary", 0),
        SECONDARY("Secondary", 1);

        public final int monitorIndex;
        public final String label;

        DISPLAY(String label, int index) {
            this.label = label;
            this.monitorIndex = index;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private GraphicsDevice graphicsDevice;
    private boolean isFullscreenSupported;
    private static VisualizationWindow instance;
    private final VisualizationThread thread = new VisualizationThread();
    private int monitorCount;
    private InactivityListener inactivityListener;
    private JFrame visFrame = null;

    private VisualizationWindow() {
        initializeDisplay();
        logger.log(Level.INFO, "isFullscreenSupported: {0}", isFullscreenSupported);
        if (!isFullscreenSupported) {
            logger.warning("Full screen mode is not supported! Visualization will unfortunately not work very well :(");
        }
    }

    @Override
    public void reloadUI() {
        initializeDisplay();
    }

    /**
     * Prepares for visualization on whichever monitor has been selected in application preferences,
     * assuming that monitor is available. Makes a best attempt to set up visualization, but will
     * fall back to a windowed mode if something goes wrong. This method is safe to call multiple
     * times, and in fact the intention here is that you can re-initialize if the user chooses a
     * different monitor in application settings (of course it requires a restart of visualization,
     * but not a restart of the application).
     */
    public void initializeDisplay() {
        DISPLAY preferredDisplay = AppConfig.getInstance().getPreferredVisualizationDisplay();

        monitorCount = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices().length;
        if (preferredDisplay.monitorIndex >= monitorCount) {
            logger.warning("Visualizer: preferred display is not available; reverting to primary display.");
            preferredDisplay = DISPLAY.PRIMARY;
        } else {
            logger.log(Level.INFO, "Visualizer initialized on display {0}", preferredDisplay.monitorIndex);
        }
        GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
        graphicsDevice = env.getScreenDevices()[preferredDisplay.monitorIndex];
        isFullscreenSupported = graphicsDevice.isFullScreenSupported();
    }

    public static VisualizationWindow getInstance() {
        if (instance == null) {
            instance = new VisualizationWindow();
            ReloadUIAction.getInstance().registerReloadable(instance);
        }

        return instance;
    }

    /**
     * Updates the current track info.
     *
     * @param info TrackInfo for the currently playing song, or null.
     */
    public void setTrackInfo(VisualizationTrackInfo info) {
        thread.setTrackInfo(info);
    }

    /**
     * Starts fullscreen visualization mode. If fullscreen is not supported, we'll make a best effort
     * to show a resizable visualization window, but performance will be terrible and this is not
     * recommended. If fullscreen is supported, we'll use whichever monitor you've picked in
     * application config (assuming you have more than one) and go fullscreen on that monitor.
     * While in fullscreen mode, we will make efforts to prevent the screensaver from clicking on
     * (but this is not guaranteed as we don't have much low-level control over the OS in Java by design).
     */
    public void goFullScreen() {
        if (inactivityListener != null) {
            inactivityListener.stop();
            inactivityListener = null;
        }
        if (thread.isRunning()) {
            stopFullScreen();
            return;
        }

        thread.setFullScreen(isFullscreenSupported);
        visFrame = buildVisualizationWindow();
        thread.setVisFrame(visFrame);
        if (isFullscreenSupported) {
            GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
            graphicsDevice = env.getScreenDevices()[AppConfig.getInstance().getPreferredVisualizationDisplay().monitorIndex];
            DisplayMode displayMode = graphicsDevice.getDisplayMode();
            thread.setSize(displayMode.getWidth(), displayMode.getHeight());
            graphicsDevice.setFullScreenWindow(visFrame);
            visFrame.createBufferStrategy(2);
        } else {
            visFrame.setVisible(true);
        }
        logger.info("Starting visualization thread");
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(100); // just hang on a tick, window still initializing
                    new Thread(thread).start();
                }
                catch (InterruptedException ignored) { }
            }
        });
    }

    /**
     * Exits fullscreen mode and stops visualization, if we were running it.
     * Otherwise, does nothing. Our window is hidden, but not destroyed,
     * as we can re-use it later if fullscreen mode is re-initiated.
     */
    public void stopFullScreen() {
        if (thread.isRunning()) {
            if (inactivityListener != null) {
                inactivityListener.stop();
                inactivityListener = null;
            }

            logger.info("Stopping visualization thread.");
            thread.stop();
            if (isFullscreenSupported) {
                graphicsDevice.setFullScreenWindow(null);
            }
        }

        if (visFrame != null) {
            visFrame.dispose();
            visFrame = null;
        }
        thread.setVisFrame(null);
    }

    /**
     * This is temp code until I can find out why the visualization window very occasionally fails
     * to load. There's some kind of rare edge case condition that causes the visualization window
     * to come up completely blank. You have to close it and restart it to get it to work, and I'm
     * not sure why. Hit X to dump this debug information to the log, maybe it'll be useful.
     */
    public void debugDump() {
        logger.info("Visualization debug dump begins");
        logger.info("Thread running: " + thread.isRunning());
        logger.info(
            "Display mode: " + graphicsDevice.getDisplayMode().getWidth()
                + "x"
                + graphicsDevice.getDisplayMode().getHeight()
                + " (bit depth:"
                + graphicsDevice.getDisplayMode().getBitDepth()
                + ", refreshRate:"
                + graphicsDevice.getDisplayMode().getRefreshRate()
                + ")");
        thread.debugDump();
        logger.info("Debug dump complete.");
    }

    /**
     * Invoked internally to build the actual visualization window and return it.
     * We used to pre-create one instance of this and re-use it, but there are some
     * interesting scenarios there, particularly if the user goes into settings and
     * changes the preferred display. So, I took a nuke-and-pave approach instead,
     * and rebuild the window with fresh settings each time we start visualization.
     * <p>
     * If fullscreen mode is supported on this device, the returned window
     * will be one that is ready for fullscreen mode (i.e. it will be undecorated
     * and will be configured to ignore the usual repaint events). We will also
     * make a best effort here to prevent the system screensaver from clicking on
     * while the fullscreen window is up. No guarantees on that front.
     * </p>
     * <p>
     * If fullscreen mode is not supported on this device, you'll get a regular
     * JFrame which will have absolutely terrible performance. But, eh, it's
     * better than nothing, I guess.
     * </p>
     *
     * @return A window ready for visualization.
     */
    private JFrame buildVisualizationWindow() {
        JFrame window = new JFrame(Version.NAME + " visualizer") {
            @Override
            public void paint(Graphics g) {
                // Even though we setIgnoreRepaint, apparently we still need to prevent
                // calling super.paint() if we're in full screen mode, or we can get all
                // kinds of screen tearing and flickering.
                if (!isFullscreenSupported) {
                    super.paint(g);
                }
            }
        };
        window.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        window.setIconImage(MainWindow.loadIconResource("/ca/corbett/musicplayer/images/logo.png", 64, 64));
        DisplayMode displayMode = graphicsDevice.getDisplayMode();
        window.setSize(displayMode.getWidth(), displayMode.getHeight()); // apparently initial size matters
        KeyboardManager.addGlobalKeyListener(window);

        if (isFullscreenSupported) {
            // We don't need or want AWT paint messages as we will handle our own display:
            window.setIgnoreRepaint(isFullscreenSupported);
            window.getContentPane().setIgnoreRepaint(isFullscreenSupported);

            // Also hide the mouse pointer:
            window.getContentPane().setCursor(Toolkit.getDefaultToolkit().createCustomCursor(
                    new BufferedImage(3, 3, BufferedImage.TYPE_INT_ARGB), new Point(0, 0), "null"));

            window.setUndecorated(true); // otherwise you get an ugly title bar/window controls
            window.getRootPane().setWindowDecorationStyle(JRootPane.NONE);
            if (inactivityListener != null) {
                inactivityListener.stop();
            }
            if (AppConfig.getInstance().isVisualizerScreensaverPreventionEnabled()) {
                try {
                    Robot robot = new Robot();
                    inactivityListener = new InactivityListener(window, new AbstractAction() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            // jiggle mouse, mash keyboard, whatever, just stop the screensaver from clicking on:
                            robot.keyPress(KeyEvent.VK_SHIFT);
                            robot.keyRelease(KeyEvent.VK_SHIFT);
                            robot.mouseMove(1, 1);
                        }
                    });
                    inactivityListener.setRepeats(true);
                    inactivityListener.start();
                } catch (AWTException ignored) {
                    logger.warning("Visualizer: Robot is unsupported. Can't disable screensaver during visualization :(");
                }
            }

        } else {
            window.setLayout(new BorderLayout());
            ImagePanel imagePanel = new ImagePanel(ImagePanelConfig.createSimpleReadOnlyProperties());
            window.add(imagePanel, BorderLayout.CENTER);
            thread.setImagePanel(imagePanel);
        }

        // Add the key listener once:
        // NOTE this is in addition to the global KeyboardManager, not instead of it:
        window.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                switch (e.getKeyCode()) {
                    // I for track info on/off:
                    case KeyEvent.VK_I:
                        thread.setTextOverlayEnabled(!thread.isTextOverlayEnabled());
                        break;

                    case KeyEvent.VK_ESCAPE:
                        stopFullScreen();
                        break;
                }
            }
        });

        // Add the focus listener once:
        final int deviceCount = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices().length;
        window.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent arg0) {
                window.setAlwaysOnTop(true);
            }

            @Override
            public void focusLost(FocusEvent arg0) {
                // If there's only one monitor, stop full screen mode when focus is lost.
                // This almost certainly means someone alt+tabbed away from the visualizer,
                // and so we'll just kill it.
                // If there's more than one monitor, ignore this event as it's possible
                // to leave the visualizer up on monitor 2 while doing stuff on monitor 1.
                if (isFullscreenSupported && deviceCount == 1 && AppConfig.getInstance().isStopVisualizerOnFocusLost()) {
                    stopFullScreen();
                }
            }
        });

        return window;
    }
}
