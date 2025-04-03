package ca.corbett.musicplayer.audio;

import ca.corbett.extras.audio.PlaybackListener;
import ca.corbett.extras.audio.PlaybackThread;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Provides utility methods for loading and handling AudioData.
 * This was based heavily on AudioUtil in swing-extras but modified
 * for this application.
 *
 * @author scorbo2
 * @since 2025-03-23 (copy+paste+modified from AudioUtil)
 */
public class AudioUtil {

    private static final Logger logger = Logger.getLogger(AudioUtil.class.getName());

    private AudioUtil() {
    }

    /**
     * Load an AudioData object from the given source file. This implementation is a bit goofy
     * and I'm not happy with it, but it goes like this:
     * <ol>
     *     <li>If the file is a wav file, just load the audio data and we're done.</li>
     *     <li>If it's an mp3, convert it to wav and write the wav to the system temp dir.</li>
     *     <li>Then, load the audio data from that temp file and delete the temp file.</li>
     * </ol>
     * The end result is an AudioData object containing the raw audio data. This is horribly
     * inefficient memory-wise but it allows waveform generation and the ability to click
     * in the waveform panel to start playing from whatever location, which I can't seem to
     * figure out how to do with mp3spi and/or jlayer. But I'm sure it's possible,
     * so TODO revisit this and smarten this up.
     * Also, I'm making assumptions about input wave format that probably need checking.
     *
     * TODO large file loading locks the application UI. Put it in a worker thread with a progress monitor.
     *
     * @param sourceFile The File containing the audio data. Must be in a supported format.
     * @return An instance of AudioData which will be partially populated (lazy loading on some stuff).
     * @throws UnsupportedAudioFileException Officially we support wav and mp3 but there's probably others not tested.
     * @throws IOException                   In case of i/o error.
     */
    public static AudioData load(File sourceFile) throws UnsupportedAudioFileException, IOException {
        File convertedFile = null;
        if (sourceFile.getName().toLowerCase().endsWith(".mp3")) {
            AudioInputStream sourceStream = AudioSystem.getAudioInputStream(sourceFile);
            convertedFile = convert(sourceStream);
            if (convertedFile == null) {
                throw new IOException("Decode mp3 failed!");
            }
        }
        AudioInputStream inputStream = AudioSystem.getAudioInputStream(convertedFile == null ? sourceFile : convertedFile);

        int frameLength = (int) inputStream.getFrameLength();
        int frameSize = inputStream.getFormat().getFrameSize();

        // Odd NegativeArraySize exception can be thrown below, see AREC-10:
        if (frameLength < 0 || frameSize < 0 || (frameLength * frameSize) < 0) {
            throw new IOException("Empty or corrupt Audio stream.");
        }

        byte[] byteArray = new byte[frameLength * frameSize];
        inputStream.read(byteArray);

        if (convertedFile != null) {
            convertedFile.delete();
        }

        // Now we'll split our byte array into channels and samples.
        int numChannels = inputStream.getFormat().getChannels();
        int[][] audioData = new int[numChannels][frameLength];
        int sampleIndex = 0;
        for (int t = 0; t < byteArray.length; ) {
            for (int channel = 0; channel < numChannels; channel++) {
                int lowByte = byteArray[t++];
                int highByte = byteArray[t++];
                int sample = (highByte << 8) | (lowByte & 0x00ff);
                audioData[channel][sampleIndex] = sample;
            }
            sampleIndex++;
        }

        float sampleRate = inputStream.getFormat().getSampleRate();
        inputStream.close();

        return new AudioData(audioData, sourceFile, sampleRate);
    }

    public static PlaybackThread play(AudioData data, PlaybackListener listener) throws IOException, LineUnavailableException {
        return play(data, 0, listener);
    }

    public static PlaybackThread play(AudioData data, long offset, PlaybackListener listener) throws IOException, LineUnavailableException {
        AudioInputStream audioStream = getAudioInputStream(data);
        PlaybackThread thread = new PlaybackThread(audioStream, offset, 0, listener);
        new Thread(thread).start();
        return thread;
    }

    /**
     * Constructs an AudioInputStream based on the parsed audio data. This is mainly used
     * to play a clip that has been parsed by one of the parseAudio methods in this class.
     *
     * @param audioData An AudioData instance.
     * @return An AudioInputStream ready to be read.
     */
    public static AudioInputStream getAudioInputStream(AudioData audioData) {
        // Audio saving code cobbled together from
        //  https://stackoverflow.com/questions/3297749/java-reading-manipulating-and-writing-wav-files

        // Convert the int array into a single byte array:
        int[][] sourceData = audioData.getRawData();
        byte[] byteArray = new byte[sourceData[0].length * sourceData.length * 2];
        int sample = 0;
        for (int i = 0; i < byteArray.length; ) {
            for (int channel = 0; channel < sourceData.length; channel++) {
                byteArray[i++] = (byte) (sourceData[channel][sample] & 0xff);
                byteArray[i++] = (byte) (sourceData[channel][sample] >>> 8);
            }
            sample++;
        }

        AudioFormat format = new AudioFormat(audioData.getSampleRate(), 16, sourceData.length, true, false);
        ByteArrayInputStream bais = new ByteArrayInputStream(byteArray);
        AudioInputStream audioStream = new AudioInputStream(bais, format, sourceData[0].length);
        return audioStream;
    }

    private static File convert(AudioInputStream inStream) throws IOException {
        AudioFormat sourceFormat = inStream.getFormat();
        AudioFormat targetFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, sourceFormat.getSampleRate(), 16, sourceFormat.getChannels(), 4, sourceFormat.getFrameRate(), false);
        if (!AudioSystem.isConversionSupported(targetFormat, inStream.getFormat())) {
            logger.severe("Audio conversion not possible for this track :(");
            return null;
        }
        AudioInputStream convertedStream = AudioSystem.getAudioInputStream(targetFormat, inStream);
        File tmpFile = File.createTempFile("mp_", ".wav");
        AudioSystem.write(convertedStream, AudioFileFormat.Type.WAVE, tmpFile);
        return tmpFile;
    }
}
