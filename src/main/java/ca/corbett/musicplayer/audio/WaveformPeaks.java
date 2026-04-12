package ca.corbett.musicplayer.audio;

import java.util.ArrayList;
import java.util.List;

/**
 * Compact waveform data represented as peak amplitudes per bucket.
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

