package ca.corbett.musicplayer.extensions.builtin;

import ca.corbett.extensions.AppExtensionInfo;
import ca.corbett.extras.properties.AbstractProperty;
import ca.corbett.extras.properties.ColorProperty;
import ca.corbett.extras.properties.EnumProperty;
import ca.corbett.extras.properties.IntegerProperty;
import ca.corbett.musicplayer.AppConfig;
import ca.corbett.musicplayer.Version;
import ca.corbett.musicplayer.extensions.MusicPlayerExtension;
import ca.corbett.musicplayer.ui.VisualizationManager;
import ca.corbett.musicplayer.ui.VisualizationTrackInfo;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides some extra full-screen visualizations to supplement the boring built-in one.
 * These were copied from the old 1.x code.
 *
 * @author scorbo2
 * @since 2017 or so for most of these
 */
public class ExtraVisualizers extends MusicPlayerExtension {
    private final AppExtensionInfo info;

    private RollingWaveVisualizer rollingWaves;

    public ExtraVisualizers() {
        info = new AppExtensionInfo.Builder("Extra visualizers")
                .setAuthor("Steve Corbett")
                .setShortDescription("A collection of extra full-screen visualizers for MusicPlayer.")
                .setLongDescription("Enable this extension for an assortment of extra full-screen visualizers " +
                        "for MusicPlayer. These were taken from the old MusicPlayer 1.x project.")
                .setTargetAppName(Version.NAME)
                .setTargetAppVersion(Version.VERSION)
                .setReleaseNotes("[2025-03-29] for the MusicPlayer 2.0 release")
                .setVersion(Version.VERSION)
                .build();

        rollingWaves = new RollingWaveVisualizer();
    }

    @Override
    public AppExtensionInfo getInfo() {
        return info;
    }

    @Override
    public List<AbstractProperty> getConfigProperties() {
        List<AbstractProperty> props = new ArrayList<>();

        props.addAll(rollingWaves.getProperties());

        return props;
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }

    @Override
    public List<VisualizationManager.Visualizer> getCustomVisualizers() {
        List<VisualizationManager.Visualizer> visualizers = new ArrayList<>();

        visualizers.add(rollingWaves);

        return visualizers;
    }

    public static class RollingWaveVisualizer extends VisualizationManager.Visualizer {

        public static final String NAME = "Rolling waves";
        private static final String startColorPropName = NAME + ".General.startColor";
        private static final String endColorPropName = NAME + ".General.endColor";
        private static final String directionPropName = NAME + ".General.direction";
        private static final String wavelengthPropName = NAME + ".General.wavelength";
        private static final String waveSpeedPropName = NAME + ".General.waveSpeed";

        private Color startColor;
        private Color endColor;
        private DIRECTION direction;
        private int wavelength;
        private int waveSpeed;
        private Color[] gradientPrecompute;
        private BufferedImage buffer;
        private int gradientPosition;
        int width;
        int height;

        public static enum DIRECTION {
            HORIZONTAL("Horizontal"),
            VERTICAL("Vertical");

            private final String label;

            DIRECTION(String label) {
                this.label = label;
            }

            @Override
            public String toString() {
                return label;
            }
        }

        public RollingWaveVisualizer() {
            super(NAME);
        }

        public List<AbstractProperty> getProperties() {
            List<AbstractProperty> props = new ArrayList<>();
            props.add(new ColorProperty(startColorPropName, "Start color:", ColorProperty.ColorType.SOLID, Color.BLACK));
            props.add(new ColorProperty(endColorPropName, "End color:", ColorProperty.ColorType.SOLID, Color.BLUE));
            props.add(new EnumProperty<>(directionPropName, "Direction:", RollingWaveVisualizer.DIRECTION.HORIZONTAL));
            props.add(new IntegerProperty(wavelengthPropName, "Wavelength:", 500, 10, 16000, 10));
            props.add(new IntegerProperty(waveSpeedPropName, "Wave speed:", 2, 1, 8, 1));
            return props;
        }

        @Override
        public void initialize(int width, int height) {
            gradientPosition = 0;
            this.width = width;
            this.height = height;

            // Precompute the gradient colours to save time during the animation loop:
            startColor = ((ColorProperty) AppConfig.getInstance().getPropertiesManager().getProperty(startColorPropName)).getColor();
            endColor = ((ColorProperty) AppConfig.getInstance().getPropertiesManager().getProperty(endColorPropName)).getColor();
            direction = ((EnumProperty<DIRECTION>) AppConfig.getInstance().getPropertiesManager().getProperty(directionPropName)).getSelectedItem();
            wavelength = ((IntegerProperty) AppConfig.getInstance().getPropertiesManager().getProperty(wavelengthPropName)).getValue();
            waveSpeed = ((IntegerProperty) AppConfig.getInstance().getPropertiesManager().getProperty(waveSpeedPropName)).getValue();
            int limit = wavelength;

            gradientPrecompute = new Color[limit * 2];
            for (int i = 0; i < limit * 2; i++) {

                // We need to figure out, on a scale of 0.0 to 1.0, where we are in the gradient.
                // This means from the beginning of the first trough, up to "limit" (which is the peak
                // of the gradient), and then on to the beginning of the next trough. So, 0.0 means we're
                // at the very start of the first trough, 0.5 means we're at the peak of the gradient,
                // and 1.0 means we're at the very end, or at the start of the next trough.
                float blend = (i < limit) ? ((float) i / limit) : ((float) ((limit * 2) - i) / limit);

                // Compute and cache that colour:
                gradientPrecompute[i] = interpolateColor(startColor, endColor, blend);
            }

            // Precompute the gradient image itself:
            int bufferWidth = direction == DIRECTION.HORIZONTAL ? limit * 2 : width;
            int bufferHeight = direction == DIRECTION.HORIZONTAL ? height : limit * 2;
            buffer = new BufferedImage(bufferWidth, bufferHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = (Graphics2D) buffer.createGraphics();
            for (int i = 0; i < limit * 2; i++) {
                graphics.setColor(gradientPrecompute[i]);
                // Draw a single slice of this gradient either vertically or horizontally:
                if (direction == DIRECTION.HORIZONTAL) {
                    graphics.drawLine(i, 0, i, height);
                } else {
                    graphics.drawLine(0, i, width, i);
                }
            }
            graphics.dispose();
        }

        /**
         * Invoked by the Application's animation loop to draw a single frame of the animation.
         * We use internal state variables to determine how far along in the gradient we are
         * and draw as appropriate. Note we don't have to worry about timing of the loop here,
         * that is handled by the Application.
         *
         * @param graphics  A Graphics2D object we can use for drawing.
         * @param trackInfo Information about the currently playing track (ignored here).
         */
        @Override
        public void renderFrame(Graphics2D graphics, VisualizationTrackInfo trackInfo) {

            // Grab our configuration values:
            int limit = wavelength;
            int speed = waveSpeed;

            // If our gradient is horizontal, fill from left to right:
            if (direction == DIRECTION.HORIZONTAL) {
                graphics.drawImage(buffer, -gradientPosition, 0, null);
                graphics.drawImage(buffer, buffer.getWidth() - gradientPosition, 0, null);

                for (int x = (buffer.getWidth() - gradientPosition); x < width; x += buffer.getWidth()) {
                    graphics.drawImage(buffer, x, 0, null);
                }
            }

            // Otherwise, fill from top to bottom:
            else {
                graphics.drawImage(buffer, 0, -gradientPosition, null);
                graphics.drawImage(buffer, 0, buffer.getHeight() - gradientPosition, null);

                for (int y = (buffer.getHeight() - gradientPosition); y <= height; y += buffer.getHeight()) {
                    graphics.drawImage(buffer, 0, y, null);
                }
            }

            // Now increment our state variable. Note that we don't simply ++ here, we want to increment
            // by our "speed" config field. This allows the user to set a value higher than one to make
            // the animation "move" more rapidly:
            gradientPosition += speed;
            if (gradientPosition >= (limit * 2)) {
                gradientPosition = 0;
            }
        }

        @Override
        public void stop() {
            gradientPrecompute = null;
            buffer.flush();
            buffer = null;
        }

        /**
         * Computes a Color that is in between the given start and end, using the given blend.
         * The blend value is a float from 0.0 to 1.0 indicating the distance between start (0.0)
         * and end (1.0) colours. Examples:
         * <ul>
         * <li>interpolateColor(Black,White,0.0) : returns Black</li>
         * <li>interpolateColor(Black,White,1.0) : returns White</li>
         * <li>interpolateColor(Black,White,0.5) : returns Grey</li>
         * </ul>
         * Implementation note: stolen from StackOverflow.
         *
         * @param startColor The starting colour for the interpolation
         * @param endColor   The ending colour for the interpolation
         * @param blend      The position between start and end to interpolate (0=start, 1=end)
         * @return The interpolated color.
         */
        private Color interpolateColor(Color startColor, Color endColor, float blend) {
            // Make sure "blend" is in the range we expect and require:
            blend = (blend > 1.0) ? 1.0f : blend;
            blend = (blend < 0.0) ? 0.0f : blend;

            // Take the inverse of the blend:
            float inverse_blend = 1 - blend;

            // The new RGB components will be partway between the start and the end values:
            float red = startColor.getRed() * inverse_blend + endColor.getRed() * blend;
            float green = startColor.getGreen() * inverse_blend + endColor.getGreen() * blend;
            float blue = startColor.getBlue() * inverse_blend + endColor.getBlue() * blend;

            // It seems occasionally we get a value that is slightly over 255, which causes
            // an IllegalArgumentException. Clip values to the expected range.
            red = (red > 255) ? 255 : red;
            green = (green > 255) ? 255 : green;
            blue = (blue > 255) ? 255 : blue;

            return new Color((float) red / 255, (float) green / 255, (float) blue / 255);
        }
    }
}
