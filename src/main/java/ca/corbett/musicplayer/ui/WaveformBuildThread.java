package ca.corbett.musicplayer.ui;

import ca.corbett.musicplayer.audio.AudioUtil;
import ca.corbett.musicplayer.audio.WaveformPeaks;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import java.io.File;
import java.io.IOException;
import java.util.function.BooleanSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Background worker that decodes source audio into compact waveform peaks.
 */
public class WaveformBuildThread extends Thread {

    private static final Logger logger = Logger.getLogger(WaveformBuildThread.class.getName());

    private final File sourceFile;
    private final WaveformPeaks peaks;
    private final BooleanSupplier keepGoing;
    private final Runnable onUpdate;

    public WaveformBuildThread(File sourceFile, WaveformPeaks peaks, BooleanSupplier keepGoing, Runnable onUpdate) {
        super("musicplayer-waveform-builder");
        this.sourceFile = sourceFile;
        this.peaks = peaks;
        this.keepGoing = keepGoing == null ? () -> true : keepGoing;
        this.onUpdate = onUpdate == null ? () -> { } : onUpdate;
        setDaemon(true);
        setPriority(Thread.MIN_PRIORITY);
    }

    @Override
    public void run() {
        if (sourceFile == null || peaks == null) {
            return;
        }

        try (AudioInputStream stream = AudioUtil.openPlaybackStream(sourceFile)) {
            AudioFormat format = stream.getFormat();
            int channels = Math.max(1, format.getChannels());
            int frameSize = Math.max(2, format.getFrameSize());
            int framesPerBucket = Math.max(1, peaks.getFramesPerBucket());
            byte[] buffer = new byte[frameSize * 2048];
            short[] maxAbs = new short[channels];
            int frameCount = 0;
            int updateCounter = 0;

            int bytesRead;
            while ((bytesRead = stream.read(buffer)) > 0) {
                if (!keepGoing.getAsBoolean() || Thread.currentThread().isInterrupted()) {
                    return;
                }

                int frames = bytesRead / frameSize;
                int idx = 0;
                for (int f = 0; f < frames; f++) {
                    for (int ch = 0; ch < channels; ch++) {
                        int lo = buffer[idx++] & 0xff;
                        int hi = buffer[idx++];
                        short sample = (short) ((hi << 8) | lo);
                        short abs = (short) Math.min(Short.MAX_VALUE, Math.abs(sample));
                        if (abs > maxAbs[ch]) {
                            maxAbs[ch] = abs;
                        }
                    }

                    frameCount++;
                    if (frameCount >= framesPerBucket) {
                        peaks.addBucket(maxAbs);
                        maxAbs = new short[channels];
                        frameCount = 0;
                        updateCounter++;
                        if (updateCounter >= 24) {
                            updateCounter = 0;
                            onUpdate.run();
                        }
                    }
                }
            }

            if (frameCount > 0) {
                peaks.addBucket(maxAbs);
            }
            peaks.setComplete(true);
            onUpdate.run();
        }
        catch (IOException ex) {
            logger.log(Level.FINE, "Waveform build aborted for {0}: {1}", new Object[]{sourceFile.getName(), ex.getMessage()});
        }
    }
}

