package ca.corbett.musicplayer.ui;

import ca.corbett.extras.gradient.GradientConfig;
import ca.corbett.extras.gradient.GradientUtil;
import ca.corbett.extras.image.ImageTextUtil;
import ca.corbett.musicplayer.AppConfig;
import ca.corbett.musicplayer.actions.ReloadUIAction;
import ca.corbett.musicplayer.extensions.MusicPlayerExtensionManager;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Whenever the application is idle (not playing or in a paused state), we
 * can show a little animation in the waveform panel for something neat to
 * look at while we wait for the user to load a track.
 * <p>
 * Extensions can supply additional idle animations by extending
 * the Animation class to implement whatever they want. The user
 * can choose between these animations in AppConfig.
 * </p>
 *
 * @author scorbo2
 * @since 2025-03-23
 */
public class AudioPanelIdleAnimation implements UIReloadable {

    protected static AudioPanelIdleAnimation instance;
    protected Animation runningThread;

    protected AudioPanelIdleAnimation() {

    }

    public static AudioPanelIdleAnimation getInstance() {
        if (instance == null) {
            instance = new AudioPanelIdleAnimation();
            ReloadUIAction.getInstance().registerReloadable(instance);
        }
        return instance;
    }

    /**
     * Looks up and returns the Animation with the given name, or
     * returns a safe default value if the given name is not found.
     *
     * @param name The name of the Animation to find.
     * @return An Animation instance (guaranteed not null)
     */
    public Animation get(String name) {
        Animation[] animations = getAll();
        for (Animation animation : animations) {
            if (animation.getName() != null && animation.getName().equals(name)) {
                return animation;
            }
        }

        // Fallback default in case of garbage input:
        return new RollingColorWaves();
    }

    public Animation[] getAll() {
        List<AudioPanelIdleAnimation.Animation> extensionAnimations = MusicPlayerExtensionManager.getInstance().getCustomIdleAnimations();
        Animation[] animations = new Animation[2 + extensionAnimations.size()];
        animations[0] = new RollingColorWaves();
        animations[1] = new PlainBackground();
        int index = 2;
        for (Animation animation : extensionAnimations) {
            animations[index++] = animation;
        }
        return animations;
    }

    public void go() {
        if (runningThread != null) {
            return;
        }
        runningThread = AppConfig.getInstance().getIdleAnimation();
        new Thread(runningThread).start();
    }

    public void stop() {
        if (runningThread == null) {
            return;
        }
        runningThread.stop();
        runningThread = null;
    }

    /**
     * Overridden here so we can restart our animation if one is in progress.
     * Most animations use the current application theme to get their colors.
     * So, if the current theme changes, we need to reload the animation.
     * Does nothing if an animation is not currently running.
     */
    @Override
    public void reloadUI() {
        if (runningThread != null) {
            stop();
            go();
        }
    }

    /**
     * Extensions can extend this class to provide a custom idle animation - this animation
     * will be available for the user to select in AppConfig.
     * <p>
     * <b>Important!</b> - don't reference AppConfig in your constructor! It's still
     * initializing at the time your constructor will be invoked. If you need
     * to retrieve the current theme from AppConfig, do it in your run() method instead.
     * Otherwise you will find a stack overflow at runtime.
     * </p>
     */
    public static abstract class Animation implements Runnable {
        protected final String name;

        public Animation(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public abstract void stop();
    }

    /**
     * A very simple implementation of Animation which just shows a plain
     * color background with "ready" written on it. The background and foreground
     * colors are taken from the current app theme.
     */
    protected static class PlainBackground extends Animation {
        private volatile boolean isRunning;

        public PlainBackground() {
            super("Plain solid color");
        }

        @Override
        public void stop() {
            isRunning = false;
        }

        @Override
        public void run() {
            final int WIDTH = 400;
            final int HEIGHT = 200;
            isRunning = true;
            while (isRunning) {
                BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics = image.createGraphics();
                graphics.setColor(AppConfig.getInstance().getAppTheme().getNormalBgColor());
                graphics.fillRect(0, 0, WIDTH, HEIGHT);
                graphics.dispose();

                ImageTextUtil.drawText(image, "R E A D Y", 100, new Font(Font.MONOSPACED, Font.PLAIN, 12), ImageTextUtil.TextAlign.CENTER, Color.BLUE, 0f, AppConfig.getInstance().getAppTheme().getNormalFgColor(), null, new Rectangle(60, 20, WIDTH - 120, 100));
                AudioPanel.getInstance().setIdleImage(image);

                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    /**
     * An example Animation implementation that shows a slowly rolling color gradient
     * with the word "ready" printed on it. The colors to use are taken from the
     * current app theme.
     */
    protected static class RollingColorWaves extends Animation {

        public static final int WIDTH = 400;
        public static final int HEIGHT = AudioPanel.WAVEFORM_HEIGHT;
        private volatile boolean isRunning;
        int x = 0;

        public RollingColorWaves() {
            super("Rolling color wave");
        }

        @Override
        public void stop() {
            isRunning = false;
        }

        @Override
        public void run() {
            isRunning = true;
            AppTheme.Theme theme = AppConfig.getInstance().getAppTheme();
            GradientConfig gradient1 = new GradientConfig();
            gradient1.setColor1(theme.getNormalBgColor());
            gradient1.setColor2(theme.getNormalFgColor());
            gradient1.setGradientType(GradientUtil.GradientType.HORIZONTAL_LINEAR);
            GradientConfig gradient2 = new GradientConfig();
            gradient2.setColor1(theme.getNormalFgColor());
            gradient2.setColor2(theme.getNormalBgColor());
            gradient2.setGradientType(GradientUtil.GradientType.HORIZONTAL_LINEAR);
            while (isRunning) {
                BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics = image.createGraphics();
                graphics.setColor(theme.getNormalBgColor());
                graphics.fillRect(0, 0, WIDTH, HEIGHT);
                GradientUtil.fill(gradient1, graphics, 0, 0, x, AudioPanel.WAVEFORM_HEIGHT);
                GradientUtil.fill(gradient2, graphics, x, 0, WIDTH - x, AudioPanel.WAVEFORM_HEIGHT);
                graphics.dispose();

                ImageTextUtil.drawText(image, "R E A D Y", 100,
                        new Font(Font.MONOSPACED, Font.PLAIN, 12),
                        ImageTextUtil.TextAlign.CENTER,
                        theme.getNormalFgColor(),
                        0f,
                        theme.getNormalFgColor(),
                        null,
                        new Rectangle(60, 20, WIDTH - 120, 100));
                AudioPanel.getInstance().setIdleImage(image);

                x++;

                try {
                    Thread.sleep(80);
                } catch (InterruptedException ignored) {
                }
            }
        }
    }
}
