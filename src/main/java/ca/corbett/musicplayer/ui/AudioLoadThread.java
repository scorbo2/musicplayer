package ca.corbett.musicplayer.ui;

import ca.corbett.extras.MessageUtil;
import ca.corbett.extras.progress.MultiProgressWorker;
import ca.corbett.musicplayer.audio.AudioData;

import java.io.File;
import java.util.function.BooleanSupplier;
import java.util.logging.Logger;

/**
 * A simple worker that performs lightweight track loading for a given file.
 *
 * The current implementation only parses metadata and creates an {@link AudioData}
 * wrapper. Waveform peak extraction is performed later on a separate background
 * thread managed by {@link AudioLoadCoordinator}.
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
        fireProgressBegins(1);
        fireMajorProgressUpdate(1, 1, "Loading " + sourceFile.getName() + "...");
        fireMinorProgressUpdate(1, 0, "Reading track metadata...");

        checkCanceled();
        AudioData audioData = new AudioData(sourceFile);
        fireMinorProgressUpdate(1, 1, "Preparing waveform generation...");
        checkCanceled();
        return audioData;
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
