package ca.corbett.musicplayer.ui;

import ca.corbett.extras.MessageUtil;
import ca.corbett.extras.audio.PlaybackListener;
import ca.corbett.extras.audio.PlaybackThread;
import ca.corbett.extras.image.ImagePanel;
import ca.corbett.extras.image.ImagePanelConfig;
import ca.corbett.musicplayer.actions.ReloadUIAction;
import ca.corbett.musicplayer.audio.AudioData;
import ca.corbett.musicplayer.audio.AudioUtil;

import javax.sound.sampled.LineUnavailableException;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * TODO saw an intermittent issue where if you set a manual mark position towards the
 *      very end of the clip and hit play, it won't play right to the end.
 *      Not sure, but seems like the playback thread might be miscalculating offset position.
 */
public class AudioPanel extends JPanel implements UIReloadable {

    private final static Logger logger = Logger.getLogger(AudioPanel.class.getName());

    public static final int WAVEFORM_HEIGHT = 140;

    private MessageUtil messageUtil;
    private static AudioPanel instance;
    private final Timer resizeTimer;

    public enum PanelState {
        IDLE, PLAYING, PAUSED
    }

    private AudioData audioData;
    private PlaybackThread playbackThread;
    private float playbackPosition; // 0f==start, 1f==end
    private final PlaybackListener playbackListener;
    private final VisualizationTrackInfo trackInfo;

    private float markPosition;

    private final ImagePanel imagePanel;
    private BufferedImage waveformImage;

    private final List<AudioPanelListener> panelListeners;
    private PanelState panelState;

    private AudioPanel() {
        // Create and configure our image panel properties.
        // These settings are strictly internal and cannot be overridden by clients:
        ImagePanelConfig imagePanelProperties = ImagePanelConfig.createDefaultProperties();
        imagePanelProperties.setDisplayMode(ImagePanelConfig.DisplayMode.STRETCH);
        imagePanelProperties.setEnableMouseDragging(false);
        imagePanelProperties.setEnableZoomOnMouseClick(false);
        imagePanelProperties.setEnableZoomOnMouseWheel(false);
        imagePanel = new ImagePanel((BufferedImage) null, imagePanelProperties);
        imagePanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleImagePanelClick(e);
            }
        });
        imagePanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                resizeTimer.restart();
            }
        });

        resizeTimer = new Timer(100, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                redrawWaveform();
            }
        });
        resizeTimer.setRepeats(false);
        resizeTimer.setCoalesce(false);

        // Misc:
        setPreferredSize(new Dimension(400, WAVEFORM_HEIGHT));
        panelState = PanelState.IDLE;
        playbackPosition = 0f;
        markPosition = 0f;
        trackInfo = new VisualizationTrackInfo();
        trackInfo.reset();
        playbackListener = new PlaybackListener() {
            @Override
            public void started() {
            }

            @Override
            public void stopped() {
                // If we stopped because we ran out of audio data, return to IDLE state.
                // Otherwise, our state has already been explicitly updated by one of our own actions.
                if (panelState == PanelState.PLAYING) {
                    panelState = PanelState.IDLE;
                }
                VisualizationWindow.getInstance().setTrackInfo(null, null);
                fireStateChangedEvent();
            }

            @Override
            public boolean updateProgress(long curMillis, long totalMillis) {
                setPlaybackPosition((float) curMillis / (float) totalMillis);
                trackInfo.currentTime = (int) (curMillis / 1000);
                VisualizationWindow.getInstance().setTrackInfo(trackInfo, audioData.getSourceFile());
                return true;
            }

        };
        panelListeners = new ArrayList<>();

        // Lay out the UI:
        initComponents();

        // Register to receive reloadUI events:
        ReloadUIAction.getInstance().registerReloadable(this);
    }

    public static AudioPanel getInstance() {
        if (instance == null) {
            instance = new AudioPanel();
        }

        return instance;
    }

    /**
     * Invoked from AudioPanelIdleAnimation
     *
     * @param image An image to set in this panel.
     */
    public void setIdleImage(BufferedImage image) {
        imagePanel.setImage(image);
    }

    /**
     * Registers an AudioPanelListener to receive events from this panel.
     *
     * @param listener The new listener to register.
     */
    public void addAudioPanelListener(AudioPanelListener listener) {
        panelListeners.add(listener);
    }

    /**
     * Unregisters a listener from this panel.
     *
     * @param listener The listener to unregister.
     */
    public void removeAudioPanelListener(AudioPanelListener listener) {
        panelListeners.remove(listener);
    }

    public AudioData getAudioData() {
        return audioData;
    }

    public void setAudioData(AudioData data) {
        if (data == null) {
            stop();
            audioData = null;
            trackInfo.reset();
            AudioPanelIdleAnimation.getInstance().go();
            return;
        }

        AudioPanelIdleAnimation.getInstance().stop();
        if (panelState != PanelState.IDLE) {
            stop();
        }
        waveformImage = data.getWaveformImage();
        audioData = data;
        markPosition = 0f;
        playbackPosition = 0f;
        redrawWaveform();
        NowPlayingPanel.getInstance().setNowPlaying(data);
        trackInfo.artist = data.getMetadata().author;
        trackInfo.title = data.getMetadata().title;
        trackInfo.album = data.getMetadata().album;
        trackInfo.currentTime = 0;
        trackInfo.totalTime = 0; // TODO sort out duration -> data.getMetadata().durationStr;
        fireAudioLoadedEvent();
    }

    /**
     * If an audio clip is already loaded into the AudioPanel, this method will play it.
     * If the audio panel has nothing, we'll ask the Playlist for whatever is selected,
     * and then load and play that. If the playlist is empty, then this does nothing.
     */
    public void play() {

        // If we're already playing, just ignore this:
        if (panelState == PanelState.PLAYING) {
            return;
        }

        // If we're paused, we can resume:
        if (panelState == PanelState.PAUSED) {
            pause(); // hitting pause() again while paused is treated as a "resume"
            return;
        }

        // If we don't already have audio data loaded, ask our playlist
        // for whatever is currently selected:
        if (audioData == null) {
            AudioData data = Playlist.getInstance().getSelected();
            if (data == null) {
                data = Playlist.getInstance().getNext();
            }
            setAudioData(data);
        }

        // If it's still null, we got nothing, so there's nothing to play:
        if (audioData == null) {
            return;
        }

        // Set starting offset if set:
        long startOffset = 0;
        if (markPosition > 0f) {
            startOffset = (long) (markPosition * (audioData.getRawData()[0].length / 44.1f)); // WARNING assuming bit rate
        }

        internalPlay(startOffset);
    }

    public void next() {
        setAudioData(Playlist.getInstance().getNext());
        if (audioData != null) {
            play();
        }
    }

    public void prev() {
        setAudioData(Playlist.getInstance().getPrev());
        if (audioData != null) {
            play();
        }
    }

    /**
     * Pauses playing, if playback was in progress, or unpauses it if it was paused.
     * In the paused state, you can also resume by calling play().
     * <p>
     * Implementation note: if we're playing, all this method really does is update
     * the markPosition to the latest play position of the playback thread. Then
     * we stop and delete the playback thread. When we "resume" later, we're really
     * creating a new playback thread and giving it the markPosition from the last one.
     * But while the panel is in the paused state, the user can left click anywhere
     * on our waveform to move the mark position somewhere else. In that case, we
     * will "resume" from that mark position and not from the last play position
     * of the previous thread. This is a deliberate choice on my part as I find
     * it to be the least surprising behaviour. Basically we always want to "resume"
     * from the mark position, whether it was explicitly set by the user or
     * calculated by us when the pause button was hit.
     * </p>
     */
    public void pause() {
        // If we were already paused, treat this as "resume":
        if (panelState == PanelState.PAUSED) {
            long startOffset = (long) (markPosition * (audioData.getRawData()[0].length / 44.1f)); // WARNING assuming bit rate
            internalPlay(startOffset);
            return;
        }

        // If we were playing, treat this as a "pause":
        if (panelState == PanelState.PLAYING) {
            panelState = PanelState.PAUSED;
            playbackThread.stop();
            markPosition = (float) playbackThread.getCurrentOffset() / (audioData.getRawData()[0].length / 44.1f); // TODO assuming bitrate
            playbackThread = null;
            redrawWaveform();
        }
    }

    /**
     * Stops playing, if playback was in progress, or does nothing if it wasn't.
     * This can be invoked programmatically, and is also invoked from the stop button,
     * if the control panel is visible and the user clicks it.
     */
    public void stop() {
        if (playbackThread != null) {
            playbackThread.stop();
            playbackThread = null;
        }

        panelState = PanelState.IDLE;
        setPlaybackPosition(0);
        markPosition = 0f; // arbitrary decision - "stop" should clear any current mark position
        redrawWaveform();
        fireStateChangedEvent();
    }

    /**
     * Returns the current state of this panel.
     * <ul>
     * <li><b>IDLE</b> - neither playing nor recording.
     * <li><b>PLAYING</b> - currently playing audio.
     * <li><b>RECORDING</b> - currently recording audio.
     * </ul>
     *
     * @return One of the PanelState enum values as described above.
     */
    public PanelState getPanelState() {
        return panelState;
    }


    /**
     * Returns the pixel dimensions of the current audio waveform image, if there is one, or
     * null otherwise.
     *
     * @return A Dimension object representing the size of the waveform image, or null.
     */
    public Dimension getWaveformDimensions() {
        Dimension dim = null;

        if (imagePanel.getImage() != null) {
            dim = new Dimension(imagePanel.getImage().getWidth(), imagePanel.getImage().getHeight());
        }

        return dim;
    }

    /**
     * Invoked internally to lay out all UI components.
     */
    private void initComponents() {
        setLayout(new BorderLayout());
        add(imagePanel, BorderLayout.CENTER);
        add(NowPlayingPanel.getInstance(), BorderLayout.NORTH);
    }

    /**
     * Renders a visible vertical tracking line overtop of the current waveform to indicate
     * the current playback position. If the playback position is 0, this will simply render
     * the waveform itself with no tracking line.
     *
     * @param pos From 0 - 1, indicating the percentage of the image width (eg. 0.5 == middle).
     */
    private void setPlaybackPosition(float pos) {
        // If we have no waveform or audio data, reset to 0 and we're done:
        if (waveformImage == null || audioData == null) {
            playbackPosition = 0f;
            return;
        }

        // Keep it in range:
        playbackPosition = (pos < 0f) ? 0 : Math.min(pos, 1f);

        redrawWaveform();
    }

    /**
     * Clears the current waveform image.
     */
    public void clear() {
        if (panelState != PanelState.IDLE) {
            stop();
        }
        audioData = null;
        waveformImage = null;
        redrawWaveform();
    }

    @Override
    public void reloadUI() {
        regenerateWaveformImage();
    }

    public void regenerateWaveformImage() {
        if (panelState != PanelState.IDLE) {
            stop();
        }
        if (audioData == null) {
            return;
        }

        waveformImage = audioData.regenerateWaveformImage(); // force redraw
        redrawWaveform();
    }

    /**
     * Re-draws the current waveform, if there is one, or clears the panel if not.
     * If you want to force a recalculation of the waveform image, use regenerateWaveform instead.
     */
    private void redrawWaveform() {
        // If we have no waveform image, we're done here:
        if (waveformImage == null || audioData == null) {
            imagePanel.setImage(null);
            return;
        }

        BufferedImage buf = new BufferedImage(imagePanel.getWidth(),
                imagePanel.getHeight(),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = (Graphics2D) buf.createGraphics();
        graphics.drawImage(waveformImage, 0, 0, buf.getWidth(), buf.getHeight(), null);

        // Add the playback position if not at the extreme edges:
        if (playbackPosition > 0.01f && playbackPosition < 0.99f) {
            int x = (int) (buf.getWidth() * playbackPosition);
            graphics.setColor(Color.RED);
            graphics.setXORMode(Color.GREEN);
            graphics.drawLine(x, 0, x, buf.getHeight());
            graphics.drawLine(x + 1, 0, x + 1, buf.getHeight());
        }

        // Draw the marker position if any:
        if (markPosition > 0f) {
            int x = (int) (imagePanel.getWidth() * markPosition);
            graphics.setColor(Color.BLACK);
            graphics.setXORMode(Color.WHITE);
            graphics.drawLine(x, 0, x, imagePanel.getHeight());
        }

        graphics.dispose();
        imagePanel.setImage(buf);
    }

    private void handleImagePanelClick(MouseEvent e) {
        // Ignore this click if we're currently playing or if we have no audio data:
        if (panelState == PanelState.PLAYING || audioData == null) {
            return;
        }

        // If we're not paused, you can right click to clear the current marker:
        // (if we are paused, we want to resume play from that marker)
        if (panelState != PanelState.PAUSED) {
            // Clear previous mark point or selection:
            markPosition = 0f;
        }

        // If it was a left click, set the new mark point:
        if (e.getButton() == MouseEvent.BUTTON1) {
            markPosition = e.getX() / (float) imagePanel.getWidth();
        }

        // Redraw with these settings:
        redrawWaveform();
    }

    /**
     * Notifies all listeners that an audio clip has been loaded into this panel.
     */
    private void fireAudioLoadedEvent() {
        for (AudioPanelListener listener : panelListeners) {
            listener.audioLoaded(null);
        }
    }

    /**
     * Notifies all listeners that our state has changed.
     */
    private void fireStateChangedEvent() {
        for (AudioPanelListener listener : panelListeners) {
            listener.stateChanged(this, panelState);
        }
    }

    /**
     * Invoked internally when playing or resuming play from a paused state.
     *
     * @param startOffset The offset from which to begin playing.
     */
    private void internalPlay(long startOffset) {
        try {
            panelState = PanelState.PLAYING;
            playbackThread = AudioUtil.play(audioData, startOffset, playbackListener);
            fireStateChangedEvent();
        } catch (IOException | LineUnavailableException exc) {
            getMessageUtil().error("Playback error", "Problem playing audio: " + exc.getMessage(), exc);
            playbackThread = null;
            panelState = PanelState.IDLE;
            fireStateChangedEvent();
        }
    }

    private MessageUtil getMessageUtil() {
        if (messageUtil == null) {
            messageUtil = new MessageUtil(this, logger);
        }
        return messageUtil;
    }
}
