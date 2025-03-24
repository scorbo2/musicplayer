package ca.corbett.musicplayer.ui;

import ca.corbett.extras.audio.AudioWaveformPanel;

public interface AudioPanelListener {
    /**
     * Indicates a state change within the panel.
     * <ul>
     * <li><b>IDLE</b> - the panel is neither playing nor recording.
     * <li><b>PLAYING</b> - the panel is playing audio.
     * </ul>
     *
     * @param sourcePanel The AudioPanel that triggered this event.
     * @param state The new state of the panel.
     */
    public void stateChanged(AudioPanel sourcePanel, AudioPanel.PanelState state);

    /**
     * Indicates that an audio clip has been loaded into the panel. This event is also triggered
     * after recording a clip, as the newly recorded clip is loaded into the panel.
     *
     * @param sourcePanel The AudioPanel that triggered this event.
     */
    public void audioLoaded(AudioPanel sourcePanel);

}
