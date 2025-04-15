package ca.corbett.musicplayer;

import ca.corbett.extensions.AppProperties;
import ca.corbett.extras.audio.WaveformConfig;
import ca.corbett.extras.properties.AbstractProperty;
import ca.corbett.extras.properties.BooleanProperty;
import ca.corbett.extras.properties.ColorProperty;
import ca.corbett.extras.properties.ComboProperty;
import ca.corbett.extras.properties.DecimalProperty;
import ca.corbett.extras.properties.DirectoryProperty;
import ca.corbett.extras.properties.EnumProperty;
import ca.corbett.extras.properties.FontProperty;
import ca.corbett.extras.properties.IntegerProperty;
import ca.corbett.extras.properties.PropertiesDialog;
import ca.corbett.extras.properties.PropertiesManager;
import ca.corbett.forms.FormPanel;
import ca.corbett.forms.fields.CheckBoxField;
import ca.corbett.forms.fields.ComboField;
import ca.corbett.forms.fields.FormField;
import ca.corbett.musicplayer.actions.ReloadUIAction;
import ca.corbett.musicplayer.extensions.MusicPlayerExtension;
import ca.corbett.musicplayer.extensions.MusicPlayerExtensionManager;
import ca.corbett.musicplayer.ui.AppTheme;
import ca.corbett.musicplayer.ui.AudioPanel;
import ca.corbett.musicplayer.ui.AudioPanelIdleAnimation;
import ca.corbett.musicplayer.ui.MainWindow;
import ca.corbett.musicplayer.ui.VisualizationManager;
import ca.corbett.musicplayer.ui.VisualizationThread;
import ca.corbett.musicplayer.ui.VisualizationWindow;

import javax.swing.AbstractAction;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static ca.corbett.extras.properties.ColorProperty.ColorType;
import static ca.corbett.extras.properties.PropertiesManager.findFormField;
import static ca.corbett.musicplayer.ui.AudioPanel.WaveformResolution;
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
 * All configuration is persisted to the properties file which
 * is defined by the system property "ca.corbett.musicplayer.props.file".
 * If this system property is not defined, it defaults to a file
 * called MusicPlayer.props which will be created in the user's
 * home directory.
 * </p>
 * <p>
 * To override the default properties file location:
 * </p>
 * <pre>java -Dca.corbett.musicplayer.props.file=/tmp/blah.props -jar musicplayer.jar</pre>
 *
 * @author scorbo2
 * @since 2025-03-23
 */
public class AppConfig extends AppProperties<MusicPlayerExtension> {

    private static final Logger logger = Logger.getLogger(AppConfig.class.getName());

    private static AppConfig instance;
    public static final File PROPS_FILE;

    private ComboProperty idleAnimation;
    private EnumProperty<ButtonSize> buttonSize;
    private EnumProperty<ControlAlignment> controlAlignment;
    private ComboProperty overrideAppThemeWaveform;
    private ColorProperty waveformBgColor;
    private ColorProperty waveformFillColor;
    private ColorProperty waveformOutlineColor;
    private IntegerProperty waveformOutlineThickness;
    private EnumProperty<WaveformResolution> waveformResolution;
    private ComboProperty applicationTheme;
    private BooleanProperty shuffleEnabled;
    private BooleanProperty repeatEnabled;
    private IntegerProperty windowWidth;
    private IntegerProperty windowHeight;
    private DirectoryProperty lastBrowseDir;
    private ComboProperty visualizerType;
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

    /**
     * This is only used for setting default waveform prefs.
     */
    private static final WaveformConfig defaultWaveform = new WaveformConfig();

    // The location of the properties file can be set with a system property
    // on startup (refer to the user's guide in the README). If so set, we
    // need to read it in this static initializer block on startup so we're
    // ready to go if it's somewhere other than the default location.
    static {
        File f;
        if (System.getProperty("ca.corbett.musicplayer.props.file") != null) {
            f = new File(System.getProperty("ca.corbett.musicplayer.props.file"));
        }
        else {
            f = new File(System.getProperty("user.home"), "MusicPlayer.props");
        }
        PROPS_FILE = f;
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

    public ButtonSize getButtonSize() {
        return buttonSize.getSelectedItem();
    }

    public ControlAlignment getControlAlignment() {
        return controlAlignment.getSelectedItem();
    }

    public Color getWaveformBgColor() {
        return waveformBgColor.getColor();
    }

    public Color getWaveformFillColor() {
        return waveformFillColor.getColor();
    }

    public Color getWaveformOutlineColor() {
        return waveformOutlineColor.getColor();
    }

    public int getWaveformOutlineWidth() {
        return waveformOutlineThickness.getValue();
    }

    public WaveformResolution getWaveformResolution() {
        return waveformResolution.getSelectedItem();
    }

    public AppTheme.Theme getAppTheme() {
        return AppTheme.getTheme(applicationTheme.getSelectedItem());
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
        return visualizerOverlayBackground.getColor();
    }

    public Color getVisualizerOverlayBorderColor() {
        return visualizerOverlayBorderColor.getColor();
    }

    public Color getVisualizerOverlayTrackColor() {
        return visualizerOverlayTrackColor.getColor();
    }

    public Color getVisualizerOverlayHeaderColor() {
        return visualizerOverlayHeaderColor.getColor();
    }

    public Color getVisualizerOverlayProgressBackground() {
        return visualizerOverlayProgressBackground.getColor();
    }

    public Color getVisualizerOverlayProgressForeground() {
        return visualizerOverlayProgressForeground.getColor();
    }

    public boolean isStopVisualizerOnFocusLost() {
        return stopVisualizerOnFocusLost.getValue();
    }

    public boolean isVisualizerScreensaverPreventionEnabled() {
        return visualizerScreensaverPrevention.getValue();
    }

    @Override
    protected List<AbstractProperty> createInternalProperties() {
        buttonSize = new EnumProperty<>("UI.General.buttonSize", "Control size:", ButtonSize.LARGE);
        controlAlignment = new EnumProperty<>("UI.General.controlAlignment", "Control alignment:",
                                              ControlAlignment.CENTER);

        idleAnimation = buildCombo("UI.General.idleAnimation", "Idle animation:", getIdleAnimationChoices(), true);
        overrideAppThemeWaveform = buildCombo("Waveform.Waveform graphics.override", "Waveform:",
                                              getOverrideThemeWaveformChoices(), false);

        waveformBgColor = new ColorProperty("Waveform.Waveform graphics.bgColor", "Background:", ColorType.SOLID,
                                            defaultWaveform.getBgColor());
        waveformFillColor = new ColorProperty("Waveform.Waveform graphics.fillColor", "Fill:", ColorType.SOLID,
                                              defaultWaveform.getFillColor());
        waveformOutlineColor = new ColorProperty("Waveform.Waveform graphics.outlineColor", "Outline:", ColorType.SOLID,
                                                 defaultWaveform.getOutlineColor());
        waveformOutlineThickness = new IntegerProperty("Waveform.Waveform graphics.outlineWidth", "Outline width:",
                                                       defaultWaveform.getOutlineThickness(), 0, 24, 1);

        waveformResolution = new EnumProperty<>("Waveform.Resolution.resolution", "Resolution:",
                                                AudioPanel.WaveformResolution.HIGH);

        applicationTheme = buildCombo("UI.Theme.theme", "Theme:", getAppThemeChoices(), true);

        visualizerType = buildCombo("Visualization.General.visualizer", "Visualizer:", getVisualizerChoices(), true);
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
        visualizerOverlayBackground = new ColorProperty("Visualization.Overlay.bgColor", "Overlay background:",
                                                        ColorType.SOLID, Color.BLACK);
        visualizerOverlayBorderColor = new ColorProperty("Visualization.Overlay.borderColor", "Overlay border:",
                                                         ColorType.SOLID, Color.WHITE);
        visualizerOverlayHeaderColor = new ColorProperty("Visualization.Overlay.headerColor", "Header text:",
                                                         ColorType.SOLID, Color.WHITE);
        visualizerOverlayTrackColor = new ColorProperty("Visualization.Overlay.trackColor", "Info text:",
                                                        ColorType.SOLID, Color.LIGHT_GRAY);

        visualizerOverlayProgressBackground = new ColorProperty("Visualization.Overlay.progressBg",
                                                                "Progress background:", ColorType.SOLID,
                                                                Color.DARK_GRAY);
        visualizerOverlayProgressForeground = new ColorProperty("Visualization.Overlay.progressFg",
                                                                "Progress foreground:", ColorType.SOLID,
                                                                Color.BLUE);

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
                       overrideAppThemeWaveform,
                       waveformBgColor,
                       waveformFillColor,
                       waveformOutlineColor,
                       waveformOutlineThickness,
                       waveformResolution,
                       applicationTheme,
                       shuffleEnabled,
                       repeatEnabled,
                       windowWidth,
                       windowHeight,
                       lastBrowseDir,
                       visualizerType,
                       visualizerScreensaverPrevention,
                       stopVisualizerOnFocusLost,
                       allowVisualizerOverride,
                       visualizerDisplay,
                       visualizerSpeed,
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
     * Overridden here so we can add custom logic to our properties form before
     * it gets rendered. Setting certain action listeners and the initial state
     * of controls needs to be done before the form is rendered. Fortunately,
     * the PropertiesManager class provides a way for us to do that somewhat easily.
     *
     * @param owner The owning Frame (so we can make the dialog modal to that Frame).
     * @return true if the user OK'd the dialog with changes.
     */
    @Override
    public boolean showPropertiesDialog(Frame owner) {
        List<FormPanel> formPanels = propsManager.generateUnrenderedFormPanels(FormPanel.Alignment.TOP_LEFT, 24);

        // Our custom form logic goes here:
        addWaveformOverrideFormBehaviour(formPanels);
        addOverlayOverrideFormBehaviour(formPanels);

        PropertiesDialog dialog = new PropertiesDialog(propsManager, owner, Version.NAME + " properties", formPanels);
        dialog.setSize(660, 480);
        dialog.setMinimumSize(new Dimension(660, 480));
        dialog.setVisible(true);
        if (dialog.wasOkayed()) {
            save();
        }

        return dialog.wasOkayed();
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
    private ComboProperty buildCombo(String fieldName, String label, List<String> options, boolean prefer2nd) {
        int selectedIndex = 0;
        if (prefer2nd && options.size() > 1) {
            selectedIndex = 1;
        }
        return new ComboProperty(fieldName, label, options, selectedIndex, false);
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
            options.add(visualizer.getName());
        }
        return options;
    }


    /**
     * Adds custom logic to enable/disable the waveform cosmetic properties depending on the value
     * of the override combo box, and sets the initial state of all relevant components.
     * This can only be done before the form panel is rendered.
     *
     * @param formPanels A list of unrendered FormPanels.
     */
    private void addWaveformOverrideFormBehaviour(List<FormPanel> formPanels) {
        final ComboField combo = (ComboField)findFormField(overrideAppThemeWaveform.getFullyQualifiedName(),
                                                           formPanels);
        final FormField bgColorField = findFormField(waveformBgColor.getFullyQualifiedName(), formPanels);
        final FormField fillColorField = findFormField(waveformFillColor.getFullyQualifiedName(), formPanels);
        final FormField outlineColorField = findFormField(waveformOutlineColor.getFullyQualifiedName(), formPanels);
        final FormField outlineWidthField = findFormField(waveformOutlineThickness.getFullyQualifiedName(), formPanels);
        if (combo == null || bgColorField == null || fillColorField == null || outlineColorField == null || outlineWidthField == null) {
            logger.severe("addWaveformOverrideFormBehaviour: unable to locate required form fields! Internal error.");
            return;
        }

        // Set initial state for these fields:
        bgColorField.setEnabled(combo.getSelectedIndex() == 1);
        fillColorField.setEnabled(combo.getSelectedIndex() == 1);
        outlineColorField.setEnabled(combo.getSelectedIndex() == 1);
        outlineWidthField.setEnabled(combo.getSelectedIndex() == 1);

        // Now allow it to change based on the override combo box value:
        combo.addValueChangedAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean shouldEnable = combo.getSelectedIndex() == 1;
                bgColorField.setEnabled(shouldEnable);
                fillColorField.setEnabled(shouldEnable);
                outlineColorField.setEnabled(shouldEnable);
                outlineWidthField.setEnabled(shouldEnable);
            }
        });
    }

    /**
     * Adds custom logic to enable/disable the waveform cosmetic properties depending on the value
     * of the override combo box, and sets the initial state of all relevant components.
     * This can only be done before the form panel is rendered.
     *
     * @param formPanels A list of unrendered FormPanels.
     */
    private void addOverlayOverrideFormBehaviour(List<FormPanel> formPanels) {
        CheckBoxField allowOverride = (CheckBoxField)findFormField(
            visualizerOverlayOverrideTheme.getFullyQualifiedName(), formPanels);
        FormField overlayBg = findFormField(visualizerOverlayBackground.getFullyQualifiedName(), formPanels);
        FormField overlayFg = findFormField(visualizerOverlayTrackColor.getFullyQualifiedName(), formPanels);
        FormField headerFg = findFormField(visualizerOverlayHeaderColor.getFullyQualifiedName(), formPanels);
        FormField borderColor = findFormField(visualizerOverlayBorderColor.getFullyQualifiedName(), formPanels);
        FormField progressBg = findFormField(visualizerOverlayProgressBackground.getFullyQualifiedName(), formPanels);
        FormField progressFg = findFormField(visualizerOverlayProgressForeground.getFullyQualifiedName(), formPanels);
        if (allowOverride == null || overlayBg == null || overlayFg == null ||
            headerFg == null || progressFg == null || progressBg == null || borderColor == null) {
            logger.severe("addOverlayOverrideFormBehaviour: unable to locate required form fields! Internal error.");
            return;
        }

        // set initial state for these fields:
        overlayBg.setEnabled(allowOverride.isChecked());
        overlayFg.setEnabled(allowOverride.isChecked());
        headerFg.setEnabled(allowOverride.isChecked());
        borderColor.setEnabled(allowOverride.isChecked());
        progressBg.setEnabled(allowOverride.isChecked());
        progressFg.setEnabled(allowOverride.isChecked());

        // now allow it to change based on the override checkbox:
        allowOverride.addValueChangedAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                overlayBg.setEnabled(allowOverride.isChecked());
                overlayFg.setEnabled(allowOverride.isChecked());
                headerFg.setEnabled(allowOverride.isChecked());
                borderColor.setEnabled(allowOverride.isChecked());
                progressBg.setEnabled(allowOverride.isChecked());
                progressFg.setEnabled(allowOverride.isChecked());
            }
        });
    }
}
