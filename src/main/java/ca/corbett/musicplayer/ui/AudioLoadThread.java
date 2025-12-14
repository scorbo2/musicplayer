package ca.corbett.musicplayer.ui;

import ca.corbett.extras.MessageUtil;
import ca.corbett.extras.progress.MultiProgressWorker;
import ca.corbett.musicplayer.audio.AudioData;
import ca.corbett.musicplayer.audio.AudioUtil;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * A simple worker thread to load audio data from a given file.
 * Upon completion, the audio data will be loaded into the audio
 * panel and played automatically. If something goes wrong, an
 * error will be logged and displayed to the user.
 * A progress monitor is supplied so the user has some idea
 * that work is being done.
 *
 * @author scorbo2
 * @since 2025-04-02
 */
public class AudioLoadThread extends MultiProgressWorker {

    private static final Logger logger = Logger.getLogger(AudioLoadThread.class.getName());
    private final File sourceFile;
    private MessageUtil messageUtil;

    public AudioLoadThread(File inputFile) {
        sourceFile = inputFile;
    }

    @Override
    public void run() {
        try {
            fireProgressBegins(2);
            fireMajorProgressUpdate(1, 3, "Loading " + sourceFile.getName() + "...");
            fireMinorProgressUpdate(1, 0, "Converting audio...");

            // Convert if necessary from mp3 to wav:
            File convertedFile = null;
            if (sourceFile.getName().toLowerCase().endsWith(".mp3")) {
                AudioInputStream sourceStream = AudioSystem.getAudioInputStream(sourceFile);
                convertedFile = AudioUtil.convert(sourceStream);
                if (convertedFile == null) {
                    throw new IOException("Decode mp3 failed!");
                }
            }

            fireMinorProgressUpdate(1, 1, "Parsing converted data...");

            // Now load this audio stream:
            AudioData audioData = AudioUtil.load(sourceFile, convertedFile);

            fireMinorProgressUpdate(1, 2, "Processing audio...");

            // If we get this far, set the audio data and play it:
            AudioPanel.getInstance().setAudioData(audioData);
            fireProgressComplete();
            AudioPanel.getInstance().play();
        } catch (Exception e) {
            getMessageUtil().error("Problem loading file: " + e.getMessage(), e);
        }
    }

    private MessageUtil getMessageUtil() {
        if (messageUtil == null) {
            messageUtil = new MessageUtil(MainWindow.getInstance(), logger);
        }
        return messageUtil;
    }
}
