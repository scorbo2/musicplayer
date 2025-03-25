package ca.corbett.musicplayer;

import ca.corbett.extensions.AppProperties;
import ca.corbett.extras.audio.WaveformConfig;
import ca.corbett.extras.properties.AbstractProperty;
import ca.corbett.extras.properties.ColorProperty;
import ca.corbett.extras.properties.ComboProperty;
import ca.corbett.extras.properties.IntegerProperty;
import ca.corbett.musicplayer.extensions.MusicPlayerExtension;
import ca.corbett.musicplayer.extensions.MusicPlayerExtensionManager;
import ca.corbett.musicplayer.ui.AudioPanel;
import ca.corbett.musicplayer.ui.ControlPanel;
import ca.corbett.musicplayer.ui.Playlist;
import ca.corbett.musicplayer.ui.PlaylistTheme;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Taking advantage of the AppProperties helper class in my own
 * application-extensions library, as it saves a LOT of work in
 * managing application properties and exposing them to the user.
 * <p>
 * The persistence file for application properties defaults
 * to "MusicPlayer.props" in the user's home directory.
 * You can override this by setting the
 * ca.corbett.musicplayer.props.file system property.
 * </p>
 * <p>
 * <B>Example:</B> java -Dca.corbett.musicplayer.props.file=/tmp/blah.props MusicPlayer.jar
 * </p>
 *
 * @author scorbo2
 * @since 2025-03-23
 */
public class AppConfig extends AppProperties<MusicPlayerExtension> {

    private static AppConfig instance;
    public static final File PROPS_FILE;

    private ComboProperty buttonSize;
    private ComboProperty mediaPlayerControlAlignment;
    private ComboProperty playlistControlAlignment;
    private ColorProperty waveformBgColor;
    private ColorProperty waveformFillColor;
    private ColorProperty waveformOutlineColor;
    private IntegerProperty waveformOutlineThickness;
    private ComboProperty playlistTheme; // TODO playlist theme? or app theme?

    public enum ButtonSize {
        XSMALL(16),
        SMALL(20),
        NORMAL(24),
        LARGE(30),
        XLARGE(36);

        final private int buttonSize;

        ButtonSize(int btnSize) {
            buttonSize = btnSize;
        }

        public int getButtonSize() {
            return buttonSize;
        }

        public static ButtonSize fromString(String name) {
            for (ButtonSize size : values()) {
                if (size.name().equals(name)) {
                    return size;
                }
            }
            return LARGE; // arbitrary default in case of garbage data
        }
    }

    public enum ControlAlignment {
        LEFT, CENTER, RIGHT;

        public static ControlAlignment fromString(String name) {
            for (ControlAlignment alignment : values()) {
                if (alignment.name().equals(name)) {
                    return alignment;
                }
            }
            return CENTER; // arbitrary default in case of garbage data
        }
    }


    static {
        File f;
        if (System.getProperty("ca.corbett.musicplayer.props.file") != null) {
            f = new File(System.getProperty("ca.corbett.musicplayer.props.file"));
        } else {
            f = new File(System.getProperty("user.home"), "MusicPlayer.props");
        }
        PROPS_FILE = f;
    }

    protected AppConfig() {
        super(Version.FULL_NAME, PROPS_FILE, MusicPlayerExtensionManager.getInstance());
    }

    /**
     * Overridden here so we can update our UI elements when the user has made
     * changes in the preferences dialog.
     */
    @Override
    public void save() {
        super.save();
        ControlPanel.getInstance().rebuildControls();
        Playlist.getInstance().rebuildControls();
        AudioPanel.getInstance().regenerateWaveformImage();
    }

    public static AppConfig getInstance() {
        if (instance == null) {
            instance = new AppConfig();
        }

        return instance;
    }

    public void setButtonSize(ButtonSize newSize) {
        buttonSize.setSelectedItem(newSize.name());
    }

    public void setButtonSize(String newSize) {
        buttonSize.setSelectedItem(newSize);
    }

    public ButtonSize getButtonSize() {
        return ButtonSize.fromString(buttonSize.getSelectedItem());
    }

    public void setMediaPlayerControlAlignment(ControlAlignment alignment) {
        mediaPlayerControlAlignment.setSelectedItem(alignment.name());
    }

    public void setMediaPlayerControlAlignment(String alignment) {
        mediaPlayerControlAlignment.setSelectedItem(alignment);
    }

    public ControlAlignment getMediaPlayerControlAlignment() {
        return ControlAlignment.fromString(mediaPlayerControlAlignment.getSelectedItem());
    }

    public void setPlaylistControlAlignment(ControlAlignment alignment) {
        playlistControlAlignment.setSelectedItem(alignment.name());
    }

    public void setPlaylistControlAlignment(String alignment) {
        mediaPlayerControlAlignment.setSelectedItem(alignment);
    }

    public ControlAlignment getPlaylistControlAlignment() {
        return ControlAlignment.fromString(playlistControlAlignment.getSelectedItem());
    }

    public void setWaveformBgColor(Color color) {
        waveformBgColor.setColor(color);
    }

    public Color getWaveformBgColor() {
        return waveformBgColor.getColor();
    }

    public void setWaveformFillColor(Color color) {
        waveformFillColor.setColor(color);
    }

    public Color getWaveformFillColor() {
        return waveformFillColor.getColor();
    }

    public void setWaveformOutlineColor(Color color) {
        waveformOutlineColor.setColor(color);
    }

    public Color getWaveformOutlineColor() {
        return waveformOutlineColor.getColor();
    }

    public void setWaveformOutlineWidth(int width) {
        waveformOutlineThickness.setValue(width);
    }

    public int getWaveformOutlineWidth() {
        return waveformOutlineThickness.getValue();
    }

    public PlaylistTheme.Theme getPlaylistTheme() {
        return PlaylistTheme.getTheme(playlistTheme.getSelectedItem());
    }

    @Override
    protected List<AbstractProperty> createInternalProperties() {
        List<String> options = new ArrayList<>();
        for (ButtonSize size : ButtonSize.values()) {
            options.add(size.name());
        }
        buttonSize = new ComboProperty("UI.General.buttonSize", "Control size:", options, 3, false);

        options = new ArrayList<>();
        for (ControlAlignment alignment : ControlAlignment.values()) {
            options.add(alignment.name());
        }
        mediaPlayerControlAlignment = new ComboProperty("UI.General.mediaControlAlignment", "Media controls:", options, 1, false);
        playlistControlAlignment = new ComboProperty("UI.General.playlistControlAlignment", "Playlist controls:", options, 1, false);

        WaveformConfig defaultConfig = new WaveformConfig();
        waveformBgColor = new ColorProperty("Waveform.Waveform graphics.bgColor", "Background:", ColorProperty.ColorType.SOLID, defaultConfig.getBgColor());
        waveformFillColor = new ColorProperty("Waveform.Waveform graphics.fillColor", "Fill:", ColorProperty.ColorType.SOLID, defaultConfig.getFillColor());
        waveformOutlineColor = new ColorProperty("Waveform.Waveform graphics.outlineColor", "Outline:", ColorProperty.ColorType.SOLID, defaultConfig.getOutlineColor());
        waveformOutlineThickness = new IntegerProperty("Waveform.Waveform graphics.outlineWidth", "Outline width:", defaultConfig.getOutlineThickness(), 0, 24, 1);

        options = new ArrayList<>();
        for (PlaylistTheme.Theme theme : PlaylistTheme.getAll()) {
            options.add(theme.name);
        }
        playlistTheme = new ComboProperty("UI.Playlist.theme", "Playlist theme:", options, 1, false);

        return List.of(buttonSize, mediaPlayerControlAlignment, playlistControlAlignment, waveformBgColor, waveformFillColor, waveformOutlineColor, waveformOutlineThickness, playlistTheme);
    }
}
