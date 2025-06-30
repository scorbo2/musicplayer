package ca.corbett.musicplayer.ui;

import ca.corbett.extras.image.ImagePanel;
import ca.corbett.musicplayer.AppConfig;
import ca.corbett.musicplayer.actions.ReloadUIAction;
import ca.corbett.musicplayer.extensions.MusicPlayerExtensionManager;

import javax.swing.JFrame;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
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

    public enum VisualizerRotation {
        NEVER("Never", 0),
        ONE("1 minute", 1),
        TWO("2 minutes", 2),
        FIVE("5 minutes", 5),
        TEN("10 minutes", 10),
        FIFTEEN("15 minutes", 15),
        THIRTY("30 minutes", 30);

        private final String label;
        private final int minutes;

        VisualizerRotation(String label, int minutes) {
            this.label = label;
            this.minutes = minutes;
        }

        @Override
        public String toString() {
            return label;
        }

        public int getMinutes() {
            return minutes;
        }
    }

    private volatile boolean running;
    private final AnimationSpeed animationSpeed;
    private VisualizationTrackInfo trackInfo;
    private VisualizationManager.Visualizer effectiveVisualizer;
    private List<VisualizationManager.Visualizer> visualizerRotation;
    private File currentSongFile;
    private int width;
    private int height;
    private boolean textOverlayEnabled;
    private boolean isFullScreen;
    private ImagePanel imagePanel;
    private JFrame visFrame;
    private volatile boolean isRenderingPaused;
    private boolean isFileTriggerActive;
    private int interruptedVisualizerIndex; // for file triggers

    /**
     * Creates a new VisualizationThread with values taken from AppConfig.
     */
    public VisualizationThread() {
        animationSpeed = AppConfig.getInstance().getVisualizationAnimationSpeed();
        running = false;
        isRenderingPaused = false;
        currentSongFile = null;
        textOverlayEnabled = AppConfig.getInstance().isVisualizerOverlayEnabled();
        effectiveVisualizer = null;
        visualizerRotation = new ArrayList<>();
        ReloadUIAction.getInstance().registerReloadable(this);
        width = 1920; // completely arbitrary default
        height = 1080; // caller will override this with actual values
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
     * Whenever the current track changes, we check to see if we need to swap out the
     * current visualizer for a different one. This can happen if "allow visualizer
     * override" is enabled in application settings.
     *
     * @param info     TrackInfo for the currently playing track, or null for nothing playing.
     */
    public void setTrackInfo(VisualizationTrackInfo info) {
        trackInfo = info;

        // If we're not running, we're done here:
        if (!isRunning()) {
            currentSongFile = null;
            return;
        }

        // If the track has changed, we may need to swap out our current visualizer or swap it back:
        File songFile = (info == null) ? null : info.getSourceFile();
        if (!Objects.equals(songFile, currentSongFile) && AppConfig.getInstance().isAllowVisualizerOverride()) {
            currentSongFile = songFile;

            // Check our visualizers to see if any of them want to override the current one,
            // based on the new song file:
            boolean wasSwapped = false;
            for (VisualizationManager.Visualizer visualizer : MusicPlayerExtensionManager.getInstance().getCustomVisualizers()) {
                if (visualizer == effectiveVisualizer || !visualizer.isSupportsFileTriggers()) {
                    continue;
                }
                if (visualizer.hasOverride(trackInfo)) {
                    if (effectiveVisualizer != null) {
                        pauseRendering();
                        effectiveVisualizer.stop();
                    }
                    interruptedVisualizerIndex = getCurrentVisualizerIndex();
                    effectiveVisualizer = visualizer;
                    effectiveVisualizer.initialize(width, height);
                    isFileTriggerActive = true;
                    wasSwapped = true;
                    logger.info("Swapping out default visualizer for " + ((effectiveVisualizer == null) ? "null" : effectiveVisualizer.getName()));
                    break; // note we pick the first visualizer that volunteers, so load order matters here
                }
            }

            // If no one volunteered, then see if we need to switch back to the user-selected one:
            // (i.e. the previous track had an override but this one doesn't):
            if (!wasSwapped && isFileTriggerActive) {
                if (interruptedVisualizerIndex == -1 || interruptedVisualizerIndex >= visualizerRotation.size()) {
                    interruptedVisualizerIndex = 0;
                }
                VisualizationManager.Visualizer defaultVisualizer = visualizerRotation.get(interruptedVisualizerIndex);
                if (effectiveVisualizer != defaultVisualizer) {
                    logger.info("Restoring default visualizer");
                    if (effectiveVisualizer != null) {
                        pauseRendering();
                        effectiveVisualizer.stop();
                    }
                    effectiveVisualizer = defaultVisualizer;
                    effectiveVisualizer.initialize(width, height);
                    isFileTriggerActive = false;
                }
            }

            isRenderingPaused = false;
        }
    }

    public void setFullScreen(boolean full) {
        isFullScreen = full;
    }

    public void setImagePanel(ImagePanel panel) {
        imagePanel = panel;
    }

    @Override
    public void reloadUI() {
        // TODO did I mean to do something in here? I can't remember why I implemented this interface here
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

    public void setSize(int w, int h) {
        width = w;
        height = h;
    }

    public void setVisFrame(JFrame frame) {
        visFrame = frame;
    }

    /**
     * Handles the animation loop until this thread is interrupted or terminated.
     */
    @Override
    public void run() {
        running = true;

        // Get a handle on the buffer strategy (created by VisualizationWindow):
        BufferStrategy strategy = visFrame.getBufferStrategy();

        // Kludge alert: multi-monitor support is wonky. Seems the last entry contains the actual resolution.
        //   (the first entry contains width*2 x height instead of width x height)
        // This is made worse if your two monitors have different resolutions.
        // For example 1680x1050 and 1920x1080. The "default device" will claim to have 1680+1920 = 3600 width.
        // This might just be the case on my ancient dell laptop running linux version who knows what
        //
        // UPDATE: it doesn't do this on my newer laptop. I think we can pass in the width and height.
        //int deviceCount = env.getScreenDevices().length;
        //width = env.getScreenDevices()[deviceCount - 1].getDisplayMode().getWidth();
        //height = env.getScreenDevices()[deviceCount - 1].getDisplayMode().getHeight();
        int textBoxY = height - ((int) (height / 3));

        // Create a double buffer for this GraphicsConfiguration:
        //BufferedImage dbuffer = conf.createCompatibleImage(width, height);
        logger.log(Level.INFO, "VisualizationThread created; rendering at {0}x{1}", new Object[]{width, height});

        // Initialize the currently selected visualizer from application settings.
        // If visualizer override is enabled, this may change on us as we go, but that's okay.
        // See setTrackInfo.
        effectiveVisualizer = AppConfig.getInstance().getVisualizer();
        effectiveVisualizer.initialize(width, height);

        // Populate the list of Visualizers to rotate:
        visualizerRotation.clear();
        boolean excludeStandard = AppConfig.getInstance().isExcludeBlankVisualizerFromRotation();
        for (VisualizationManager.Visualizer vis : VisualizationManager.getAll()) {
            if (excludeStandard && (vis instanceof VisualizationManager.StandardVisualizer)) {
                continue;
            }
            if (vis.isSupportsFileTriggers()) {
                continue;
            }
            visualizerRotation.add(vis);
        }
        long lastVisualizerRotationMS = System.currentTimeMillis();
        int visualizerRotationIntervalMS = AppConfig.getInstance().getVisualizerRotation().getMinutes() * 60 * 1000;
        boolean visualizerRotationEnabled = AppConfig.getInstance().getVisualizerRotation() != VisualizerRotation.NEVER;
        if (visualizerRotationEnabled && visualizerRotation.size() > 1) {
            logger.info("Will rotate visualizers every " + AppConfig.getInstance().getVisualizerRotation());
        }
        else {
            if (visualizerRotation.size() == 1) {
                logger.info("Visualizer rotation disabled as there's only one visualizer.");
            }
            else {
                logger.info("Visualizer rotation disabled in app settings.");
            }
        }

        // we want to re-render the text overlay every 1s or so (if it's enabled):
        Random rand = new Random();
        int overlayRenderCountdown = 0;
        int overlayX = rand.nextInt(width);
        int overlayY = rand.nextInt(height);
        int overlayDeltaX = rand.nextInt(10) > 5 ? 1 : -1;
        int overlayDeltaY = rand.nextInt(10) > 5 ? 1 : -1;
        BufferedImage textOverlay = null;
        VisualizationOverlay overlay = VisualizationOverlay.getInstance();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        // Animation loop:
        while (running) {
            long frameStartTime = System.currentTimeMillis();

            // MPLAY-55: there's a very intermittent and hard to reproduce NPE here where
            //           the strategy can sometimes be null.
            if (isFullScreen && strategy == null) {
                logger.log(Level.INFO, "MPLAY-55 NPE avoidance code triggered! No cause for alarm unless this message repeats.");
                strategy = visFrame.getBufferStrategy(); // try again, no idea why sometimes 1st time fails
            }

            // Animate something
            Graphics2D g = isFullScreen ? (Graphics2D) strategy.getDrawGraphics() : image.createGraphics();
            if (isRenderingPaused) {
                g.setColor(Color.BLACK);
                g.fillRect(0, 0, width, height); // please stand by
            } else {
                effectiveVisualizer.renderFrame(g, trackInfo);
            }

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
                    if (effectiveVisualizer.reserveBottomGutter()) {
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

            if (isFullScreen && strategy.contentsLost()) {
                logger.severe("Buffer strategy contents lost!");
            }
            g.dispose();

            if (isFullScreen) {
                strategy.show();
            } else {
                imagePanel.setImage(image);
            }

            // There's a weird bug either in the JRE or possibly in the OS where lack of regular
            // mouse movement over the window will cause the priority of the thread to get ramped
            // down quite noticeably. This call to sync() magically stops that from happening.
            Toolkit.getDefaultToolkit().sync();

            try {
                // If we've exceeded our animationDelay time then there's no need to sleep:
                // Otherwise, sleep for the remainder of our animationDelay time between frames:
                long remainingTime = (frameStartTime + animationSpeed.delayMs) - System.currentTimeMillis();
                if (remainingTime > 0) {
                    Thread.sleep(remainingTime);
                }
            } catch (InterruptedException ignored) {
                stop();
            }

            // Handle visualizer rotation if enabled:
            if (visualizerRotationEnabled && visualizerRotation.size() > 1 && !isFileTriggerActive) {
                long now = System.currentTimeMillis();
                if ((now - lastVisualizerRotationMS) > visualizerRotationIntervalMS) {
                    lastVisualizerRotationMS = now;
                    logger.info("Rotating to next visualizer");
                    int index = getCurrentVisualizerIndex() + 1;
                    if (index >= visualizerRotation.size()) {
                        index = 0;
                    }
                    effectiveVisualizer.stop();
                    effectiveVisualizer = visualizerRotation.get(index);
                    effectiveVisualizer.initialize(width, height);
                }
            }
        }

        effectiveVisualizer.stop();
        effectiveVisualizer = null;
    }

    /**
     * Returns the index of the effective visualizer, or -1 if not found in the rotation.
     */
    private int getCurrentVisualizerIndex() {
        int index = -1;
        for (int i = 0; i < visualizerRotation.size(); i++) {
            if (visualizerRotation.get(i) == effectiveVisualizer) {
                index = i;
                break;
            }
        }
        return index;
    }

    /**
     * Invoked internally if we need to temporarily pause rendering, for example to swap
     * out the current visualizer for a different one. Calling code must set
     * isRenderingPaused back to false once the new visualizer is in place!
     */
    private void pauseRendering() {
        // We don't want to stop() and initialize() visualizers that are in the process of rendering
        // something, because bad things like NPEs will happen. So, we set a flag here to stop sending
        // renderFrame() messages to whatever visualizer is active, then wait a short amount of time
        // to make sure that any renders currently in progress finish, before we proceed:
        isRenderingPaused = true;
        try {
            Thread.sleep(animationSpeed.delayMs * 2L); // sit out a frame or two
        }
        catch (InterruptedException ignored) {
        }
    }
}
