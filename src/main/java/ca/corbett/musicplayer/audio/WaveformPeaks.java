package ca.corbett.musicplayer.audio;

import java.util.ArrayList;
import java.util.List;

/**
 * Compact waveform data represented as peak amplitudes per bucket.
 *
 * Instances are appended to by a background decode thread while the UI thread reads
 * snapshots for rendering, so mutable operations are synchronized.
 */
public class WaveformPeaks {

    private final int channels;
    private final float sampleRate;
    private final int framesPerBucket;
    private final List<short[]> buckets;
    private volatile boolean complete;

    public WaveformPeaks(int channels, float sampleRate, int framesPerBucket) {
        this.channels = Math.max(1, channels);
        this.sampleRate = sampleRate > 0 ? sampleRate : 44100f;
        this.framesPerBucket = Math.max(1, framesPerBucket);
        this.buckets = new ArrayList<>();
        this.complete = false;
    }

    /**
     * Adds a single peak bucket.
     *
     * If the source bucket is mono (length 1), the value is mirrored into all channels
     * to preserve symmetric top/bottom waveform rendering.
     */
    public synchronized void addBucket(short[] bucket) {
        if (bucket == null || bucket.length == 0) {
            return;
        }
        short[] copy = new short[channels];

        // For mono inputs, mirror the single channel so top/bottom waveform stays symmetric.
        if (bucket.length == 1) {
            for (int i = 0; i < copy.length; i++) {
                copy[i] = bucket[0];
            }
            buckets.add(copy);
            return;
        }

        for (int i = 0; i < copy.length; i++) {
            copy[i] = i < bucket.length ? bucket[i] : 0;
        }
        buckets.add(copy);
    }

    /**
     * Returns an immutable-style snapshot arranged as [channel][bucketIndex].
     */
    public synchronized int[][] snapshotByChannel() {
        int[][] out = new int[channels][buckets.size()];
        for (int i = 0; i < buckets.size(); i++) {
            short[] bucket = buckets.get(i);
            for (int ch = 0; ch < channels; ch++) {
                out[ch][i] = bucket[ch];
            }
        }
        return out;
    }

    /**
     * Returns the number of peak buckets currently collected.
     */
    public synchronized int getBucketCount() {
        return buckets.size();
    }

    public int getChannels() {
        return channels;
    }

    public float getSampleRate() {
        return sampleRate;
    }

    public int getFramesPerBucket() {
        return framesPerBucket;
    }

    /**
     * Returns an estimated duration based on collected bucket count.
     *
     * This is most useful while background peak generation is still in progress.
     */
    public long getDurationMillisEstimate() {
        return (long) ((getBucketCount() * (double) framesPerBucket / sampleRate) * 1000d);
    }

    public boolean isComplete() {
        return complete;
    }

    public void setComplete(boolean complete) {
        this.complete = complete;
    }
}

