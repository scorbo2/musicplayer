package ca.corbett.musicplayer;

import ca.corbett.extensions.AppProperties;
import ca.corbett.extras.audio.WaveformConfig;
import ca.corbett.extras.properties.AbstractProperty;
import ca.corbett.extras.properties.BooleanProperty;
import ca.corbett.extras.properties.ColorProperty;
import ca.corbett.extras.properties.ComboProperty;
import ca.corbett.extras.properties.EnumProperty;
import ca.corbett.extras.properties.IntegerProperty;
import ca.corbett.musicplayer.actions.ReloadUIAction;
import ca.corbett.musicplayer.extensions.MusicPlayerExtension;
import ca.corbett.musicplayer.extensions.MusicPlayerExtensionManager;
import ca.corbett.musicplayer.ui.AppTheme;
import ca.corbett.musicplayer.ui.MainWindow;

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

    private EnumProperty<ButtonSize> buttonSize;
    private EnumProperty<ControlAlignment> controlAlignment;
    private ColorProperty waveformBgColor;
    private ColorProperty waveformFillColor;
    private ColorProperty waveformOutlineColor;
    private IntegerProperty waveformOutlineThickness;
    private EnumProperty<WaveformResolution> waveformResolution;
    private ComboProperty playlistTheme;
    private BooleanProperty shuffleEnabled;
    private BooleanProperty repeatEnabled;
    private IntegerProperty windowWidth;
    private IntegerProperty windowHeight;
    private BooleanProperty playlistVisible;

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
    }

    public enum ControlAlignment {
        LEFT, CENTER, RIGHT;
    }

    public enum WaveformResolution {
        LOW(512),
        MEDIUM(1024),
        HIGH(2048),
        SUPER_HIGH(4096);

        private final int xLimit;

        WaveformResolution(int xLimit) {
            this.xLimit = xLimit;
        }

        public int getXLimit() {
            return xLimit;
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

    @Override
    public void load() {
        super.load();
        ReloadUIAction.getInstance().actionPerformed(null);
    }

    /**
     * Overridden here so we can update our UI elements when the user has made
     * changes in the preferences dialog.
     */
    @Override
    public void save() {
        save(true);
    }

    public void saveWithoutUIReload() {
        save(false);
    }

    protected void save(boolean reloadUI) {
        super.save();
        if (reloadUI) {
            ReloadUIAction.getInstance().actionPerformed(null);
        }
    }

    public static AppConfig getInstance() {
        if (instance == null) {
            instance = new AppConfig();
        }

        return instance;
    }

    public void setButtonSize(ButtonSize newSize) {
        buttonSize.setSelectedItem(newSize);
    }

    public ButtonSize getButtonSize() {
        return buttonSize.getSelectedItem();
    }

    public void setControlAlignment(ControlAlignment alignment) {
        controlAlignment.setSelectedItem(alignment);
    }

    public ControlAlignment getControlAlignment() {
        return controlAlignment.getSelectedItem();
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

    public void setWaveformResolution(WaveformResolution resolution) {
        waveformResolution.setSelectedItem(resolution);
    }

    public WaveformResolution getWaveformResolution() {
        return waveformResolution.getSelectedItem();
    }

    public AppTheme.Theme getAppTheme() {
        return AppTheme.getTheme(playlistTheme.getSelectedItem());
    }

    public boolean isShuffleEnabled() {
        return shuffleEnabled.getValue();
    }

    public void setShuffleEnabled(boolean enabled) {
        shuffleEnabled.setValue(enabled);
    }

    public boolean isRepeatEnabled() {
        return repeatEnabled.getValue();
    }

    public void setRepeatEnabled(boolean enabled) {
        repeatEnabled.setValue(enabled);
    }

    public void setWindowWidth(int width) {
        windowWidth.setValue(width);
    }

    public int getWindowWidth() {
        return windowWidth.getValue();
    }

    public void setWindowHeight(int height) {
        windowHeight.setValue(height);
    }

    public int getWindowHeight() {
        return windowHeight.getValue();
    }

    public void setPlaylistVisible(boolean visible) {
        playlistVisible.setValue(visible);
    }

    public boolean isPlaylistVisible() {
        return playlistVisible.getValue();
    }

    @Override
    protected List<AbstractProperty> createInternalProperties() {
        buttonSize = new EnumProperty<ButtonSize>("UI.General.buttonSize", "Control size:", ButtonSize.LARGE);
        controlAlignment = new EnumProperty<ControlAlignment>("UI.General.controlAlignment", "Control alignment:", ControlAlignment.CENTER);

        WaveformConfig defaultConfig = new WaveformConfig();
        waveformBgColor = new ColorProperty("Waveform.Waveform graphics.bgColor", "Background:", ColorProperty.ColorType.SOLID, defaultConfig.getBgColor());
        waveformFillColor = new ColorProperty("Waveform.Waveform graphics.fillColor", "Fill:", ColorProperty.ColorType.SOLID, defaultConfig.getFillColor());
        waveformOutlineColor = new ColorProperty("Waveform.Waveform graphics.outlineColor", "Outline:", ColorProperty.ColorType.SOLID, defaultConfig.getOutlineColor());
        waveformOutlineThickness = new IntegerProperty("Waveform.Waveform graphics.outlineWidth", "Outline width:", defaultConfig.getOutlineThickness(), 0, 24, 1);

        List<String> options = new ArrayList<>();
        for (WaveformResolution res : WaveformResolution.values()) {
            options.add(res.name());
        }
        waveformResolution = new EnumProperty<WaveformResolution>("Waveform.Resolution.resolution", "Resolution:", WaveformResolution.HIGH);

        options = new ArrayList<>();
        for (AppTheme.Theme theme : AppTheme.getAll()) {
            options.add(theme.name);
        }
        playlistTheme = new ComboProperty("UI.Playlist.theme", "Playlist theme:", options, 1, false);

        shuffleEnabled = new BooleanProperty("hidden.props.shuffleEnabled", "shuffleEnabled", false);
        repeatEnabled = new BooleanProperty("hidden.props.repeatEnabled", "repeatEnabled", false);
        shuffleEnabled.setExposed(false);
        repeatEnabled.setExposed(false);

        windowWidth = new IntegerProperty("hidden.props.windowWidth", "windowWidth", MainWindow.DEFAULT_WIDTH);
        windowHeight = new IntegerProperty("hidden.props.windowHeight", "windowHeight", MainWindow.DEFAULT_HEIGHT);
        playlistVisible = new BooleanProperty("hidden.props.playlistVisible", "playlistVisible", false);
        windowWidth.setExposed(false);
        windowHeight.setExposed(false);
        playlistVisible.setExposed(false);

        return List.of(buttonSize,
                controlAlignment,
                waveformBgColor,
                waveformFillColor,
                waveformOutlineColor,
                waveformOutlineThickness,
                waveformResolution,
                playlistTheme,
                shuffleEnabled,
                repeatEnabled,
                windowWidth,
                windowHeight,
                playlistVisible);
    }
}
