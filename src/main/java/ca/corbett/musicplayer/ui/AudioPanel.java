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

public class AudioPanel extends JPanel implements UIReloadable {

    private final static Logger logger = Logger.getLogger(AudioPanel.class.getName());

    public static final int WAVEFORM_HEIGHT = 140;

    private MessageUtil messageUtil;
    private static AudioPanel instance;
    private final Timer resizeTimer;

    public enum PanelState {
        IDLE, PLAYING
    }

    private AudioData audioData;
    private PlaybackThread playbackThread;
    private float playbackPosition; // 0f==start, 1f==end
    private final PlaybackListener playbackListener;

    private float markPosition;

    private final ImagePanel imagePanel;
    private BufferedImage waveformImage;

    private JPanel controlPanelMain;

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
        playbackListener = new PlaybackListener() {
            @Override
            public void started() {
            }

            @Override
            public void stopped() {
                setPlaybackPosition(0);
                panelState = PanelState.IDLE;
                fireStateChangedEvent();
            }

            @Override
            public boolean updateProgress(long curMillis, long totalMillis) {
                //System.out.println("progress("+curMillis+","+totalMillis+") = "+((float)curMillis/(float)totalMillis));
                setPlaybackPosition((float) curMillis / (float) totalMillis);
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
     * @param image
     */
    void setIdleImage(BufferedImage image) {
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

    public void setAudioData(AudioData data) {
        if (data == null) {
            stop();
            audioData = null;
            return;
        }

        AudioPanelIdleAnimation.stop();
        if (panelState != PanelState.IDLE) {
            stop();
        }
        waveformImage = data.getWaveformImage();
        audioData = data;
        markPosition = 0f;
        playbackPosition = 0f;
        redrawWaveform();
        NowPlayingPanel.getInstance().setNowPlaying(data);
        fireAudioLoadedEvent();
    }

    /**
     * If an audio clip is already loaded into the AudioPanel, this method will play it.
     * If the audio panel has nothing, we'll ask the Playlist for whatever is selected,
     * and then load and play that. If the playlist is empty, then this does nothing.
     */
    public void play() {

        // Ignore this call if our state is anything other than IDLE:
        if (panelState != PanelState.IDLE) {
            return;
        }

        // If we already have AudioData loaded, just play it.
        // Otherwise, ask our playlist for whatever is selected:
        if (audioData == null) {
            setAudioData(Playlist.getInstance().getSelected());
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

        try {
            panelState = PanelState.PLAYING;
            fireStateChangedEvent();
            playbackThread = AudioUtil.play(audioData, startOffset, playbackListener);
        } catch (IOException | LineUnavailableException exc) {
            getMessageUtil().error("Playback error", "Problem playing audio: " + exc.getMessage(), exc);
            playbackThread = null;
            panelState = PanelState.IDLE;
            fireStateChangedEvent();
        }
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
     * Stops playing, if playback was in progress, or does nothing if it wasn't.
     * This can be invoked programmatically, and is also invoked from the stop button,
     * if the control panel is visible and the user clicks it.
     */
    public void stop() {
        if (panelState == PanelState.PLAYING) {
            playbackThread.stop();
            playbackThread = null;
        }

        panelState = PanelState.IDLE;
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
            playbackPosition = pos;
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
        // Ignore this click if we're not idle or if we have no audio data:
        if (panelState != PanelState.IDLE || audioData == null) {
            return;
        }

        // Clear previous mark point or selection:
        markPosition = 0f;

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

    private MessageUtil getMessageUtil() {
        if (messageUtil == null) {
            messageUtil = new MessageUtil(this, logger);
        }
        return messageUtil;
    }
}
