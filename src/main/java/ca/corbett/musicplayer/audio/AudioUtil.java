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
 * TODO this is a copy+paste from swing-extras so I can muck with it.
 * If these changes prove useful, backport them to swing-extras.
 * But if it diverges too much and gets too specific, maybe just keep it here.
 */
public class AudioUtil {

    private static final Logger logger = Logger.getLogger(AudioUtil.class.getName());

    private AudioUtil() {
    }

    public static AudioData load(File sourceFile) throws UnsupportedAudioFileException, IOException {
        boolean wasConverted = false;
        if (sourceFile.getName().toLowerCase().endsWith(".mp3")) {
            AudioInputStream sourceStream = AudioSystem.getAudioInputStream(sourceFile);
            sourceFile = convert(sourceStream);
            if (sourceFile == null) {
                throw new IOException("Decode mp3 failed!");
            }
            wasConverted = true;
        }
        AudioInputStream inputStream = AudioSystem.getAudioInputStream(sourceFile);

        int frameLength = (int) inputStream.getFrameLength();
        int frameSize = (int) inputStream.getFormat().getFrameSize();

        // Odd NegativeArraySize exception can be thrown below, see AREC-10:
        if (frameLength < 0 || frameSize < 0 || (frameLength * frameSize) < 0) {
            throw new IOException("Empty or corrupt Audio stream.");
        }

        byte[] byteArray = new byte[frameLength * frameSize];
        int result = inputStream.read(byteArray);

        if (wasConverted) {
            sourceFile.delete();
        }

        // Now split that byte array into channels and proper samples (assuming 16 bit samples here):
        int numChannels = inputStream.getFormat().getChannels();
        int[][] audioData = new int[numChannels][frameLength];
        int sampleIndex = 0;
        for (int t = 0; t < byteArray.length; ) {
            for (int channel = 0; channel < numChannels; channel++) {
                int lowByte = (int) byteArray[t++];
                int highByte = (int) byteArray[t++];
                int sample = (highByte << 8) | (lowByte & 0x00ff);
                audioData[channel][sampleIndex] = sample;
            }
            sampleIndex++;
        }

        inputStream.close();

        return new AudioData(audioData, sourceFile);
    }

    public static PlaybackThread play(AudioData data, PlaybackListener listener) throws IOException, LineUnavailableException {
        return play(data, 0, listener);
    }

    public static PlaybackThread play(AudioData data, long offset, PlaybackListener listener) throws IOException, LineUnavailableException {
        AudioInputStream audioStream = getAudioInputStream(data.getRawData());
        PlaybackThread thread = new PlaybackThread(audioStream, offset, 0, listener);
        new Thread(thread).start();
        return thread;
    }

    /**
     * Constructs an AudioInputStream based on the parsed audio data. This is mainly used
     * to play a clip that has been parsed by one of the parseAudio methods in this class.
     *
     * @param audioData Audio data as parsed by one of the parseAudio methods in this class.
     * @return An AudioInputStream ready to be read.
     */
    public static AudioInputStream getAudioInputStream(int[][] audioData) {
        // Audio saving code cobbled together from
        //  https://stackoverflow.com/questions/3297749/java-reading-manipulating-and-writing-wav-files

        // Convert the int array into a single byte array:
        byte[] byteArray = new byte[audioData[0].length * audioData.length * 2];
        int sample = 0;
        for (int i = 0; i < byteArray.length; ) {
            for (int channel = 0; channel < audioData.length; channel++) {
                byteArray[i++] = (byte) (audioData[channel][sample] & 0xff);
                byteArray[i++] = (byte) (audioData[channel][sample] >>> 8);
            }
            sample++;
        }

        AudioFormat format = new AudioFormat(44100f, 16, audioData.length, true, false);
        ByteArrayInputStream bais = new ByteArrayInputStream(byteArray);
        AudioInputStream audioStream = new AudioInputStream(bais, format, audioData[0].length);
        return audioStream;
    }

    private static File convert(AudioInputStream inStream) throws IOException {
        AudioFormat targetFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100f, 16, 2, 4, 1f, false);
        if (!AudioSystem.isConversionSupported(targetFormat, inStream.getFormat())) {
            System.out.println("nope");
            return null;
        }
        AudioInputStream convertedStream = AudioSystem.getAudioInputStream(targetFormat, inStream);
        File tmpFile = File.createTempFile("mp_", ".wav");
        AudioSystem.write(convertedStream, AudioFileFormat.Type.WAVE, tmpFile);
        return tmpFile;
    }
}
