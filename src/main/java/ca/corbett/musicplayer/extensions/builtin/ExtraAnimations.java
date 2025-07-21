package ca.corbett.musicplayer.extensions.builtin;

import ca.corbett.extensions.AppExtensionInfo;
import ca.corbett.extras.gradient.GradientConfig;
import ca.corbett.extras.gradient.GradientUtil;
import ca.corbett.extras.image.ImageTextUtil;
import ca.corbett.extras.properties.AbstractProperty;
import ca.corbett.musicplayer.AppConfig;
import ca.corbett.musicplayer.Version;
import ca.corbett.musicplayer.extensions.MusicPlayerExtension;
import ca.corbett.musicplayer.ui.AppTheme;
import ca.corbett.musicplayer.ui.AudioPanel;
import ca.corbett.musicplayer.ui.AudioPanelIdleAnimation;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A collection of extra "idle" animations - that is, little animations
 * that we can play in the waveform panel when the application is not
 * playing anything.
 *
 * @author scorbo2
 * @since 2025-03-29
 */
public class ExtraAnimations extends MusicPlayerExtension {
    private final AppExtensionInfo info;

    public ExtraAnimations() {
        info = new AppExtensionInfo.Builder("Extra animations")
                .setAuthor("Steve Corbett")
                .setShortDescription("A collection of extra animations for MusicPlayer.")
                .setLongDescription("Enable this extension for an assortment of extra \"idle\" animations " +
                        "for MusicPlayer - that is, little animations that we can play in the waveform " +
                        "panel when the application is not playing anything.")
                .setTargetAppName(Version.NAME)
                .setTargetAppVersion(Version.VERSION)
                .setReleaseNotes("[2025-03-29] for the MusicPlayer 2.0 release")
                .setVersion(Version.VERSION)
                .build();
    }

    @Override
    public AppExtensionInfo getInfo() {
        return info;
    }

    @Override
    protected List<AbstractProperty> createConfigProperties() {
        return List.of();
    }

    @Override
    public List<AudioPanelIdleAnimation.Animation> getCustomIdleAnimations() {
        List<AudioPanelIdleAnimation.Animation> animations = new ArrayList<>();

        animations.add(new RollingColorWaves());
        animations.add(new StaticNoise());
        animations.add(new BouncingBox());

        return animations;
    }

    /**
     * An example Animation implementation that shows a slowly rolling color gradient
     * with the word "ready" printed on it. The colors to use are taken from the
     * current app theme.
     */
    protected static class RollingColorWaves extends AudioPanelIdleAnimation.Animation {

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

    /**
     * An animation that tries to show tv-style static in the background with READY in the foreground.
     * The static will be gray but the text color will be taken from the current theme.
     */
    protected static class StaticNoise extends AudioPanelIdleAnimation.Animation {

        public static final int WIDTH = 400;
        public static final int HEIGHT = AudioPanel.WAVEFORM_HEIGHT;
        private volatile boolean isRunning;

        public StaticNoise() {
            super("Television static");
        }

        @Override
        public void stop() {
            isRunning = false;
        }

        @Override
        public void run() {
            isRunning = true;
            Color fontColor = AppConfig.getInstance().getAppTheme().getNormalFgColor();
            fontColor = new Color(fontColor.getRed(), fontColor.getGreen(), fontColor.getBlue(), 128);
            BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
            int[] staticPixels = new int[128];
            for (int i = 0; i < 128; i++) {
                staticPixels[i] = (0xFF << 24) | ((i & 0xFF) << 16) | ((i & 0xFF) << 8) | (i & 0xFF);
            }
            Random rand = new Random(System.currentTimeMillis());

            while (isRunning) {
                // Generate new static:
                int[] pixels = new int[WIDTH * HEIGHT];
                for (int i = 0; i < pixels.length; i++) {
                    pixels[i] = staticPixels[rand.nextInt(128)];
                }

                // Set it into the image:
                image.setRGB(0, 0, WIDTH, HEIGHT, pixels, 0, WIDTH);

                ImageTextUtil.drawText(image, "R E A D Y", 100,
                        new Font(Font.MONOSPACED, Font.PLAIN, 12),
                        ImageTextUtil.TextAlign.CENTER,
                        fontColor,
                        0f,
                        fontColor,
                        null,
                        new Rectangle(60, 20, WIDTH - 120, 100));
                AudioPanel.getInstance().setIdleImage(image);

                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    /**
     * A box that endless bounces around inside the waveform panel.
     */
    protected static class BouncingBox extends AudioPanelIdleAnimation.Animation {

        public static final int WIDTH = 400;
        public static final int HEIGHT = AudioPanel.WAVEFORM_HEIGHT;
        public static final int BOX_SIZE = 20;
        private volatile boolean isRunning;
        int x = 0;
        int y = 0;
        int deltaX = 1;
        int deltaY = 1;
        int velocity = 2;

        public BouncingBox() {
            super("Bouncing box");
        }

        @Override
        public void stop() {
            isRunning = false;
        }

        @Override
        public void run() {
            isRunning = true;
            AppTheme.Theme theme = AppConfig.getInstance().getAppTheme();
            Color bg = theme.getNormalBgColor();
            Color fg = theme.getNormalFgColor();
            while (isRunning) {
                BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics = image.createGraphics();
                graphics.setColor(bg);
                graphics.fillRect(0, 0, WIDTH, HEIGHT);
                graphics.setColor(fg);
                //graphics.fillOval(x, y, BOX_SIZE, BOX_SIZE); // it could be a bouncing ball instead
                graphics.fillRect(x, y, BOX_SIZE, BOX_SIZE);
                graphics.dispose();

                ImageTextUtil.drawText(image, "R E A D Y", 100,
                        new Font(Font.MONOSPACED, Font.PLAIN, 12),
                        ImageTextUtil.TextAlign.CENTER,
                        fg,
                        0f,
                        fg,
                        null,
                        new Rectangle(60, 20, WIDTH - 120, 100));
                AudioPanel.getInstance().setIdleImage(image);

                // move the box:
                x += (deltaX * velocity);
                y += (deltaY * velocity);

                // bounds checking:
                if (x < 0) { // bounce!
                    deltaX = 1;
                    x = Math.abs(x);
                } else if (x >= (WIDTH - BOX_SIZE)) { // bounce!
                    deltaX = -1;
                    x = WIDTH - (x - (WIDTH - BOX_SIZE)) - BOX_SIZE;
                }
                if (y < 0) { // bounce!
                    deltaY = 1;
                    y = Math.abs(y);
                } else if (y >= (HEIGHT - BOX_SIZE)) { // pam swears she saw it hit the corner once
                    deltaY = -1;
                    y = HEIGHT - (y - (HEIGHT - BOX_SIZE)) - BOX_SIZE;
                }

                try {
                    Thread.sleep(50);
                } catch (InterruptedException ignored) {
                }
            }
        }
    }
}
