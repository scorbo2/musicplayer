package ca.corbett.musicplayer;

import ca.corbett.extensions.AppProperties;
import ca.corbett.extras.audio.WaveformConfig;
import ca.corbett.extras.audio.WaveformConfigField;
import ca.corbett.extras.gradient.ColorSelectionType;
import ca.corbett.extras.properties.AbstractProperty;
import ca.corbett.extras.properties.BooleanProperty;
import ca.corbett.extras.properties.ColorProperty;
import ca.corbett.extras.properties.ComboProperty;
import ca.corbett.extras.properties.DecimalProperty;
import ca.corbett.extras.properties.DirectoryProperty;
import ca.corbett.extras.properties.EnumProperty;
import ca.corbett.extras.properties.FontProperty;
import ca.corbett.extras.properties.IntegerProperty;
import ca.corbett.extras.properties.LabelProperty;
import ca.corbett.extras.properties.PropertiesManager;
import ca.corbett.extras.properties.ShortTextProperty;
import ca.corbett.extras.properties.SliderProperty;
import ca.corbett.forms.fields.CheckBoxField;
import ca.corbett.forms.fields.ComboField;
import ca.corbett.forms.fields.ShortTextField;
import ca.corbett.musicplayer.actions.ReloadUIAction;
import ca.corbett.musicplayer.extensions.MusicPlayerExtension;
import ca.corbett.musicplayer.extensions.MusicPlayerExtensionManager;
import ca.corbett.musicplayer.ui.AppTheme;
import ca.corbett.musicplayer.ui.AudioPanelIdleAnimation;
import ca.corbett.musicplayer.ui.MainWindow;
import ca.corbett.musicplayer.ui.VisualizationManager;
import ca.corbett.musicplayer.ui.VisualizationThread;
import ca.corbett.musicplayer.ui.VisualizationWindow;

import java.awt.Color;
import java.awt.Font;
import java.awt.Frame;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static ca.corbett.musicplayer.ui.ControlPanel.ButtonSize;
import static ca.corbett.musicplayer.ui.ControlPanel.ControlAlignment;
import static ca.corbett.musicplayer.ui.MainWindow.DEFAULT_WIDTH;
import static ca.corbett.musicplayer.ui.VisualizationOverlay.OverlaySize;
import static ca.corbett.musicplayer.ui.VisualizationThread.AnimationSpeed;
import static ca.corbett.musicplayer.ui.VisualizationWindow.DISPLAY;

/**
 * This class extends the AppProperties class to bring together our custom
 * ExtensionManager together with a PropertiesManager so that we can manage
 * all extensions and all configuration properties in one easy location.
 * <p>
 * This class provides access to all configuration properties so
 * that code throughout the application can always retrieve the
 * latest settings. We also expose the PropertiesDialog and the
 * ExtensionManagerDialog through this class.
 * </p>
 * <p>
 * All configuration is stored in a file named "MusicPlayer.props"
 * which lives in ${SETTINGS_DIR}. The settings directory lives
 * in the user's home directory by default, but can be overridden
 * by specifying the SETTINGS_DIR system property when launching
 * the application, as shown in this example:
 * </p>
 * <pre>java -DSETTINGS_DIR=/path/to/settings/dir -jar musicplayer.jar</pre>
 *
 * @author scorbo2
 * @since 2025-03-23
 */
public class AppConfig extends AppProperties<MusicPlayerExtension> {

    private static final Logger logger = Logger.getLogger(AppConfig.class.getName());

    public static final String DEFAULT_FORMAT_STRING = "[%a] - %t (%D)"; // [artist] - title (duration)

    private static AppConfig instance;
    public static final File PROPS_FILE;

    private ComboProperty<String> idleAnimation;
    private EnumProperty<ButtonSize> buttonSize;
    private EnumProperty<ControlAlignment> controlAlignment;
    private ComboProperty<String> overrideAppThemeWaveform;
    private ColorProperty waveformBgColor;
    private ColorProperty waveformFillColor;
    private ColorProperty waveformOutlineColor;
    private IntegerProperty waveformOutlineThickness;
    private EnumProperty<WaveformConfigField.Compression> waveformResolution;
    private EnumProperty<WaveformConfigField.WidthLimit> waveformWidthLimit;
    private BooleanProperty enableSingleInstance;
    private ComboProperty<String> applicationTheme;
    private ShortTextProperty playlistFormatString;
    private ShortTextProperty playlistCustomSortString;
    private BooleanProperty shuffleEnabled;
    private BooleanProperty repeatEnabled;
    private IntegerProperty windowWidth;
    private IntegerProperty windowHeight;
    private DirectoryProperty lastBrowseDir;
    private ComboProperty<String> visualizerType;
    private EnumProperty<VisualizationThread.VisualizerRotation> visualizerRotation;
    private BooleanProperty excludeBlankVisualizerFromRotation;
    private BooleanProperty visualizerScreensaverPrevention;
    private BooleanProperty stopVisualizerOnFocusLost;
    private BooleanProperty allowVisualizerOverride;
    private EnumProperty<DISPLAY> visualizerDisplay;
    private EnumProperty<AnimationSpeed> visualizerSpeed;
    private BooleanProperty visualizerOverlayEnabled;
    private FontProperty visualizerOverlayTrackFont;
    private FontProperty visualizerOverlayHeaderFont;
    private EnumProperty<OverlaySize> visualizerOverlaySize;
    private DecimalProperty visualizerOverlayOpacity;
    private IntegerProperty visualizerOverlayBorderWidth;
    private BooleanProperty visualizerOverlayOverrideTheme;
    private ColorProperty visualizerOverlayBackground;
    private ColorProperty visualizerOverlayBorderColor;
    private ColorProperty visualizerOverlayTrackColor;
    private ColorProperty visualizerOverlayHeaderColor;
    private ColorProperty visualizerOverlayProgressBackground;
    private ColorProperty visualizerOverlayProgressForeground;
    private ComboProperty<String> visualizerOldHardwareDelay;
    private SliderProperty loadProgressBarShowDelayMS;

    /**
     * This is only used for setting default waveform prefs.
     */
    private static final WaveformConfig defaultWaveform = new WaveformConfig();

    static {
        PROPS_FILE = new File(Version.SETTINGS_DIR, "MusicPlayer.props");
    }

    protected AppConfig() {
        super(Version.FULL_NAME, PROPS_FILE, MusicPlayerExtensionManager.getInstance());
    }

    /**
     * Saves current settings and triggers a UI reload in case something
     * important has changed.
     */
    public void saveAndReloadUI() {
        save();
        ReloadUIAction.getInstance().actionPerformed(null);
    }

    /**
     * We need to expose the properties manager so that extensions can get access
     * to the underlying properties instance for manual property lookups.
     * This is to deal with the bug described <a href="https://github.com/scorbo2/app-extensions/issues/5">here</a>.
     *
     * @return Our PropertiesManager instance
     */
    public PropertiesManager getPropertiesManager() {
        return propsManager;
    }

    /**
     * Overridden here so that we can force a UI reload after every load().
     * The rationale is that the properties file may have been hand-edited
     * on disk, and we need to update to reflect any changes that were made.
     */
    @Override
    public void load() {
        super.load();
        ReloadUIAction.getInstance().actionPerformed(null);
    }

    /**
     * Basically only invoked once on app startup to kick things off.
     */
    public void loadWithoutUIReload() {
        super.load();
    }

    public static AppConfig getInstance() {
        if (instance == null) {
            instance = new AppConfig();
        }

        return instance;
    }

    /**
     * Offers a peek directly into the props file without going through the usual loading mechanism.
     * This allows direct access to properties (in String form only) exactly as they currently
     * exist in the props file. This can be useful in rare cases where an extension needs to know
     * a property value in order to initialize some other property value.
     * If the value does not exist or an error occurs while reading the props file, empty string is returned.
     *
     * @param propName The fully qualified name of the property in question.
     * @return The raw value in String form as it exists in the props file at the time of this call. May be empty.
     */
    public static String peek(String propName) {
        return AppProperties.peek(PROPS_FILE, propName);
    }

    public ButtonSize getButtonSize() {
        return buttonSize.getSelectedItem();
    }

    public ControlAlignment getControlAlignment() {
        return controlAlignment.getSelectedItem();
    }

    public Color getWaveformBgColor() {
        return waveformBgColor.getSolidColor();
    }

    public Color getWaveformFillColor() {
        return waveformFillColor.getSolidColor();
    }

    public Color getWaveformOutlineColor() {
        return waveformOutlineColor.getSolidColor();
    }

    public int getWaveformOutlineWidth() {
        return waveformOutlineThickness.getValue();
    }

    public WaveformConfigField.Compression getWaveformResolution() {
        return waveformResolution.getSelectedItem();
    }

    public WaveformConfigField.WidthLimit getWaveformWidthLimit() {
        return waveformWidthLimit.getSelectedItem();
    }

    public boolean isSingleInstanceEnabled() {
        return enableSingleInstance.getValue();
    }

    public void setSingleInstanceEnabled(boolean enabled) {
        enableSingleInstance.setValue(enabled);
    }

    public AppTheme.Theme getAppTheme() {
        return AppTheme.getTheme(applicationTheme.getSelectedItem());
    }

    /**
     * Sets the playlist format string. See getPlaylistFormatCheatsheet for details.
     */
    public void setPlaylistFormatString(String formatStr) {
        playlistFormatString.setValue(formatStr);
    }

    public String getPlaylistFormatString() {
        return playlistFormatString.getValue();
    }

    /**
     * Invoked internally to persist custom sort orders created via the Custom Sort dialog.
     * This setting is not shown to the user.
     */
    public void setPlaylistCustomSortString(String sortStr) {
        playlistCustomSortString.setValue(sortStr);
    }

    public String getPlaylistCustomSortString() {
        return playlistCustomSortString.getValue();
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

    public boolean useWaveformFromAppTheme() {
        return overrideAppThemeWaveform.getSelectedIndex() == 0;
    }

    public AudioPanelIdleAnimation.Animation getIdleAnimation() {
        return AudioPanelIdleAnimation.getInstance().get(idleAnimation.getSelectedItem());
    }

    /**
     * Note: null is allowed here.
     *
     * @param dir A directory, or null.
     */
    public void setLastBrowseDir(File dir) {
        lastBrowseDir.setDirectory(dir);
    }

    /**
     * Note: could be null, meaning no saved dir.
     * We also check that the given directory still exists, and if not, you get null.
     * Last browse dir has not yet been set? Believe it or not, null. Straight away.
     *
     * @return A directory, or null.
     */
    public File getLastBrowseDir() {
        return (lastBrowseDir.getDirectory() != null && lastBrowseDir.getDirectory()
                                                                     .exists()) ? lastBrowseDir.getDirectory() : null;
    }

    public VisualizationManager.Visualizer getVisualizer() {
        return VisualizationManager.getVisualizer(visualizerType.getSelectedItem());
    }

    public VisualizationThread.VisualizerRotation getVisualizerRotation() {
        return visualizerRotation.getSelectedItem();
    }

    public boolean isExcludeBlankVisualizerFromRotation() {
        return excludeBlankVisualizerFromRotation.getValue();
    }

    public VisualizationWindow.DISPLAY getPreferredVisualizationDisplay() {
        return visualizerDisplay.getSelectedItem();
    }

    public VisualizationThread.AnimationSpeed getVisualizationAnimationSpeed() {
        return visualizerSpeed.getSelectedItem();
    }

    public Font getVisualizerOverlayTrackFont() {
        return visualizerOverlayTrackFont.getFont();
    }

    public Font getVisualizerOverlayHeaderFont() {
        return visualizerOverlayHeaderFont.getFont();
    }

    public OverlaySize getVisualizationOverlaySize() {
        return visualizerOverlaySize.getSelectedItem();
    }

    public float getVisualizationOverlayOpacity() {
        return (float)visualizerOverlayOpacity.getValue();
    }

    public boolean isVisualizerOverlayEnabled() {
        return visualizerOverlayEnabled.getValue();
    }

    public boolean isAllowVisualizerOverride() {
        return allowVisualizerOverride.getValue();
    }

    public boolean isVisualizerOverlayOverrideTheme() {
        return visualizerOverlayOverrideTheme.getValue();
    }

    public int getVisualizerOverlayBorderWidth() {
        return visualizerOverlayBorderWidth.getValue();
    }

    public Color getVisualizerOverlayBackground() {
        return visualizerOverlayBackground.getSolidColor();
    }

    public Color getVisualizerOverlayBorderColor() {
        return visualizerOverlayBorderColor.getSolidColor();
    }

    public Color getVisualizerOverlayTrackColor() {
        return visualizerOverlayTrackColor.getSolidColor();
    }

    public Color getVisualizerOverlayHeaderColor() {
        return visualizerOverlayHeaderColor.getSolidColor();
    }

    public Color getVisualizerOverlayProgressBackground() {
        return visualizerOverlayProgressBackground.getSolidColor();
    }

    public Color getVisualizerOverlayProgressForeground() {
        return visualizerOverlayProgressForeground.getSolidColor();
    }

    public boolean isStopVisualizerOnFocusLost() {
        return stopVisualizerOnFocusLost.getValue();
    }

    public boolean isVisualizerScreensaverPreventionEnabled() {
        return visualizerScreensaverPrevention.getValue();
    }

    public int getLoadProgressBarShowDelayMS() {
        return loadProgressBarShowDelayMS.getValue();
    }

    /**
     * Older or less capable graphics hardware needs 100-200 milliseconds to effect a
     * display mode switch (switching to fullscreen mode) - that means we sometimes
     * have to wait a little bit before attempting to create our BufferStrategy, otherwise
     * the switch to fullscreen mode will simply fail.
     * See <a href="https://github.com/scorbo2/musicplayer/issues/23">issue 23</a>
     * for more details.
     * <p>Newer or more capable hardware can set this number down low as the mode
     * switch is much faster.</p>
     *
     * @return A count of milliseconds to wait after requesting a display mode switch.
     */
    public int getVisualizerOldHardwareDelay() {
        return getOldHardwareDelayFromStringOption(visualizerOldHardwareDelay.getSelectedItem());
    }

    @Override
    protected List<AbstractProperty> createInternalProperties() {
        buttonSize = new EnumProperty<>("UI.General.buttonSize", "Control size:", ButtonSize.LARGE);
        controlAlignment = new EnumProperty<>("UI.General.controlAlignment", "Control alignment:",
                                              ControlAlignment.CENTER);

        idleAnimation = buildCombo("UI.General.idleAnimation", "Idle animation:", getIdleAnimationChoices(), true);
        overrideAppThemeWaveform = buildCombo("Waveform.Waveform graphics.override", "Waveform:",
                                              getOverrideThemeWaveformChoices(), false);

        enableSingleInstance = new BooleanProperty("UI.General.singleInstance",
                                                   "Only allow a single instance of MusicPlayer",
                                                   true);

        LabelProperty label = new LabelProperty("UI.General.progressBarDelayMSLabel",
                                                "Optional delay before showing the audio load progress bar:");
        loadProgressBarShowDelayMS = new SliderProperty("UI.General.progressBarDelay", "", 0, 5000, 1000);
        loadProgressBarShowDelayMS.setShouldExpand(false);
        loadProgressBarShowDelayMS.setLabels(List.of("no delay", "1s", "2s", "3s", "4s", "5s"), false);

        // Make sure we respond to change events properly, to enable or disable the override fields:
        overrideAppThemeWaveform.addFormFieldChangeListener(event -> {
            //noinspection unchecked
            boolean shouldEnable = ((ComboField<String>)event.formField()).getSelectedIndex() == 1;
            event.formPanel().getFormField(waveformBgColor.getFullyQualifiedName()).setEnabled(shouldEnable);
            event.formPanel().getFormField(waveformFillColor.getFullyQualifiedName()).setEnabled(shouldEnable);
            event.formPanel().getFormField(waveformOutlineColor.getFullyQualifiedName()).setEnabled(shouldEnable);
            event.formPanel().getFormField(waveformOutlineThickness.getFullyQualifiedName())
                 .setEnabled(shouldEnable);
        });

        waveformBgColor = new ColorProperty("Waveform.Waveform graphics.bgColor",
                                            "Background:",
                                            ColorSelectionType.SOLID);
        waveformBgColor.setSolidColor(defaultWaveform.getBgColor());
        waveformFillColor = new ColorProperty("Waveform.Waveform graphics.fillColor",
                                              "Fill:",
                                              ColorSelectionType.SOLID);
        waveformFillColor.setSolidColor(defaultWaveform.getFillColor());
        waveformOutlineColor = new ColorProperty("Waveform.Waveform graphics.outlineColor", "Outline:",
                                                 ColorSelectionType.SOLID);
        waveformOutlineColor.setSolidColor(defaultWaveform.getOutlineColor());
        waveformOutlineThickness = new IntegerProperty("Waveform.Waveform graphics.outlineWidth", "Outline width:",
                                                       defaultWaveform.getOutlineThickness(), 0, 24, 1);

        waveformResolution = new EnumProperty<>("Waveform.Resolution.resolution", "Resolution:",
                                                WaveformConfigField.Compression.HIGH);
        waveformWidthLimit = new EnumProperty<>("Waveform.Resolution.widthLimit", "Width limit:",
                                                WaveformConfigField.WidthLimit.LARGE);

        applicationTheme = buildCombo("UI.Theme.theme", "Theme:", getAppThemeChoices(), true);

        playlistFormatString = new ShortTextProperty("UI.Playlist.formatString",
                                                     "Playlist format:",
                                                     DEFAULT_FORMAT_STRING,
                                                     20);
        playlistFormatString.addFormFieldGenerationListener(
            (property, formField)
                -> ((ShortTextField)formField).getTextField().setColumns(20)); // THIS SHOULD NOT BE NECESSARY!
        playlistFormatString.setHelpText(getPlaylistFormatCheatsheet());

        playlistCustomSortString = new ShortTextProperty("UI.Playlist.customFormatString", "customFormat", "");
        playlistCustomSortString.setExposed(false);

        visualizerType = buildCombo("Visualization.General.visualizer", "Visualizer:", getVisualizerChoices(), true);
        visualizerRotation = new EnumProperty<>("Visualization.General.visualizerRotation", "Rotate visualizers:",
                                                VisualizationThread.VisualizerRotation.NEVER);
        excludeBlankVisualizerFromRotation = new BooleanProperty(
            "Visualization.General.excludeBlankVisualizerFromRotation",
            "Exclude blank screen visualizer from rotation",
            true);
        visualizerScreensaverPrevention = new BooleanProperty("Visualization.General.screensaverPrevention",
                                                              "Prevent screensaver during visualization (requires Robot)",
                                                              true);
        stopVisualizerOnFocusLost = new BooleanProperty("Visualization.General.stopOnFocusLost",
                                                        "In single-monitor mode, stop visualizer on window focus lost",
                                                        true);
        allowVisualizerOverride = new BooleanProperty("Visualization.General.allowOverride",
                                                      "Allow override of selected visualizer based on file triggers",
                                                      true);
        visualizerDisplay = new EnumProperty<>("Visualization.General.preferredDisplay", "Preferred display:",
                                               VisualizationWindow.DISPLAY.PRIMARY);
        visualizerSpeed = new EnumProperty<>("Visualization.General.animationSpeed", "Animation speed:",
                                             VisualizationThread.AnimationSpeed.HIGH);
        visualizerOldHardwareDelay = buildCombo("Visualization.General.oldHardwareDelay", "Fullscreen delay:",
                                                getOldHardwareDelayChoices(), false);
        visualizerOldHardwareDelay.setHelpText("Increase this delay if fullscreen mode loads to a blank screen");

        visualizerOverlayEnabled = new BooleanProperty("Visualization.Overlay.enabled",
                                                       "Enable visualizer overlay for current track info",
                                                       true);
        visualizerOverlayHeaderFont = new FontProperty("Visualization.Overlay.headerFont", "Header font:");
        visualizerOverlayTrackFont = new FontProperty("Visualization.Overlay.trackFont", "Info font:");
        visualizerOverlaySize = new EnumProperty<>("Visualization.Overlay.size", "Overlay size:", OverlaySize.SMALL);
        visualizerOverlayOpacity = new DecimalProperty("Visualization.Overlay.opacity", "Overlay opacity:", 1.0, 0.0,
                                                       1.0, 0.1);
        visualizerOverlayBorderWidth = new IntegerProperty("Visualization.Overlay.borderWidth", "Border width:", 2, 0,
                                                           10, 1);
        visualizerOverlayOverrideTheme = new BooleanProperty("Visualization.Overlay.overrideTheme",
                                                             "Override app theme and use the following colors:",
                                                             false);

        // Make sure we enable or disable the overlay fields based on the override checkbox:
        visualizerOverlayOverrideTheme.addFormFieldChangeListener(event -> {
            boolean isOverride = ((CheckBoxField)event.formField()).isChecked();
            event.formPanel().getFormField(visualizerOverlayBackground.getFullyQualifiedName())
                 .setEnabled(isOverride);
            event.formPanel().getFormField(visualizerOverlayTrackColor.getFullyQualifiedName())
                 .setEnabled(isOverride);
            event.formPanel().getFormField(visualizerOverlayHeaderColor.getFullyQualifiedName())
                 .setEnabled(isOverride);
            event.formPanel().getFormField(visualizerOverlayBorderColor.getFullyQualifiedName())
                 .setEnabled(isOverride);
            event.formPanel().getFormField(visualizerOverlayProgressBackground.getFullyQualifiedName())
                 .setEnabled(isOverride);
            event.formPanel().getFormField(visualizerOverlayProgressForeground.getFullyQualifiedName())
                 .setEnabled(isOverride);
        });

        visualizerOverlayBackground = new ColorProperty("Visualization.Overlay.bgColor",
                                                        "Overlay background:",
                                                        ColorSelectionType.SOLID);
        visualizerOverlayBackground.setSolidColor(Color.BLACK);
        visualizerOverlayBorderColor = new ColorProperty("Visualization.Overlay.borderColor",
                                                         "Overlay border:",
                                                         ColorSelectionType.SOLID);
        visualizerOverlayBorderColor.setSolidColor(Color.WHITE);
        visualizerOverlayHeaderColor = new ColorProperty("Visualization.Overlay.headerColor",
                                                         "Header text:",
                                                         ColorSelectionType.SOLID);
        visualizerOverlayHeaderColor.setSolidColor(Color.WHITE);
        visualizerOverlayTrackColor = new ColorProperty("Visualization.Overlay.trackColor",
                                                        "Info text:",
                                                        ColorSelectionType.SOLID);
        visualizerOverlayTrackColor.setSolidColor(Color.LIGHT_GRAY);

        visualizerOverlayProgressBackground = new ColorProperty("Visualization.Overlay.progressBg",
                                                                "Progress background:", ColorSelectionType.SOLID);
        visualizerOverlayProgressBackground.setSolidColor(Color.DARK_GRAY);
        visualizerOverlayProgressForeground = new ColorProperty("Visualization.Overlay.progressFg",
                                                                "Progress foreground:", ColorSelectionType.SOLID);
        visualizerOverlayProgressForeground.setSolidColor(Color.BLUE);

        // Add our internal hidden properties (not exposed to the user but available to the code):
        shuffleEnabled = new BooleanProperty("hidden.props.shuffleEnabled", "shuffleEnabled");
        repeatEnabled = new BooleanProperty("hidden.props.repeatEnabled", "repeatEnabled");
        windowWidth = new IntegerProperty("hidden.props.windowWidth", "windowWidth", DEFAULT_WIDTH);
        windowHeight = new IntegerProperty("hidden.props.windowHeight", "windowHeight", MainWindow.DEFAULT_HEIGHT);
        lastBrowseDir = new DirectoryProperty("hidden.props.browseDir", "browseDir", true);
        shuffleEnabled.setExposed(false);
        repeatEnabled.setExposed(false);
        windowWidth.setExposed(false);
        windowHeight.setExposed(false);
        lastBrowseDir.setExposed(false);

        return List.of(buttonSize,
                       controlAlignment,
                       idleAnimation,
                       enableSingleInstance,
                       label,
                       loadProgressBarShowDelayMS,
                       overrideAppThemeWaveform,
                       waveformBgColor,
                       waveformFillColor,
                       waveformOutlineColor,
                       waveformOutlineThickness,
                       waveformResolution,
                       waveformWidthLimit,
                       applicationTheme,
                       playlistFormatString,
                       playlistCustomSortString,
                       shuffleEnabled,
                       repeatEnabled,
                       windowWidth,
                       windowHeight,
                       lastBrowseDir,
                       visualizerType,
                       visualizerRotation,
                       excludeBlankVisualizerFromRotation,
                       visualizerScreensaverPrevention,
                       stopVisualizerOnFocusLost,
                       allowVisualizerOverride,
                       visualizerDisplay,
                       visualizerSpeed,
                       visualizerOldHardwareDelay,
                       visualizerOverlayEnabled,
                       visualizerOverlayHeaderFont,
                       visualizerOverlayTrackFont,
                       visualizerOverlaySize,
                       visualizerOverlayOpacity,
                       visualizerOverlayBorderWidth,
                       visualizerOverlayOverrideTheme,
                       visualizerOverlayBackground,
                       visualizerOverlayBorderColor,
                       visualizerOverlayHeaderColor,
                       visualizerOverlayTrackColor,
                       visualizerOverlayProgressBackground,
                       visualizerOverlayProgressForeground);
    }

    /**
     * Overridden here so we can add override the default size of the properties dialog,
     * and also so that we can set the initial state of certain fields based upon
     * the values of our properties.
     *
     * @param owner The owning Frame (so we can make the dialog modal to that Frame).
     * @return true if the user OK'd the dialog with changes.
     */
    @Override
    public boolean showPropertiesDialog(Frame owner) {
        // We need a slightly larger dialog than the default value:
        propertiesDialogMinimumWidth = propertiesDialogInitialWidth = 660;
        propertiesDialogMinimumHeight = propertiesDialogInitialHeight = 480;

        // Set initial state of waveform fields based on the value of overrideAppThemeWaveform:
        boolean isWaveformOverride = overrideAppThemeWaveform.getSelectedIndex() == 1;
        waveformBgColor.setInitiallyEditable(isWaveformOverride);
        waveformFillColor.setInitiallyEditable(isWaveformOverride);
        waveformOutlineColor.setInitiallyEditable(isWaveformOverride);
        waveformOutlineThickness.setInitiallyEditable(isWaveformOverride);

        // Set initial state of overlay fields based on visualizerOverlayOverrideTheme:
        boolean isOverlayOverride = visualizerOverlayOverrideTheme.getValue();
        visualizerOverlayBackground.setInitiallyEditable(isOverlayOverride);
        visualizerOverlayTrackColor.setInitiallyEditable(isOverlayOverride);
        visualizerOverlayHeaderColor.setInitiallyEditable(isOverlayOverride);
        visualizerOverlayBorderColor.setInitiallyEditable(isOverlayOverride);
        visualizerOverlayProgressBackground.setInitiallyEditable(isOverlayOverride);
        visualizerOverlayProgressForeground.setInitiallyEditable(isOverlayOverride);

        return super.showPropertiesDialog(owner);
    }

    /**
     * Invoked internally to generate and return a ComboProperty with the given options.
     * By default, the first option in the given list will be the starting selection in the combo.
     * But, you can set "prefer2nd" to true to select the second option by default.
     *
     * @param fieldName The fully qualified field name for the combo.
     * @param label     The label for the combo box.
     * @param options   The list of options to set.
     * @param prefer2nd If true, index 1 will be preselected instead of 0 (if more than one option is present)
     * @return A ComboProperty.
     */
    private ComboProperty<String> buildCombo(String fieldName, String label, List<String> options, boolean prefer2nd) {
        int selectedIndex = 0;
        if (prefer2nd && options.size() > 1) {
            selectedIndex = 1;
        }
        return new ComboProperty<>(fieldName, label, options, selectedIndex, false);
    }

    /**
     * Invoked internally to get a list of all the options for Idle Animation.
     * Behind the scenes, this will query our ExtensionManager, as extensions
     * can optionally provide additional idle animations.
     *
     * @return A list of possible idle animation choices by their String name.
     */
    private List<String> getIdleAnimationChoices() {
        List<String> options = new ArrayList<>();
        for (AudioPanelIdleAnimation.Animation animation : AudioPanelIdleAnimation.getInstance().getAll()) {
            options.add(animation.getName());
        }
        return options;
    }

    /**
     * Invoked internally to return a list of all the options for the "override
     * theme settings for waveform" combo box.
     *
     * @return A list of options in string format.
     */
    private List<String> getOverrideThemeWaveformChoices() {
        List<String> options = new ArrayList<>();
        options.add("Use waveform settings from application theme");
        options.add("Override application theme with custom settings");
        return options;
    }

    /**
     * Invoked internally to get a list of all the options for application theme.
     * Behind the scenes, this will query our ExtensionManager, as extensions
     * can optionally provide additional app themes.
     *
     * @return A list of possible app theme choices by their String name.
     */
    private List<String> getAppThemeChoices() {
        List<String> options = new ArrayList<>();
        for (AppTheme.Theme theme : AppTheme.getAll()) {
            options.add(theme.getName());
        }
        return options;
    }

    /**
     * Invoked internally to get a list of all the options for visualization.
     * Behind the scenes, this will query our ExtensionManager, as extensions
     * can optionally provide additional visualizers.
     *
     * @return A list of possible visualizer choices by their String name.
     */
    private List<String> getVisualizerChoices() {
        List<String> options = new ArrayList<>();
        for (VisualizationManager.Visualizer visualizer : VisualizationManager.getAll()) {
            if (!visualizer.isSupportsFileTriggers()) {
                options.add(visualizer.getName());
            }
        }
        return options;
    }

    private List<String> getOldHardwareDelayChoices() {
        List<String> options = new ArrayList<>();
        options.add("None - my hardware is awesome");
        options.add("Small - my hardware is a bit old");
        options.add("Medium - my hardware is not so good");
        options.add("Large - my hardware is pretty bad");
        options.add("XLarge - my hardware is ancient");
        return options;
    }

    private int getOldHardwareDelayFromStringOption(String option) {
        return switch (option) {
            case "None - my hardware is awesome" -> 25;
            case "Small - my hardware is a bit old" -> 50;
            case "Medium - my hardware is not so good" -> 75;
            case "Large - my hardware is pretty bad" -> 150;
            case "XLarge - my hardware is ancient" -> 250;
            default -> 25;
        };
    }

    private String getPlaylistFormatCheatsheet() {
        return "<html>Use the following tokens to customize the playlist format string:<br>" +
            "<ul>" +
            "<li><b>%a</b> - Artist</li>" +
            "<li><b>%t</b> - Title</li>" +
            "<li><b>%b</b> - Album</li>" +
            "<li><b>%n</b> - Track number</li>" +
            "<li><b>%D</b> - Duration (mm:ss)</li>" +
            "<li><b>%g</b> - Genre</li>" +
            "<li><b>%f</b> - Filename</li>" +
            "<li><b>%F</b> - Full file path</li>" +
            "</ul>" +
            "Combine these tokens with any desired text or punctuation to create your custom format." +
            "</html>";
    }
}
