package ca.corbett.musicplayer.ui;

import ca.corbett.musicplayer.AppConfig;
import ca.corbett.musicplayer.actions.ReloadUIAction;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.logging.Logger;

/**
 * A utility class for drawing and managing the VisualizerOverlay.
 *
 * @author scorbo2
 * @since 2017-12-15
 */
public final class VisualizationOverlay implements UIReloadable {

    public enum OverlaySize {
        SMALL("Small", 25),
        MEDIUM("Medium", 45),
        LARGE("Large", 75);

        public final String label;
        public final int columns;

        OverlaySize(String label, int cols) {
            this.label = label;
            this.columns = cols;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    /**
     * The default width, in columns, of the "value" area (where title,artist,album are shown). *
     */
    public static final int DEFAULT_COLUMNS = 25;

    private static final Logger logger = Logger.getLogger(VisualizationOverlay.class.getName());

    private static VisualizationOverlay instance;
    private VisualizationTrackInfo trackInfo;
    private BufferedImage buffer;

    private int width;   // Pixel width of the generated image, including all margins + border
    private int height;  // Pixel height of the generated image, including all margins + border
    private int borderWidth = 2; // make this configurable or nah?

    private boolean isEnabled;
    private int labelX;  // The starting x co-ordinate for the label text
    private int valueX;  // The starting x co-ordinate for the value text
    private int titleBaselineY;  // The bottom of the title display area
    private int artistBaselineY; // The bottom of the artist display area
    private int albumBaselineY;  // The bottom of the album display area
    private int trackTimeBaselineY; // The bottom of the track time display area
    private int progressBarLeft;  // The starting x co-ordinate for the track progress bar
    private int progressBarRight; // The ending x co-ordinate for the track progress bar

    // These will be grabbed from AppConfig as needed:
    private String fontFamily;
    private int fontSize;
    private Color bgColor;
    private Color fgColor;
    private Color highlightBgColor;
    private Color highlightFgColor;
    private Color headerColor;
    private float opacity;

    /**
     * Constructor is private to enforce singleton access.
     */
    private VisualizationOverlay() {
        trackInfo = null;
        reloadUI();
    }

    /**
     * The singleton accesssor. Returns an instance with all default values.
     *
     * @return The single instance of this class.
     */
    public static VisualizationOverlay getInstance() {
        if (instance == null) {
            instance = new VisualizationOverlay();
            ReloadUIAction.getInstance().registerReloadable(instance);
        }
        return instance;
    }

    @Override
    public void reloadUI() {
        AppTheme.Theme theme = AppConfig.getInstance().getAppTheme();
        isEnabled = AppConfig.getInstance().isVisualizerOverlayEnabled();
        bgColor = theme.getNormalBgColor();
        fgColor = theme.getNormalFgColor();
        headerColor = theme.getHeaderFgColor();
        fontFamily = AppConfig.getInstance().getVisualizerOverlayFont().familyName;
        fontSize = AppConfig.getInstance().getVisualizerOverlayFontSize();
        highlightBgColor = theme.getSelectedBgColor();
        highlightFgColor = theme.getSelectedFgColor();
        opacity = AppConfig.getInstance().getVisualizationOverlayOpacity();
        recomputeSize();
    }

    /**
     * Sets the current track info. Any changes are reflected the next time render() is invoked.
     *
     * @param info The current TrackInfo object.
     */
    public void setTrackInfo(VisualizationTrackInfo info) {
        trackInfo = info;
    }

    public float getOpacity() {
        return opacity;
    }

    /**
     * Redraws the overlay and returns a BufferedImage containing the output.
     * If you have not yet invoked setPreferences(), the default overlay preferences
     * are used. If you have not yet invoked setTrackInfo(), the overlay will show
     * that nothing is currently playing.
     *
     * @return A BufferedImage containing the overlay.
     */
    public BufferedImage render() {
        // If we're disabled, just return immediately with an empty image:
        if (!isEnabled) {
            return buffer;
        }

        Graphics2D graphics = (Graphics2D) buffer.createGraphics();

        // It's faster to just do a flood fill with the border colour
        // and then a subfill with the background colour if we're required
        // to draw a border:
        if (borderWidth > 0) {
            graphics.setColor(fgColor);
            graphics.fillRect(0, 0, width, height);

            graphics.setColor(bgColor);
            graphics.fillRect(borderWidth, borderWidth, width - (borderWidth * 2), height - (borderWidth * 2));
        }

        // If there's no border, just do a flood fill with the background colour.
        // Note we're ignoring opacity here because the animation thread will handle that
        // as needed.
        else {
            graphics.setColor(bgColor);
            graphics.fillRect(0, 0, width, height);
        }

        // Draw all labels:
        graphics.setColor(headerColor);
        graphics.setFont(new Font(fontFamily, Font.BOLD, fontSize));
        graphics.drawString("Title:", labelX, titleBaselineY);
        graphics.drawString("Artist:", labelX, artistBaselineY);
        graphics.drawString("Album:", labelX, albumBaselineY);
        graphics.drawString("Time:", labelX, trackTimeBaselineY);

        // Grab label values from the TrackInfo, or fill them in if needed:
        String title = (trackInfo == null) ? "(nothing playing)" : trackInfo.getTitle();
        String artist = (trackInfo == null) ? "N/A" : trackInfo.getArtist();
        String album = (trackInfo == null) ? "N/A" : trackInfo.getAlbum();
        String trackTime = computeTrackTime();

        // Draw all value labels:
        graphics.setColor(fgColor);
        graphics.setFont(new Font(fontFamily, Font.PLAIN, fontSize));
        graphics.drawString(title, valueX, titleBaselineY);
        graphics.drawString(artist, valueX, artistBaselineY);
        graphics.drawString(album, valueX, albumBaselineY);
        graphics.drawString(trackTime, valueX, trackTimeBaselineY);
        int trackTimeWidth = (int) graphics.getFontMetrics().stringWidth(trackTime);
        int trackTimeHeight = (int) graphics.getFontMetrics().getLineMetrics("A", graphics).getAscent();

        // Draw track time progress bar:
        if (trackInfo != null) {
            graphics.setColor(highlightBgColor);
            int barBottom = trackTimeBaselineY + 2;
            int barHeight = (int) (trackTimeHeight * 0.7f); // slight vertical margin
            int barTop = barBottom - barHeight;
            int barWidth = progressBarRight - progressBarLeft;
            graphics.fillRect(progressBarLeft, barTop, barWidth, barHeight);
            graphics.setColor(highlightFgColor);
            int elapsed = 0;
            if (trackInfo.getTotalTimeSeconds() != 0) {
                elapsed = (int) ((trackInfo.getCurrentTimeSeconds() / (double) trackInfo.getTotalTimeSeconds()) * barWidth);
            }
            graphics.fillRect(progressBarLeft + 2, barTop + 2, elapsed, barHeight - 4);
        }

        graphics.dispose();
        return buffer;
    }

    /**
     * Computes and returns a human-readable string based on the track time
     * information in the current TrackInfo object. If there is no current
     * TrackInfo object, "N/A" is returned.
     */
    private String computeTrackTime() {
        String trackTime = "N/A";
        if (trackInfo != null) {
            int currentMins = 0;
            int currentSecs = trackInfo.getCurrentTimeSeconds();
            int totalMins = 0;
            int totalSecs = trackInfo.getTotalTimeSeconds();

            while (currentSecs >= 60) {
                currentSecs -= 60;
                currentMins++;
            }
            while (totalSecs >= 60) {
                totalMins++;
                totalSecs -= 60;
            }

            trackTime = "";
            if (currentMins < 10) {
                trackTime += "0";
            }
            trackTime += currentMins + ":";
            if (currentSecs < 10) {
                trackTime += "0";
            }
            trackTime += currentSecs + " / ";

            if (totalMins < 10) {
                trackTime += "0";
            }
            trackTime += totalMins + ":";
            if (totalSecs < 10) {
                trackTime += "0";
            }
            trackTime += totalSecs;
        }

        return trackTime;
    }

    /**
     * Invoked internally to set up the layout of the overlay based on current font settings.
     */
    private void recomputeSize() {
        // Ensure we have a buffer so we can get a Graphics object:
        if (buffer == null) {
            buffer = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        }

        Graphics2D graphics = (Graphics2D) buffer.createGraphics();

        // Figure out the width of the widest label. They all use the same font so this should
        // be consistent.
        graphics.setFont(new Font(fontFamily, Font.BOLD, fontSize));
        int labelWidth = (int) graphics.getFontMetrics().stringWidth("Artist:");
        int labelHeight = (int) graphics.getFontMetrics().getLineMetrics("A", graphics).getAscent();

        // Set a margin to the left of where the labels will go:
        labelX = (int) (labelWidth * 0.2);

        // And a slightly smaller margin to the right, where the values will start:
        valueX = labelX + labelWidth + (int) (labelWidth * 0.2);

        // Figure out the width of the three value labels.
        // This is a bit tricky because they can all potentially have different fonts and font
        // sizes. We also don't know how long the values will be, so we have to clip
        // to a number of columns (configurable). This is a bit of a hack because most
        // fonts are proportional, so there isn't a fixed column width to use. We'll go
        // with capital A as our baseline column width.
        String sampleValue = "";
        for (int i = 0; i < AppConfig.getInstance().getVisualizationOverlaySize().columns; i++) {
            sampleValue += "A";
        }
        graphics.setFont(new Font(fontFamily, Font.PLAIN, fontSize));
        int titleWidth = (int) graphics.getFontMetrics().stringWidth(sampleValue);
        int titleHeight = (int) graphics.getFontMetrics().getLineMetrics("A", graphics).getAscent();
        titleHeight = (titleHeight > labelHeight) ? titleHeight : labelHeight;
        int artistWidth = (int) graphics.getFontMetrics().stringWidth(sampleValue);
        int artistHeight = (int) graphics.getFontMetrics().getLineMetrics("A", graphics).getAscent();
        artistHeight = (artistHeight > labelHeight) ? titleHeight : labelHeight;
        int albumWidth = (int) graphics.getFontMetrics().stringWidth(sampleValue);
        int albumHeight = (int) graphics.getFontMetrics().getLineMetrics("A", graphics).getAscent();
        albumHeight = (albumHeight > labelHeight) ? albumHeight : labelHeight;
        int trackTimeWidth = (int) graphics.getFontMetrics().stringWidth("00:00 / 00:00");
        int trackTimeHeight = (int) graphics.getFontMetrics().getLineMetrics("A", graphics).getAscent();
        trackTimeHeight = (trackTimeHeight > labelHeight) ? trackTimeHeight : labelHeight;

        // Take the widest of these as the width of our value area:
        int valueWidth = (titleWidth > artistWidth) ? titleWidth : artistWidth;
        valueWidth = (valueWidth > albumWidth) ? valueWidth : albumWidth;

        // We can now compute an overall width and height, based on these computed values
        // and adding a small margin where needed.
        width = valueX + valueWidth + (int) (valueWidth * 0.1) + (borderWidth * 2);
        height = titleHeight + (int) (titleHeight * 0.2);
        height += artistHeight + (int) (artistHeight * 0.2);
        height += albumHeight + (int) (albumHeight * 0.2);
        height += trackTimeHeight + (int) (trackTimeHeight * 0.4);
        height += borderWidth * 2;

        // Figure out the baseline Y values where each label can be drawn:
        titleBaselineY = titleHeight + (int) (titleHeight * 0.1) + borderWidth;
        artistBaselineY = titleBaselineY + (int) (titleHeight * 0.1)
                + artistHeight + (int) (artistHeight * 0.1);
        albumBaselineY = artistBaselineY + (int) (artistHeight * 0.1)
                + albumHeight + (int) (albumHeight * 0.1);
        trackTimeBaselineY = albumBaselineY + (int) (albumHeight * 0.1)
                + trackTimeHeight + (int) (trackTimeHeight * 0.1);

        // Figure out where to put the progress bar for track time:
        progressBarLeft = valueX + trackTimeWidth + (int) (trackTimeWidth * 0.1);
        progressBarRight = valueX + valueWidth - (int) (trackTimeWidth * 0.2);

        // Create our buffer with the computed size:
        buffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    }

}
