package ca.corbett.musicplayer.audio;

import ca.corbett.extras.audio.PlaybackListener;
import ca.corbett.extras.audio.PlaybackThread;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;

/**
 * Utility helpers for audio-file validation and streaming playback.
 *
 * The active pipeline is source-file based: metadata is loaded separately and
 * playback streams decoded PCM directly from the source file.
 *
 * @author scorbo2
 * @since 2025-03-23
 */
public class AudioUtil {

    private AudioUtil() {
    }

    /**
     * Returns true if the given File exists, is readable, and has an extension
     * compatible with our load code. (Currently, this means wav or mp3).
     */
    public static boolean isValidAudioFile(File candidate) {
        if (candidate == null) {
            return false;
        }
        String filename = candidate.getName().toLowerCase();
        return candidate.exists()
            && candidate.isFile()
            && candidate.canRead() &&
            (filename.endsWith(".mp3") || filename.endsWith(".wav"));
    }

    /**
     * Returns true if the given File exists, is readable, and has an extension
     * that indicates it's a MusicPlayer playlist. (mplist)
     */
    public static boolean isValidPlaylist(File candidate) {
        if (candidate == null) {
            return false;
        }
        String filename = candidate.getName().toLowerCase();
        return candidate.exists()
            && candidate.isFile()
            && candidate.canRead() &&
            filename.endsWith(".mplist");
    }

    public static PlaybackThread play(AudioData data, PlaybackListener listener) throws IOException, LineUnavailableException {
        return play(data, 0, listener);
    }

    /**
     * Starts playback from the given track at the requested millisecond offset.
     *
     * Playback is streamed from the source file and decoded to PCM on demand.
     *
     * @param data     Track wrapper with a valid source file.
     * @param offset   Start offset in milliseconds.
     * @param listener Playback callback listener.
     * @return The running playback thread.
     */
    public static PlaybackThread play(AudioData data, long offset, PlaybackListener listener) throws IOException, LineUnavailableException {
        if (data == null || data.getSourceFile() == null) {
            throw new IOException("No audio source file is available for playback.");
        }

        AudioInputStream audioStream = openPlaybackStream(data.getSourceFile());
        PlaybackThread thread = new PlaybackThread(audioStream, offset, 0, listener);
        new Thread(thread).start();
        return thread;
    }

    /**
     * Opens an audio stream suitable for immediate playback from the given source file.
     * For mp3 files, we decode to 16-bit PCM on the fly using the installed Java Sound SPI.
     * For already-playable 16-bit signed PCM files, this returns the source stream.
     *
     * @param sourceFile Audio file to open.
     * @return A stream in a playback-compatible PCM format.
     */
    public static AudioInputStream openPlaybackStream(File sourceFile) throws IOException {
        AudioInputStream sourceStream;
        try {
            sourceStream = AudioSystem.getAudioInputStream(sourceFile);
        }
        catch (UnsupportedAudioFileException e) {
            throw new IOException("Unsupported source audio file: " + sourceFile.getName(), e);
        }
        AudioFormat sourceFormat = sourceStream.getFormat();

        // Keep playback format consistent for mp3/wav by targeting signed 16-bit little-endian PCM.
        AudioFormat targetFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                                                   sourceFormat.getSampleRate(),
                                                   16,
                                                   sourceFormat.getChannels(),
                                                   sourceFormat.getChannels() * 2,
                                                   sourceFormat.getSampleRate(),
                                                   false);

        if (AudioSystem.isConversionSupported(targetFormat, sourceFormat)) {
            return AudioSystem.getAudioInputStream(targetFormat, sourceStream);
        }

        // If conversion is unsupported but the stream is already in a sensible playback format,
        // return it as-is instead of failing fast.
        if (sourceFormat.getEncoding() == AudioFormat.Encoding.PCM_SIGNED
            && sourceFormat.getSampleSizeInBits() == 16
            && sourceFormat.getChannels() > 0) {
            return sourceStream;
        }

        sourceStream.close();
        throw new IOException("Unsupported playback format: " + sourceFormat);
    }
}
