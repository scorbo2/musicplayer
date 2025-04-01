package ca.corbett.musicplayer.ui;

import ca.corbett.extras.image.ImagePanel;
import ca.corbett.extras.image.ImagePanelConfig;
import ca.corbett.musicplayer.AppConfig;
import ca.corbett.musicplayer.Version;
import ca.corbett.musicplayer.actions.ReloadUIAction;

import javax.swing.JFrame;
import javax.swing.JRootPane;
import java.awt.BorderLayout;
import java.awt.DisplayMode;
import java.awt.Graphics;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import java.awt.image.BufferedImage;
import java.io.File;
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
public class VisualizationWindow extends JFrame implements UIReloadable {

    private static final Logger logger = Logger.getLogger(VisualizationWindow.class.getName());

    private ImagePanel imagePanel;

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
    private final VisualizationThread thread;
    private int monitorCount;

    private VisualizationWindow() {
        super(Version.NAME + " visualizer");

        thread = new VisualizationThread();
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        initializeDisplay();

        // Turn off decorations on this window (otherwise you get an ugly title bar/window controls):
        setUndecorated(isFullscreenSupported);
        if (isFullscreenSupported) {
            getRootPane().setWindowDecorationStyle(JRootPane.NONE);
        } else {
            setLayout(new BorderLayout());
            imagePanel = new ImagePanel(ImagePanelConfig.createSimpleReadOnlyProperties());
            add(imagePanel, BorderLayout.CENTER);
            thread.setImagePanel(imagePanel);
        }
    }

    @Override
    public void reloadUI() {
        initializeDisplay();
    }

    public void initializeDisplay() {
        DISPLAY preferredDisplay = AppConfig.getInstance().getPreferredVisualizationDisplay();
        setIconImage(MainWindow.getInstance().getIconImage());
        monitorCount = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices().length;
        if (preferredDisplay.monitorIndex >= monitorCount) {
            logger.warning("Visualizer: preferred display is not available; reverting to primary display.");
            preferredDisplay = DISPLAY.PRIMARY;
        } else {
            logger.log(Level.INFO, "Starting up visualizer on display {0}", preferredDisplay.monitorIndex);
        }
        GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
        graphicsDevice = env.getScreenDevices()[preferredDisplay.monitorIndex];
        DisplayMode displayMode = graphicsDevice.getDisplayMode();
        setSize(displayMode.getWidth(), displayMode.getHeight()); // apparently initial size matters
        isFullscreenSupported = graphicsDevice.isFullScreenSupported();
        logger.log(Level.INFO, "isFullscreenSupported: {0}", isFullscreenSupported);
        if (!isFullscreenSupported) {
            logger.warning("Full screen mode is not supported! Visualization will unfortunately not work very well :(");
        }

        // We don't need or want AWT paint messages as we will handle our own display:
        setIgnoreRepaint(isFullscreenSupported);
        getContentPane().setIgnoreRepaint(isFullscreenSupported);

        // Also hide the mouse pointer:
        if (isFullscreenSupported) {
            getContentPane().setCursor(Toolkit.getDefaultToolkit().createCustomCursor(
                    new BufferedImage(3, 3, BufferedImage.TYPE_INT_ARGB), new Point(0, 0), "null"));
        }
    }

    public static VisualizationWindow getInstance() {
        if (instance == null) {
            instance = new VisualizationWindow();
            ReloadUIAction.getInstance().registerReloadable(instance);

            // Add the key listener once:
            instance.addKeyListener(new KeyAdapter() {
                @Override
                public void keyReleased(KeyEvent e) {
                    switch (e.getKeyCode()) {
                        // Left or Up arrow for "previous song":
                        case KeyEvent.VK_LEFT:
                        case KeyEvent.VK_UP:
                            AudioPanel.getInstance().prev();
                            break;

                        // Right or down arrow for "next song":
                        case KeyEvent.VK_RIGHT:
                        case KeyEvent.VK_DOWN:
                            AudioPanel.getInstance().next();
                            break;

                        // Space for "pause":
                        case KeyEvent.VK_SPACE:
                            if (AudioPanel.getInstance().getPanelState() == AudioPanel.PanelState.PLAYING ||
                                    AudioPanel.getInstance().getPanelState() == AudioPanel.PanelState.PLAYING) {
                                AudioPanel.getInstance().pause();
                            } else {
                                AudioPanel.getInstance().play();
                            }
                            break;

                        // I for track info on/off:
                        case KeyEvent.VK_I:
                            instance.thread.setTextOverlayEnabled(!instance.thread.isTextOverlayEnabled());
                            break;

                        case KeyEvent.VK_ESCAPE:
                            //instance.setVisible(false);
                            instance.stopFullScreen();
                            break;
                    }
                }
            });

            // Add the focus listener once:
            final int deviceCount = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices().length;
            instance.addFocusListener(new FocusListener() {
                @Override
                public void focusGained(FocusEvent arg0) {
                    instance.setAlwaysOnTop(true);
                }

                @Override
                public void focusLost(FocusEvent arg0) {
                    // If there's only one monitor, stop full screen mode when focus is lost.
                    // This almost certainly means someone alt+tabbed away from the visualizer,
                    // and so we'll just kill it.
                    // If there's more than one monitor, ignore this event as it's possible
                    // to leave the visualizer up on monitor 2 while doing stuff on monitor 1.
                    if (instance.isFullscreenSupported && deviceCount == 1) {
                        instance.stopFullScreen();
                    }
                }

            });

            // add a window state listener:
            instance.addWindowStateListener(new WindowStateListener() {
                @Override
                public void windowStateChanged(WindowEvent e) {
                    logger.log(Level.INFO, "window state changed: {0} to {1}", new Object[]{e.getOldState(), e.getNewState()});
                }
            });
        }

        return instance;
    }

    /**
     * Updates the current track info.
     *
     * @param info TrackInfo for the currently playing song, or null.
     * @param file A File object representing the currently playing song, or null.
     */
    public void setTrackInfo(VisualizationTrackInfo info, File file) {
        thread.setTrackInfo(info, file);
    }

    public void goFullScreen() {
        if (!thread.isRunning()) {
            thread.setFullScreen(isFullscreenSupported);
            if (isFullscreenSupported) {
                GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
                graphicsDevice = env.getScreenDevices()[AppConfig.getInstance().getPreferredVisualizationDisplay().monitorIndex];
                DisplayMode displayMode = graphicsDevice.getDisplayMode();
                setSize(displayMode.getWidth(), displayMode.getHeight()); // apparently initial size matters
                thread.setSize(displayMode.getWidth(), displayMode.getHeight());
                graphicsDevice.setFullScreenWindow(instance);
                if (getBufferStrategy() == null) {
                    createBufferStrategy(2);
                }
            } else {
                setVisible(true);
            }
            new Thread(thread).start();
            logger.log(Level.INFO, "Starting animation thread, window is {0}x{1}.", new Object[]{getContentPane().getWidth(), getContentPane().getHeight()});
        } else {
            stopFullScreen();
        }
    }

    public void stopFullScreen() {
        logger.info("Stopping animation thread.");
        thread.stop();
        if (isFullscreenSupported) {
            graphicsDevice.setFullScreenWindow(null);
            instance.setAlwaysOnTop(false);
            if (getBufferStrategy() != null) {
                getBufferStrategy().dispose();
            }
        }
        setVisible(false);
    }

    /**
     * Even though we setIgnoreRepaint on both this window and the content pane, apparently
     * we still need to override this to avoid calling super.paint(), else we get all kinds
     * of screen tearing and flickering in the animation.
     *
     * @param g Ignored.
     */
    @Override
    public void paint(Graphics g) {
        if (!isFullscreenSupported) {
            super.paint(g);
        }
    }
}
