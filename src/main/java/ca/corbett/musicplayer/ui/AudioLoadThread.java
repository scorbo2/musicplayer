package ca.corbett.musicplayer.ui;

import ca.corbett.extras.MessageUtil;
import ca.corbett.extras.progress.MultiProgressWorker;
import ca.corbett.musicplayer.audio.AudioData;
import ca.corbett.musicplayer.audio.AudioUtil;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.File;
import java.io.IOException;
import java.util.function.BooleanSupplier;
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
    private final BooleanSupplier keepGoing;
    private MessageUtil messageUtil;

    public AudioLoadThread(File inputFile) {
        this(inputFile, () -> true);
    }

    public AudioLoadThread(File inputFile, BooleanSupplier keepGoing) {
        sourceFile = inputFile;
        this.keepGoing = keepGoing == null ? () -> true : keepGoing;
    }

    @Override
    public void run() {
        try {
            AudioData audioData = loadAudioData();
            if (audioData != null) {
                fireProgressComplete();
            }
        }
        catch (InterruptedException ignored) {
            Thread.interrupted();
        } catch (Exception e) {
            getMessageUtil().error("Problem loading file: " + e.getMessage(), e);
        }
    }

    public AudioData loadAudioData() throws Exception {
        fireProgressBegins(2);
        fireMajorProgressUpdate(1, 3, "Loading " + sourceFile.getName() + "...");
        fireMinorProgressUpdate(1, 0, "Converting audio...");

        checkCanceled();

        File convertedFile = null;
        try {
            // Convert if necessary from mp3 to wav:
            if (sourceFile.getName().toLowerCase().endsWith(".mp3")) {
                try (AudioInputStream sourceStream = AudioSystem.getAudioInputStream(sourceFile)) {
                    convertedFile = AudioUtil.convert(sourceStream);
                }
                if (convertedFile == null) {
                    throw new IOException("Decode mp3 failed!");
                }
            }

            fireMinorProgressUpdate(1, 1, "Parsing converted data...");
            checkCanceled();

            AudioData audioData = AudioUtil.load(sourceFile, convertedFile);
            convertedFile = null; // AudioUtil.load deletes it after successful parse.

            fireMinorProgressUpdate(1, 2, "Processing audio...");
            checkCanceled();
            return audioData;
        }
        finally {
            if (convertedFile != null && convertedFile.exists()) {
                convertedFile.delete();
            }
        }
    }

    private void checkCanceled() throws InterruptedException {
        if (Thread.currentThread().isInterrupted() || !keepGoing.getAsBoolean()) {
            throw new InterruptedException("Audio load request was cancelled.");
        }
    }

    private MessageUtil getMessageUtil() {
        if (messageUtil == null) {
            messageUtil = new MessageUtil(MainWindow.getInstance(), logger);
        }
        return messageUtil;
    }
}
