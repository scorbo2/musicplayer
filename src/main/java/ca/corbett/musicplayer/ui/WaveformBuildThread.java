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

    private static int decodeSample(byte[] buffer, int offset, int bytesPerSample, boolean bigEndian, boolean signedPcm) {
        int sample = 0;
        if (bigEndian) {
            for (int i = 0; i < bytesPerSample; i++) {
                sample = (sample << 8) | (buffer[offset + i] & 0xff);
            }
        }
        else {
            for (int i = bytesPerSample - 1; i >= 0; i--) {
                sample = (sample << 8) | (buffer[offset + i] & 0xff);
            }
        }

        int bits = bytesPerSample * 8;
        if (signedPcm) {
            int signBit = 1 << (bits - 1);
            if ((sample & signBit) != 0) {
                sample -= 1 << bits;
            }
        }
        else {
            sample -= 1 << (bits - 1);
        }
        return sample;
    }

    private static short toPeakMagnitude(int sample, int sampleSizeInBits) {
        long abs = Math.abs((long) sample);
        if (sampleSizeInBits > 16) {
            abs >>= (sampleSizeInBits - 16);
        }
        else if (sampleSizeInBits > 0 && sampleSizeInBits < 16) {
            abs <<= (16 - sampleSizeInBits);
        }
        return (short) Math.min(Short.MAX_VALUE, abs);
    }

    @Override
    public void run() {
        if (sourceFile == null || peaks == null) {
            return;
        }

        try (AudioInputStream stream = AudioUtil.openPlaybackStream(sourceFile)) {
            AudioFormat format = stream.getFormat();
            int channels = Math.max(1, format.getChannels());
            int sampleSizeInBits = format.getSampleSizeInBits() > 0 ? format.getSampleSizeInBits() : 16;
            int bytesPerSample = Math.max(1, (sampleSizeInBits + 7) / 8);
            int frameSize = Math.max(bytesPerSample * channels, format.getFrameSize());
            boolean bigEndian = format.isBigEndian();
            boolean signedPcm = AudioFormat.Encoding.PCM_SIGNED.equals(format.getEncoding());
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
                    int frameStart = idx;
                    for (int ch = 0; ch < channels; ch++) {
                        int sample = decodeSample(buffer, idx, bytesPerSample, bigEndian, signedPcm);
                        idx += bytesPerSample;
                        short abs = toPeakMagnitude(sample, sampleSizeInBits);
                        if (abs > maxAbs[ch]) {
                            maxAbs[ch] = abs;
                        }
                    }
                    idx = frameStart + frameSize;

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

