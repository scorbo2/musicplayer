package ca.corbett.musicplayer.ui;

import ca.corbett.extras.MessageUtil;
import ca.corbett.musicplayer.audio.AudioData;

import javax.swing.SwingUtilities;
import java.io.File;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Coordinates track load requests so that only the most recent request is allowed
 * to affect the UI. Requests are serialized onto a single background worker and
 * rapid repeated requests are coalesced down to the latest pending file.
 *
 * @author scorbo2
 * @since 2026-04-11
 */
public final class AudioLoadCoordinator {

    private static final Logger logger = Logger.getLogger(AudioLoadCoordinator.class.getName());
    private static AudioLoadCoordinator instance;

    private final AtomicLong requestCounter = new AtomicLong(0);
    private final AtomicLong latestRequestId = new AtomicLong(0);
    private final Object requestLock = new Object();
    private final Thread workerThread;
    private volatile LoadRequest pendingRequest;
    private volatile boolean running = true;
    private MessageUtil messageUtil;

    private AudioLoadCoordinator() {
        workerThread = new Thread(this::processRequests, "musicplayer-audio-loader");
        workerThread.setDaemon(true);
        workerThread.start();
    }

    public static synchronized AudioLoadCoordinator getInstance() {
        if (instance == null) {
            instance = new AudioLoadCoordinator();
        }
        return instance;
    }

    /**
     * Queues a request to load the given file. If another request is already pending,
     * it is replaced by this one.
     *
     * @param sourceFile The audio file to load.
     * @return The request id assigned to this request.
     */
    public long requestLoad(File sourceFile) {
        if (sourceFile == null) {
            return latestRequestId.get();
        }

        long requestId = requestCounter.incrementAndGet();
        latestRequestId.set(requestId);
        synchronized (requestLock) {
            pendingRequest = new LoadRequest(requestId, sourceFile);
            requestLock.notifyAll();
        }
        return requestId;
    }

    /**
     * Invalidates any current or pending load request so late results are ignored.
     * This is useful when the user hits stop while a background load is still running.
     */
    public void cancelPendingRequests() {
        latestRequestId.incrementAndGet();
        synchronized (requestLock) {
            pendingRequest = null;
            requestLock.notifyAll();
        }
        workerThread.interrupt();
    }

    public long getLatestRequestId() {
        return latestRequestId.get();
    }

    public boolean isCurrentRequest(long requestId) {
        return requestId > 0 && latestRequestId.get() == requestId;
    }

    public void shutdown() {
        running = false;
        synchronized (requestLock) {
            pendingRequest = null;
            requestLock.notifyAll();
        }
        workerThread.interrupt();
    }

    private void processRequests() {
        while (running) {
            LoadRequest request = waitForNextRequest();
            if (!running || request == null) {
                continue;
            }

            if (!isCurrentRequest(request.requestId)) {
                continue;
            }

            AudioLoadThread loader = new AudioLoadThread(request.sourceFile, () -> running && isCurrentRequest(request.requestId));
            try {
                AudioData audioData = loader.loadAudioData();
                if (audioData == null || !isCurrentRequest(request.requestId)) {
                    continue;
                }

                SwingUtilities.invokeLater(() -> {
                    if (!isCurrentRequest(request.requestId)) {
                        return;
                    }

                    AudioPanel panel = AudioPanel.getInstance();
                    if (panel.applyLoadedAudioData(request.requestId, audioData)) {
                        panel.playRequest(request.requestId);
                    }
                });
            }
            catch (InterruptedException ignored) {
                Thread.interrupted();
            }
            catch (Exception exc) {
                if (!isCurrentRequest(request.requestId)) {
                    logger.log(Level.FINE, "Ignoring stale audio load failure for {0}: {1}",
                               new Object[]{request.sourceFile.getName(), exc.getMessage()});
                    continue;
                }
                getMessageUtil().error("Problem loading file: " + exc.getMessage(), exc);
            }
        }
    }

    private LoadRequest waitForNextRequest() {
        synchronized (requestLock) {
            while (running && pendingRequest == null) {
                try {
                    requestLock.wait();
                }
                catch (InterruptedException ignored) {
                    Thread.interrupted();
                }
            }
            if (!running) {
                return null;
            }

            LoadRequest request = pendingRequest;
            pendingRequest = null;
            return request;
        }
    }

    private MessageUtil getMessageUtil() {
        if (messageUtil == null) {
            messageUtil = new MessageUtil(MainWindow.getInstance(), logger);
        }
        return messageUtil;
    }

    private record LoadRequest(long requestId, File sourceFile) {
        private LoadRequest {
            Objects.requireNonNull(sourceFile);
        }
    }
}
