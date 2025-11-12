package ca.corbett.musicplayer.extensions.builtin;

import ca.corbett.extensions.AppExtensionInfo;
import ca.corbett.extras.gradient.ColorSelectionType;
import ca.corbett.extras.image.ImageUtil;
import ca.corbett.extras.image.animation.ImageScroller;
import ca.corbett.extras.properties.AbstractProperty;
import ca.corbett.extras.properties.ColorProperty;
import ca.corbett.extras.properties.DecimalProperty;
import ca.corbett.extras.properties.EnumProperty;
import ca.corbett.extras.properties.IntegerProperty;
import ca.corbett.extras.properties.LabelProperty;
import ca.corbett.musicplayer.AppConfig;
import ca.corbett.musicplayer.Version;
import ca.corbett.musicplayer.actions.ReloadUIAction;
import ca.corbett.musicplayer.extensions.MusicPlayerExtension;
import ca.corbett.musicplayer.ui.UIReloadable;
import ca.corbett.musicplayer.ui.VisualizationManager;
import ca.corbett.musicplayer.ui.VisualizationTrackInfo;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Provides some extra full-screen visualizations to supplement the boring built-in one.
 * These were copied from the old 1.x code.
 *
 * @author scorbo2
 * @since 2017 or so for most of these
 */
public class ExtraVisualizers extends MusicPlayerExtension implements UIReloadable {
    private final AppExtensionInfo info;

    private final RollingWaveVisualizer rollingWaves;
    private final AlbumArtVisualizer albumArtVisualizer;

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
        albumArtVisualizer = new AlbumArtVisualizer();
    }

    @Override
    public void loadJarResources() {
    }

    @Override
    public AppExtensionInfo getInfo() {
        return info;
    }

    @Override
    protected List<AbstractProperty> createConfigProperties() {
        List<AbstractProperty> props = new ArrayList<>();
        props.addAll(rollingWaves.getProperties());
        props.addAll(albumArtVisualizer.getProperties());
        return props;
    }

    @Override
    public void reloadUI() {
        albumArtVisualizer.reloadUI();
    }

    @Override
    public void onActivate() {
        ReloadUIAction.getInstance().registerReloadable(this);
    }

    @Override
    public void onDeactivate() {
        ReloadUIAction.getInstance().unregisterReloadable(this);
    }

    @Override
    public List<VisualizationManager.Visualizer> getCustomVisualizers() {
        List<VisualizationManager.Visualizer> visualizers = new ArrayList<>();

        visualizers.add(rollingWaves);
        visualizers.add(albumArtVisualizer);

        return visualizers;
    }

    /**
     * This was actually the very first visualizer I wrote for MusicPlayer 1.0 back in 2017 or so.
     */
    public static class RollingWaveVisualizer extends VisualizationManager.Visualizer {

        public static final String NAME = "Rolling waves";
        private static final String startColorPropName = "Visualizers.Rolling Waves.General.startColor";
        private static final String endColorPropName = "Visualizers.Rolling Waves.endColor";
        private static final String directionPropName = "Visualizers.Rolling Waves.direction";
        private static final String wavelengthPropName = "Visualizers.Rolling Waves.wavelength";
        private static final String waveSpeedPropName = "Visualizers.Rolling Waves.waveSpeed";

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
            props.add(LabelProperty.createLabel("Visualizers.Rolling Waves.label",
                    "<html>The " + NAME + " visualizer shows a gently rolling wave of<br>" +
                            "color gradients, moving either horizontally or vertically.</html>"
            ));
            props.add(new ColorProperty(startColorPropName, "Start color:", ColorSelectionType.SOLID)
                          .setSolidColor(Color.BLACK));
            props.add(new ColorProperty(endColorPropName, "End color:", ColorSelectionType.SOLID)
                          .setSolidColor(Color.BLUE));
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
            startColor = ((ColorProperty)AppConfig.getInstance().getPropertiesManager()
                                                  .getProperty(startColorPropName)).getSolidColor();
            endColor = ((ColorProperty)AppConfig.getInstance().getPropertiesManager()
                                                .getProperty(endColorPropName)).getSolidColor();
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

    /**
     * The AlbumArtVisualizer looks for album or track images in the same directory as whatever
     * audio file is being played. We start by looking for a file with the same filename as the
     * audio track, but with a jpg or png extension. If that is not present, we look for a file
     * named album.png or album.jpg in the same directory.
     * <p>
     * If a track image or an album image is found, it is shown using the configuration
     * properties for this extension in application settings. Otherwise, a plain black
     * screen.
     * </p>
     * <p>
     * Note that this extension supports visualizer overrides. So, you don't have to pick
     * it as the default visualizer. Just enable "visualizer override" in settings, and
     * if a track or album image is detected, it will automatically activate this visualizer.
     * </p>
     *
     * @author scorbo2
     * @since 2024-04-03
     */
    public static class AlbumArtVisualizer extends VisualizationManager.Visualizer implements UIReloadable {

        private static final Logger logger = Logger.getLogger(AlbumArtVisualizer.class.getName());

        private static final String NAME = "Album art";
        private static final String OVERSIZE_PROP = "Visualizers.Album Art.oversizeImage";
        private static final String SPEED_PROP = "Visualizers.Album Art.scrollSpeed";
        private static final String BOUNCE_ZONE_PROP = "Visualizers.Album Art.bounceZone";
        private static final String BOUNCE_EASING_STRENGTH_PROP = "Visualizers.Album Art.easingStrength";

        public enum OversizeHandling {
            SCALE_TO_FIT("Scale to fit screen"),
            STRETCH_TO_FIT("Stretch to fit screen"),
            OVERFLOW_AND_PAN("Overflow and slow scroll");

            private final String label;

            OversizeHandling(String label) {
                this.label = label;
            }

            @Override
            public String toString() {
                return label;
            }
        }

        private File sourceFile;
        int width;
        int height;
        private OversizeHandling oversizeHandling;
        BufferedImage image;
        private ImageScroller imageScroller;
        private volatile boolean isLoadInProgress;

        public AlbumArtVisualizer() {
            super(NAME);
            oversizeHandling = OversizeHandling.SCALE_TO_FIT;
        }

        @Override
        public void initialize(int width, int height) {
            // We won't get image details until we get a renderFrame() message, so just blank out for now:
            if (imageScroller == null) {
                imageScroller = new ImageScroller(createBlankImage(width, height), width, height);
            }
            if (image != null) {
                reset();
                isLoadInProgress = false;
            }
            this.width = width;
            this.height = height;
            reloadUI();
        }

        private BufferedImage createBlankImage(int w, int h) {
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, w, h);
            g.dispose();
            return img;
        }

        // Not currently using these setters, but we could wire them up with AppProperties maybe...
        public void setBounceZoneRatio(float ratio) {
            if (imageScroller != null) {
                imageScroller.setBounceZoneRatio(Math.max(0.01f, Math.min(0.5f, ratio)));
            }
        }

        public void setMinSpeedRatio(float ratio) {
            if (imageScroller != null) {
                imageScroller.setMinSpeedRatio(Math.max(0.0f, Math.min(1.0f, ratio)));
            }
        }

        public void setEasingPower(ImageScroller.EasingStrength strength) {
            if (imageScroller != null) {
                imageScroller.setEasingStrength(strength);
            }
        }

        public void setScrollSpeed(ImageScroller.ScrollSpeed speed) {
            if (imageScroller != null) {
                imageScroller.setScrollSpeed(speed);
            }
        }

        @Override
        public void reloadUI() {
            AbstractProperty oversizedProp = AppConfig.getInstance().getPropertiesManager().getProperty(OVERSIZE_PROP);
            AbstractProperty scrollProp = AppConfig.getInstance().getPropertiesManager().getProperty(SPEED_PROP);
            AbstractProperty zoneProp = AppConfig.getInstance().getPropertiesManager().getProperty(BOUNCE_ZONE_PROP);
            AbstractProperty easingProp = AppConfig.getInstance().getPropertiesManager()
                                                   .getProperty(BOUNCE_EASING_STRENGTH_PROP);
            if (!(oversizedProp instanceof EnumProperty) ||
                !(scrollProp instanceof EnumProperty) ||
                !(zoneProp instanceof DecimalProperty) ||
                !(easingProp instanceof EnumProperty)) {
                logger.warning("AlbumArtVisualizer: our properties are of the wrong type!");
                return;
            }

            // We can't use instanceof to pre-check these class casts because of type erasure, but eh, it'll be fine.
            //noinspection unchecked
            oversizeHandling = ((EnumProperty<OversizeHandling>)oversizedProp).getSelectedItem();
            //noinspection unchecked
            setScrollSpeed(((EnumProperty<ImageScroller.ScrollSpeed>)scrollProp).getSelectedItem());
            setBounceZoneRatio((float)((DecimalProperty)zoneProp).getValue());
            //noinspection unchecked
            setEasingPower(((EnumProperty<ImageScroller.EasingStrength>)easingProp).getSelectedItem());
        }

        public List<AbstractProperty> getProperties() {
            List<AbstractProperty> props = new ArrayList<>();
            props.add(LabelProperty.createLabel("Visualizers.Album Art.label",
                    "<html>The " + NAME + " visualizer looks for a an image file<br>" +
                            "for the current track or the current album. If it finds<br>" +
                            "one, the image is displayed for visualization.<br><br>" +
                            "Try putting an album.png or album.jpg in your music folder!<br>" +
                            "Or, an image file with the same name as an audio track.<br>" +
                            "  Example: some_track.mp3 and some_track.png</html>"));
            props.add(new EnumProperty<OversizeHandling>(OVERSIZE_PROP, "Oversized images:", oversizeHandling));
            props.add(new EnumProperty<ImageScroller.ScrollSpeed>(SPEED_PROP, "Scroll speed:",
                                                                  ImageScroller.ScrollSpeed.SLOW));

            AbstractProperty prop = new DecimalProperty(BOUNCE_ZONE_PROP, "Bounce zone size:", 0.06, 0.01,
                                                        0.49, 0.01);
            prop.setHelpText(
                "<html>Percentage of image width/height to<br>use as the \"bounce zone\"<br>(For acceleration/deceleration)</html>");
            props.add(prop);
            prop = new EnumProperty<ImageScroller.EasingStrength>(BOUNCE_EASING_STRENGTH_PROP, "Easing strength:",
                                                                  ImageScroller.EasingStrength.QUADRATIC);
            prop.setHelpText("Strength of acceleration/deceleration");
            props.add(prop);

            return props;
        }

        /**
         * Invoked internally by our image loading thread, so we don't block the rendering
         * thread when loading track or album images.
         *
         * @param image The loaded image.
         */
        private void setImageData(BufferedImage image) {
            reset();
            this.isLoadInProgress = false;
            this.image = image;
            imageScroller.setImage(image);
        }

        @Override
        public boolean isSupportsFileTriggers() {
            return true;
        }

        /**
         * We will volunteer to override whatever the current visualizer is IF the given
         * track has either an album image or a track image available. An album image is
         * an image file in the same directory as the given audio file, and the album
         * image must be named "album.jpg" or "album.png". That image applies to every
         * track in the same directory. Alternatively, you can set a track image for a
         * given audio file, by creating an image with the same name as the audio file
         * but with a "jpg" or a "png" extension. For example, if your audio track is
         * called "some_track.mp3", then a track image will be looked for with the name
         * "some_track.jpg" or "some_track.png". If no such image files are found, then
         * we return false here.
         *
         * @param trackInfo Metadata and source file information for the new track. The
         *                  visualization manager will invoke this method once whenever
         *                  the current track changes to the next one. Might be null
         *                  if the audio has stopped.
         * @return true if we found an album image or a track image for the given audio file.
         */
        @Override
        public boolean hasOverride(VisualizationTrackInfo trackInfo) {
            if (trackInfo != null && trackInfo.getSourceFile() != null && trackInfo.getSourceFile().exists()) {
                return findImage(trackInfo.getSourceFile()) != null;
            }
            return false;
        }

        @Override
        public void renderFrame(Graphics2D g, VisualizationTrackInfo trackInfo) {
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, width, height);

            // Has the source file changed since our last render?
            if (trackInfo != null && !Objects.equals(sourceFile, trackInfo.getSourceFile())) {
                reset();
                sourceFile = trackInfo.getSourceFile();
            }

            if (image != null) {
                // If the image dimensions are very small, just ignore it:
                // (see asyncImageLoad below for details... this is a bit goofy but handles image load issues)
                if (image.getWidth() < 5 && image.getHeight() < 5) {
                    return;
                }

                // If we're set to stretch the image, do it:
                if (oversizeHandling == OversizeHandling.STRETCH_TO_FIT) {
                    g.drawImage(imageScroller.getImage(), 0, 0, width, height, null);
                }

                // Otherwise, check to see if a scale is necessary:
                else if (oversizeHandling == OversizeHandling.SCALE_TO_FIT || (image.getWidth() <= width && image.getHeight() <= height)) {
                    float zoomFactor;
                    float imgAspect = (float)image.getWidth() / (float)image.getHeight();
                    float myAspect = (float)width / (float)height;
                    if (imgAspect >= 1.0) {
                        if (myAspect >= imgAspect) {
                            zoomFactor = (float)height / image.getHeight();
                        }
                        else {
                            zoomFactor = (float)width / image.getWidth();
                        }
                    }
                    else {
                        if (myAspect <= imgAspect) {
                            zoomFactor = (float)width / image.getWidth();
                        }
                        else {
                            zoomFactor = (float)height / image.getHeight();
                        }
                    }

                    if (zoomFactor <= 0.0) {
                        zoomFactor = 1;
                    }

                    // Figure out our actual image dimensions:
                    int imgWidth = (int) (image.getWidth() * zoomFactor);
                    int imgHeight = (int) (image.getHeight() * zoomFactor);
                    int centerX = (width / 2) - (imgWidth / 2);
                    int centerY = (height / 2) - (imgHeight / 2);
                    g.drawImage(image, centerX, centerY, imgWidth, imgHeight, null);
                }

                // Otherwise, scale and scroll:
                else {
                    imageScroller.renderFrame(g);
                }
            }

            // otherwise, fire off a worker thread to load it:
            else {
                if (!isLoadInProgress && sourceFile != null) {
                    asyncImageLoad(findImage(sourceFile));
                }
            }
        }

        private void reset() {
            if (image != null) {
                image.flush();
                image = null;
                sourceFile = null;
            }
        }

        private void asyncImageLoad(File imageFile) {
            isLoadInProgress = true;
            final AlbumArtVisualizer thisVisualizer = this;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    // We default to a very small image. This seems goofy, but setting this
                    // is preferable to "null" in the case that something goes wrong, because
                    // otherwise we'd end up calling this over and over every time renderFrame()
                    // is invoked, in the case of an image that can't be loaded. So, default
                    // to a very small image, and we'll add some logic in the render loop to
                    // just ignore such small images. That way we only hit this code once.
                    BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
                    try {
                        if (imageFile != null) {
                            logger.info("Loading track image: " + imageFile.getAbsolutePath());
                            image = ImageUtil.loadImage(imageFile);
                        }
                    } catch (IOException ioe) {
                        logger.severe("Unable to load track image: " + ioe.getMessage());
                    }
                    thisVisualizer.setImageData(image);
                }
            }).start();
        }

        @Override
        public void stop() {
            reset();
        }

        /**
         * Looks for an album image or a track image for the given audio file.
         * We will prioritize a track image if one exists, and fall back to
         * an album image if there is no track image. If no image exists,
         * we return null. We prioritize png over jpg, so if both exist,
         * we'll use the png to avoid compression artifacts.
         *
         * @param audioFile The audio file in question.
         * @return A track image, or album image, or null if nothing was found.
         */
        private File findImage(File audioFile) {
            File parentDir = audioFile.getParentFile();
            File albumImageJpg = new File(parentDir, "album.jpg");
            File albumImagePng = new File(parentDir, "album.png");
            File trackImageJpg = new File(parentDir, createFilename(audioFile.getName(), "jpg"));
            File trackImagePng = new File(parentDir, createFilename(audioFile.getName(), "png"));
            if (trackImagePng.exists()) {
                return trackImagePng;
            }
            if (trackImageJpg.exists()) {
                return trackImageJpg;
            }
            if (albumImagePng.exists()) {
                return albumImagePng;
            }
            if (albumImageJpg.exists()) {
                return albumImageJpg;
            }
            return null;
        }

        /**
         * Takes any file name, strips off its extension, and returns an equivalent filename
         * with the new given extension. For example:
         * <pre>createFilename("some_file.mp3", "png");</pre>
         * This will return "some_file.png".
         * <p>
         * If the input filename has no extension, we'll just tack the given extension onto it.
         * </p>
         *
         * @param basename     The base name of the input file.
         * @param newExtension The new extension to use, replacing the old extension.
         * @return A new filename.
         */
        private String createFilename(String basename, String newExtension) {
            // Special case files with no extension:
            if (!basename.contains(".")) {
                return basename + "." + newExtension;
            }

            // Otherwise, strip off the last extension and add the new one:
            String newName = basename.substring(0, basename.lastIndexOf("."));
            return newName + "." + newExtension;
        }
    }
}
