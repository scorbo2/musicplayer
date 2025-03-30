package ca.corbett.musicplayer.ui;

import ca.corbett.musicplayer.AppConfig;
import ca.corbett.musicplayer.actions.ReloadUIAction;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Objects;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Worker thread for the visualization window.
 *
 * @author scorbo2
 * @since 2017-12-05
 */
public class VisualizationThread implements Runnable, UIReloadable {

    private static final Logger logger = Logger.getLogger(VisualizationThread.class.getName());

    public enum AnimationSpeed {
        LOW("Low", 90),
        MEDIUM("Medium", 45),
        HIGH("High", 25);

        public final String label;
        public final int delayMs;

        AnimationSpeed(String label, int delay) {
            this.label = label;
            this.delayMs = delay;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private volatile boolean running;
    private final AnimationSpeed animationSpeed;
    private VisualizationTrackInfo trackInfo;
    private File currentSongFile;
    private int width;
    private int height;
    private boolean textOverlayEnabled;

    /**
     * Creates a new VisualizationThread with values taken from AppConfig.
     */
    public VisualizationThread() {
        animationSpeed = AppConfig.getInstance().getVisualizationAnimationSpeed();
        running = false;
        textOverlayEnabled = AppConfig.getInstance().isVisualizerOverlayEnabled();
        ReloadUIAction.getInstance().registerReloadable(this);
    }

    /**
     * Reports whether or not the text overlay is currently showing.
     *
     * @return Whether the text overlay is currently showing.
     */
    public boolean isTextOverlayEnabled() {
        return textOverlayEnabled;
    }

    /**
     * Provides a way to toggle the text overlay on/off. This will override the
     * setting from the preferences dialog without making the change permanent.
     * Really it's here so that we can have a hotkey in the VisualizationWindow
     * to temporarily turn it on or off without leaving the visualization.
     *
     * @param enabled Whether to toggle the text overlay on or off.
     */
    public void setTextOverlayEnabled(boolean enabled) {
        textOverlayEnabled = enabled;
    }

    /**
     * Updates the current track info. Pass null to indicate nothing is currently playing.
     *
     * @param info     TrackInfo for the currently playing track, or null for nothing playing.
     * @param songFile A File object for the currently playing track, or null for nothing playing.
     */
    public void setTrackInfo(VisualizationTrackInfo info, File songFile) {
        trackInfo = info;

        // New track?
        if (!Objects.equals(songFile, currentSongFile)) {
            currentSongFile = songFile;

            // TODO allow extensions to scan the current song for any triggers that would activate their visualizer
            //      example: a lyrics sheet exists in this directory
        }
    }

    @Override
    public void reloadUI() {

    }

    /**
     * Reports whether this thread is currently running (animating) or not. You can use
     * stop() to stop and kill the thread.
     *
     * @return Whether the thread is currently animating.
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Stops the current animation and terminates this thread.
     */
    public void stop() {
        running = false;
    }

    /**
     * Handles the animation loop until this thread is interrupted or terminated.
     */
    @Override
    public void run() {
        running = true;

        // Get a handle on the buffer strategy (created by VisualizationWindow):
        BufferStrategy strategy = VisualizationWindow.getInstance().getBufferStrategy();

        // These are inaccurate on linux due to wonky behaviour from some desktop environments.
        //int width = VisualizationWindow.getInstance().getWidth();
        //int height = VisualizationWindow.getInstance().getHeight();
        // Get a handle on the current graphics environment:
        GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();

        // Kludge alert: multi-monitor support is wonky. Seems the last entry contains the actual resolution.
        //   (the first entry contains width*2 x height instead of width x height)
        // This is made worse if your two monitors have different resolutions.
        // For example 1680x1050 and 1920x1080. The "default device" will claim to have 1680+1920 = 3600 width.
        // This might just be the case on my ancient dell laptop running linux version who knows what
        // TODO verify that this is still true on modern distros/hardware - can we remove this kludge?
        int deviceCount = env.getScreenDevices().length;
        width = env.getScreenDevices()[deviceCount - 1].getDisplayMode().getWidth();
        height = env.getScreenDevices()[deviceCount - 1].getDisplayMode().getHeight();
        int textBoxY = height - ((int) (height / 3));

        // Create a double buffer for this GraphicsConfiguration:
        //BufferedImage dbuffer = conf.createCompatibleImage(width, height);
        logger.log(Level.INFO, "VisualizationThread created; rendering at {0}x{1}", new Object[]{width, height});

        // Get a handle on the selected Visualizer:
        VisualizationManager.Visualizer visualizer = AppConfig.getInstance().getVisualizer();
        visualizer.initialize(width, height);
        int visualizerIndex = 0;
        long visualizerRuntime = 0L;

        // we want to re-render the text overlay every 1s or so (if it's enabled):
        Random rand = new Random();
        int overlayRenderCountdown = 0;
        //int overlayMovementCountdown = 1000; // Only move it every thousandth frame
        int overlayX = rand.nextInt(width);
        int overlayY = rand.nextInt(height);
        int overlayDeltaX = rand.nextInt(10) > 5 ? 1 : -1;
        int overlayDeltaY = rand.nextInt(10) > 5 ? 1 : -1;
        BufferedImage textOverlay = null;
        VisualizationOverlay overlay = VisualizationOverlay.getInstance();

        // Animation loop:
        while (running) {
            long frameStartTime = System.currentTimeMillis();

            // MPLAY-55: there's a very intermittent and hard to reproduce NPE here where
            //           the strategy can sometimes be null.
            if (strategy != null) {
                // Animate something
                Graphics2D g = (Graphics2D) strategy.getDrawGraphics();
                visualizer.renderFrame(g, trackInfo);

                // Draw a "paused" symbol in the center if the media player is paused:
                if (AudioPanel.getInstance().getPanelState() == AudioPanel.PanelState.PAUSED) {
                    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.75f));
                    g.setColor(Color.LIGHT_GRAY);
                    int centerX = (int) (width / 2d);
                    int centerY = (int) (height / 2d);
                    g.fillRect(centerX - 75, centerY - 80, 50, 160);
                    g.fillRect(centerX + 25, centerY - 80, 50, 160);
                    g.setComposite(AlphaComposite.Clear);
                }

                // Render the text overlay if needed:
                if (textOverlayEnabled) {
                    overlayRenderCountdown -= animationSpeed.delayMs;
                    if (overlayRenderCountdown <= 0) {
                        overlayRenderCountdown = 1000; // 1s
                        overlay.setTrackInfo(trackInfo);
                        textOverlay = overlay.render();
                    }

                    if (textOverlay != null) {
                        int overlayBottom = height;
                        if (visualizer.reserveBottomGutter()) {
                            overlayBottom = textBoxY;
                        }
                        overlayX += overlayDeltaX;
                        if (overlayX > (width - textOverlay.getWidth())) {
                            overlayX = width - textOverlay.getWidth();
                            overlayDeltaX = -overlayDeltaX;
                        }
                        if (overlayX < 0) {
                            overlayX = 0;
                            overlayDeltaX = -overlayDeltaX;
                        }
                        overlayY += overlayDeltaY;
                        if (overlayY > (overlayBottom - textOverlay.getHeight())) {
                            overlayY = overlayBottom - textOverlay.getHeight();
                            overlayDeltaY = -overlayDeltaY;
                        }
                        if (overlayY < 0) {
                            overlayY = 0;
                            overlayDeltaY = -overlayDeltaY;
                        }

                        // Display transparently if needed:
                        float opacity = overlay.getOpacity();
                        if (opacity < 1.0f) {
                            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
                        }

                        g.drawImage(textOverlay, overlayX, overlayY, null);
                    }
                }

                if (strategy.contentsLost()) {
                    logger.severe("Buffer strategry contents lost!");
                }
                strategy.show();

                // There's a weird bug either in the JRE or possibly in the OS where lack of regular
                // mouse movement over the window will cause the priority of the thread to get ramped
                // down quite noticeably. This call to sync() magically stops that from happening.
                Toolkit.getDefaultToolkit().sync();
            } else {
                logger.log(Level.INFO, "MPLAY-55 NPE avoidance code triggered! No cause for alarm unless this message repeats.");
                strategy = VisualizationWindow.getInstance().getBufferStrategy(); // try again, no idea why sometimes 1st time fails
            }

            try {
                // If we've exceed our animationDelay time then there's no need to sleep:
                // Otherwise, sleep for the remainder of our animationDelay time between frames:
                long remainingTime = (frameStartTime + animationSpeed.delayMs) - System.currentTimeMillis();
                if (remainingTime > 0) {
                    Thread.sleep(remainingTime);
                }
            } catch (InterruptedException ignored) {
                stop();
            }
        }

        visualizer.stop();
    }
}
