package ca.corbett.musicplayer.audio;

import ca.corbett.extras.audio.WaveformConfig;
import ca.corbett.musicplayer.AppConfig;
import ca.corbett.musicplayer.ui.AppTheme;
import org.tritonus.share.sampled.file.TAudioFileFormat;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A wrapper class to encapsulate a single audio clip.
 * Here, we track the original file, the converted file (if conversion was
 * done), the audio data associated with the clip, and metadata that we
 * managed to extract from the clip.
 * <p>
 * TODO I'm not happy with the wonky "convert from mp3 to wav and then play the wav" approach
 *      It's slow and monstrously memory unfriendly. Mostly I hate it because it's very slow,
 *      and results in a long gap of silence between tracks as the next track loads.
 *      But, without the raw wav data, I can't find a way to generate the audio waveform image,
 *      which is kind of a neat feature, and which also allows skipping forwards and backwards
 *      through the track by clicking on the waveform. Surely there's a middle of the road
 *      option where the application could have the best of both worlds - the speed of being
 *      able to stream audio directly from an mp3 file, while still being able to calculate
 *      a waveform image and allow clicking within it?
 * </p>
 *
 * @author scorbo2
 * @since 2025-03-23
 */
public class AudioData {

    private static final Logger logger = Logger.getLogger(AudioData.class.getName());

    private final int[][] rawData;
    private final File sourceFile;
    private BufferedImage waveformImage;
    private Metadata metadata;
    private final float sampleRate;
    private final int durationSeconds;

    public AudioData(int[][] rawData, File sourceFile, float sampleRate) {
        this.rawData = rawData;
        this.sourceFile = sourceFile;
        this.sampleRate = (sampleRate < 0) ? 44100f : sampleRate;
        this.durationSeconds = (int) (rawData[0].length / sampleRate);
    }

    public int[][] getRawData() {
        return rawData;
    }

    public float getSampleRate() {
        return sampleRate;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public File getSourceFile() {
        return sourceFile;
    }

    /**
     * Makes a best attempt to parse metadata out of our source file
     * and return it in the form of a Metadata instance. In the case of
     * an MP3 file, this will be whatever we could parse out of the id3
     * tag (if present). Otherwise, this will be what we could cobble
     * together from the file itself (for example, "title" will just
     * be the file name and "album" will be an empty string).
     *
     * @return A Metadata instance, which may or may not contain good info.
     */
    public Metadata getMetadata() {
        if (metadata == null && sourceFile != null) {
            try {
                // First attempt!
                // Try to read it from the file metadata, if present:
                AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(sourceFile);
                if (fileFormat instanceof TAudioFileFormat) {
                    Map properties = fileFormat.properties();
                    String title = (String) properties.get("title");
                    String author = (String) properties.get("author");
                    String album = (String) properties.get("album");
                    if (title != null) {
                        metadata = new Metadata(title,
                                                author == null ? "" : author,
                                                album == null ? "" : album,
                                                durationSeconds,
                                                sourceFile);
                    }
                }
            } catch (NullPointerException npe) {
                logger.warning("Audio file " + sourceFile.getAbsolutePath() + " has no metadata.");
            } catch (Exception e) {
                logger.warning("Unable to parse audio metadata from " + sourceFile.getAbsolutePath() + ": " + e.getMessage());
            }

            // Second attempt!
            // If metadata is still null at this point, try a more manual approach:
            if (metadata == null) {
                String title = sourceFile.getName();

                // This is a totally arbitrary assumption, but I'm going to assume
                // that most music collections are structured like:
                // ArtistName/AlbumName/files...
                String album = sourceFile.getParentFile().getName();

                // But I won't go so far as to assume that the grandparent
                // dir is the artist name, or that there even is a grandparent dir.
                // I'll just leave it blank:
                String author = "";

                metadata = new Metadata(title, album, author, durationSeconds, sourceFile);
            }
        }

        return metadata;
    }

    /**
     * Gets the waveform image that we generated for this audio clip, or
     * generates one if we haven't done that yet.
     *
     * @return A BufferedImage using the waveform prefs from app config.
     */
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

    /**
     * Invoked internally to generate the waveform image for our audio data.
     *
     * @return A BufferedImage representing our audio, or null if we have no audio.
     */
    private BufferedImage generateWaveformImage(int[][] audioData) {
        BufferedImage waveform = null;
        if (audioData == null) {
            return waveform;
        }
        WaveformConfig config = getWaveformConfig();

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
            int sample1 = Math.abs(audioData[topChannelIndex][i] / config.getCompression().getYValue());
            int sample2 = Math.abs(audioData[btmChannelIndex][i] / config.getCompression().getYValue());

            averagedSample1 += sample1;
            averagedSample2 += sample2;
            averagedSampleCount++;
            if (averagedSampleCount > config.getCompression().getXValue()) {
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
        int xScale = config.getCompression().getXValue();
        int width = audioData[0].length / config.getCompression().getXValue();
        if (width > config.getWidthLimit().getLimit()) {
            width = config.getWidthLimit().getLimit();
            xScale = audioData[0].length / config.getWidthLimit().getLimit();
            logger.log(Level.INFO, "AudioUtil: scaling waveform image down to fit X limit of {2} (you can change this in settings).",
                       new Object[]{config.getCompression().getXValue(), xScale, config.getWidthLimit().getLimit()});
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
            averagedSample1 += Math.abs(audioData[topChannelIndex][sample] / config.getCompression().getYValue());
            averagedSample2 += Math.abs(audioData[btmChannelIndex][sample] / config.getCompression().getYValue());
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

    /**
     * Talks to AppConfig to get the current waveform settings, and returns
     * a WaveformConfig corresponding to those settings. Each time this method
     * is invoked, a new WaveformConfig will be generated based on latest
     * AppConfig settings.
     *
     * @return A WaveformConfig driven by user preferences in AppConfig.
     */
    private WaveformConfig getWaveformConfig() {
        WaveformConfig config = null;

        // User wants to use the config from the current app theme:
        if (AppConfig.getInstance().useWaveformFromAppTheme()) {
            AppTheme.Theme theme = AppConfig.getInstance().getAppTheme();
            config = theme.getWaveformConfig() == null ? new WaveformConfig() : theme.getWaveformConfig();
        }

        // User wants to override the current app theme with custom settings:
        else {
            config = new WaveformConfig();
            config.setCompression(AppConfig.getInstance().getWaveformResolution());
            config.setWidthLimit(AppConfig.getInstance().getWaveformWidthLimit());
            config.setBgColor(AppConfig.getInstance().getWaveformBgColor());
            config.setFillColor(AppConfig.getInstance().getWaveformFillColor());
            config.setOutlineColor(AppConfig.getInstance().getWaveformOutlineColor());
            config.setOutlineThickness(AppConfig.getInstance().getWaveformOutlineWidth());
            config.setBaselineEnabled(false);
        }

        return config;
    }

    /**
     * Id3 tags apparently encode the track duration in microseconds, which seems overkillish
     * to me, but okay. This method will convert some ungodly huge number to a human-readable
     * time string.
     *
     * @param value A value in microseconds.
     * @return A human readable string in the form [[HH:]MM:]SS
     */
    private String formatMicroseconds(Long value) {
        if (value == null) {
            return " (n/a) ";
        }

        int seconds = (int) (value / 1000 / 1000);
        int hours = 0;
        int minutes = 0;
        while (seconds > 3600) {
            seconds -= 3600;
            hours++;
        }
        while (seconds > 60) {
            seconds -= 60;
            minutes++;
        }

        StringBuilder sb = new StringBuilder();
        if (hours > 0) {
            sb.append(hours);
            sb.append(":");
        }
        if (hours > 0 || minutes > 0) {
            sb.append(String.format("%02d", minutes));
            sb.append(":");
        }
        sb.append(String.format("%02d", seconds));
        return sb.toString();
    }

    public static class Metadata {
        public final String title;
        public final String author;
        public final String album;
        public final int durationSeconds;
        public final File sourceFile;

        public Metadata(String title, String author, String album, int durationSeconds) {
            this(title, author, album, durationSeconds, null);
        }

        public Metadata(String title, String author, String album, int durationSeconds, File sourceFile) {
            this.title = title;
            this.author = author;
            this.album = album;
            this.durationSeconds = durationSeconds;
            this.sourceFile = sourceFile;
        }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof Metadata metadata)) { return false; }
            return durationSeconds == metadata.durationSeconds
                && Objects.equals(title, metadata.title)
                && Objects.equals(author, metadata.author)
                && Objects.equals(album, metadata.album);
        }

        @Override
        public int hashCode() {
            return Objects.hash(title, author, album, durationSeconds);
        }
    }
}
