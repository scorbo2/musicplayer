package ca.corbett.musicplayer.audio;

import ca.corbett.extras.audio.WaveformConfig;
import ca.corbett.musicplayer.AppConfig;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A wrapper class to encapsulate a single audio clip.
 *
 * @author scorbo2
 * @since 2025-03-23
 */
public class AudioData {

    private static final Logger logger = Logger.getLogger(AudioData.class.getName());

    private final int[][] rawData;
    private final File sourceFile;
    private BufferedImage waveformImage;

    public AudioData(int[][] rawData, File sourceFile) {
        this.rawData = rawData;
        this.sourceFile = sourceFile;
    }

    public int[][] getRawData() {
        return rawData;
    }

    public File getSourceFile() {
        return sourceFile;
    }

    public BufferedImage getWaveformImage() {
        if (waveformImage == null) {
            waveformImage = generateWaveformImage(rawData);
        }
        return waveformImage;
    }

    /**
     * Force a regeneration of our waveform image. Useful if app preferences
     * have changed and the waveform config (colors, background, etc) have changed.
     *
     * @return A BufferedImage representing audio data for our clip.
     */
    public BufferedImage regenerateWaveformImage() {
        waveformImage = null;
        return getWaveformImage();
    }

    private static BufferedImage generateWaveformImage(int[][] audioData) {
        BufferedImage waveform = null;
        WaveformConfig config = new WaveformConfig();
        config.setXLimit(AppConfig.getInstance().getWaveformResolution().getXLimit());
        config.setXScale(768);
        config.setBgColor(AppConfig.getInstance().getWaveformBgColor());
        config.setFillColor(AppConfig.getInstance().getWaveformFillColor());
        config.setOutlineColor(AppConfig.getInstance().getWaveformOutlineColor());
        config.setOutlineThickness(AppConfig.getInstance().getWaveformOutlineWidth());

        if (audioData == null) {
            return waveform;
        }

        // Make sure our audio channel indexes make sense:
        int topChannelIndex = Math.max(config.getTopChannelIndex(), 0);
        int btmChannelIndex = Math.max(config.getBottomChannelIndex(), 0);
        topChannelIndex = (topChannelIndex >= audioData.length) ? audioData.length - 1 : topChannelIndex;
        btmChannelIndex = (btmChannelIndex >= audioData.length) ? audioData.length - 1 : btmChannelIndex;

        // Go through the data and find our highest y values:
        int averagedSample1 = 0;
        int averagedSample2 = 0;
        int averagedSampleCount = 0;
        int maxY1 = 0;
        int maxY2 = 0;
        for (int i = 0; i < audioData[0].length; i++) {
            int sample1 = Math.abs(audioData[topChannelIndex][i] / config.getYScale());
            int sample2 = Math.abs(audioData[btmChannelIndex][i] / config.getYScale());

            averagedSample1 += sample1;
            averagedSample2 += sample2;
            averagedSampleCount++;
            if (averagedSampleCount > config.getXScale()) {
                averagedSample1 /= averagedSampleCount;
                averagedSample2 /= averagedSampleCount;

                maxY1 = Math.max(averagedSample1, maxY1);
                maxY2 = Math.max(averagedSample2, maxY2);
                averagedSample1 = 0;
                averagedSample2 = 0;
                averagedSampleCount = 0;
            }
        }

        // We can now create a blank image of the appropriate size based on this scale:
        int xScale = config.getXScale();
        int width = audioData[0].length / config.getXScale();
        if (width > config.getXLimit()) {
            width = config.getXLimit();
            xScale = audioData[0].length / config.getXLimit();
            logger.log(Level.INFO, "AudioUtil: Adjusted xScale from {0} to {1} to accommodate x limit of {2}.",
                    new Object[]{config.getXScale(), xScale, config.getXLimit()});
        }
        int height = maxY1 + maxY2;
        height = (height <= 0) ? 100 : height; // height can be zero if there's no audio data.
        int verticalMargin = 0; // (int)((maxY1+maxY2)*0.1); // disabling margin for now
        int centerY = maxY1 + verticalMargin;
        waveform = new BufferedImage(width, height + (verticalMargin * 2), BufferedImage.TYPE_INT_RGB);

        // Flood the blank image with our background colour and get ready to draw on it:
        Graphics2D graphics = waveform.createGraphics();
        graphics.setColor(config.getBgColor());
        graphics.fillRect(0, 0, width, height + (verticalMargin * 2));

        // Now generate the waveform:
        int previousSample1 = 0;
        int previousSample2 = 0;
        int x = 0;
        for (int sample = 0; sample < audioData[0].length; sample++) {
            averagedSample1 += Math.abs(audioData[topChannelIndex][sample] / config.getYScale());
            averagedSample2 += Math.abs(audioData[btmChannelIndex][sample] / config.getYScale());
            averagedSampleCount++;

            if (averagedSampleCount > xScale) {
                averagedSample1 /= averagedSampleCount;
                averagedSample2 /= averagedSampleCount;

                graphics.setColor(config.getFillColor());
                graphics.drawLine(x, centerY, x, centerY - averagedSample1);
                graphics.drawLine(x, centerY, x, centerY + averagedSample2);

                if (config.isOutlineEnabled()) {
                    graphics.setColor(config.getOutlineColor());
                    for (int lineI = 0; lineI < config.getOutlineThickness(); lineI++) {
                        graphics.drawLine(x - 1, centerY - previousSample1 - lineI, x, centerY - averagedSample1 - lineI);
                        graphics.drawLine(x - 1, centerY + previousSample2 + lineI, x, centerY + averagedSample2 + lineI);
                    }
                }

                previousSample1 = averagedSample1;
                previousSample2 = averagedSample2;
                averagedSample1 = 0;
                averagedSample2 = 0;
                averagedSampleCount = 0;
                x++;
            }
        }

        if (config.isBaselineEnabled()) {
            int thickness = Math.max(1, config.getBaselineThickness() / 2);
            graphics.setColor(config.getBaselineColor());
            for (int y = centerY - thickness; y <= centerY + thickness; y++) {
                graphics.drawLine(0, y, width, y);
            }
        }

        graphics.dispose();

        return waveform;
    }

}
