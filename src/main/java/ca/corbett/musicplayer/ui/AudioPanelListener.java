package ca.corbett.musicplayer.ui;

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
     * Indicates that an audio clip has been loaded into the panel.
     *
     * @param sourcePanel The AudioPanel that triggered this event.
     * @param trackInfo Contains information about the track that was loaded.
     */
    public void audioLoaded(AudioPanel sourcePanel, VisualizationTrackInfo trackInfo);

}
