package ca.corbett.musicplayer.ui;

import ca.corbett.musicplayer.AppConfig;
import ca.corbett.musicplayer.extensions.MusicPlayerExtensionManager;

import java.awt.Graphics2D;
import java.util.List;
import java.util.logging.Logger;

/**
 * Responsible for organizing the list of available Visualizer intances
 * and providing access to them.
 *
 * @author scorbo2
 * @since 2025-03-30
 */
public class VisualizationManager {

    private static final Logger logger = Logger.getLogger(VisualizationManager.class.getName());

    protected VisualizationManager() {
    }

    /**
     * Returns all Visualizers currently configured - this includes the built-in one
     * along with any registered by extensions.
     *
     * @return An array of all Visualizer instances that are available.
     */
    public static Visualizer[] getAll() {
        List<Visualizer> extensionVisualizers = MusicPlayerExtensionManager.getInstance().getCustomVisualizers();
        Visualizer[] all = new Visualizer[1 + extensionVisualizers.size()];
        all[0] = new StandardVisualizer();
        int index = 1;
        for (Visualizer visualizer : extensionVisualizers) {
            all[index++] = visualizer;
        }
        return all;
    }

    /**
     * Finds and returns the Visualizer by the given name, or the standard built-in Visualizer
     * if the named one does not exist.
     */
    public static Visualizer getVisualizer(String name) {
        for (Visualizer visualizer : getAll()) {
            if (visualizer.name.equals(name)) {
                return visualizer;
            }
        }

        logger.warning("Requested visualizer \"" + name + "\" not found; using STANDARD as default.");
        return new StandardVisualizer();
    }

    /**
     * Extensions can extend this class to implement a full-screen visualization showing
     * some kind of animation.
     */
    public static abstract class Visualizer {

        protected final String name;

        public Visualizer(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        /**
         * Return true if you're going to display stuff in the bottom third of the window and you
         * don't want the bouncing text overlay to enter that part of the screen.
         * Examples: lyrics sheet visualizer, commentary visualizer.
         * Most visualizers can keep the default implementation which returns false.
         *
         * @return True if you want to keep the bouncing text overlay box out of the bottom third of the screen.
         */
        public boolean reserveBottomGutter() {
            return false;
        }

        /**
         * Invoked when the Visualizer is started, to supply it with the width and height
         * of the target window and to allow it to prepare to start visualizing.
         * This will get invoked once before you start receiving renderFrame() calls, so you
         * can do some initialization here if you need to.
         *
         * @param width  The width of the display area.
         * @param height The height of the display area.
         */
        public abstract void initialize(int width, int height);

        /**
         * Invoked in the animation loop to render a single frame.
         *
         * @param g         The Graphics2D object you can use to render your visualization.
         * @param trackInfo Information about the currently playing track, if any.
         */
        public abstract void renderFrame(Graphics2D g, VisualizationTrackInfo trackInfo);

        /**
         * Invoked when your Visualizer is stopped. Do any cleanup you need to do here.
         */
        public abstract void stop();
    }

    /**
     * A very boring Visualizer which can work as our only built-in option.
     * Displays a solid blank screen and that's all you get.
     */
    public static class StandardVisualizer extends Visualizer {

        private int width;
        private int height;

        public StandardVisualizer() {
            super("Standard - blank screen");
        }

        @Override
        public void initialize(int width, int height) {
            this.width = width;
            this.height = height;
        }

        @Override
        public void renderFrame(Graphics2D g, VisualizationTrackInfo trackInfo) {
            g.setColor(AppConfig.getInstance().getAppTheme().getNormalBgColor());
            g.fillRect(0, 0, width, height);
        }

        @Override
        public void stop() {
        }
    }
}
