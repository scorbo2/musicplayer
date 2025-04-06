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
import ca.corbett.musicplayer.ui.AudioPanelIdleAnimation;
import ca.corbett.musicplayer.ui.MainWindow;
import ca.corbett.musicplayer.ui.VisualizationManager;
import ca.corbett.musicplayer.ui.VisualizationOverlay;
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

/**
 * Taking advantage of the AppProperties helper class in my own
 * app-extensions library, as it saves a LOT of work in
 * managing application properties and exposing them to the user.
 * <p>
 * The persistence file for application properties defaults
 * to "MusicPlayer.props" in the user's home directory.
 * You can override this by setting the
 * ca.corbett.musicplayer.props.file system property.
 * </p>
 * <p>
 * <B>Example:</B>
 * </p>
 * <pre>java -Dca.corbett.musicplayer.props.file=/tmp/blah.props -jar MusicPlayer.jar</pre>
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
    private BooleanProperty stopVisualizerOnFocusLost;
    private BooleanProperty allowVisualizerOverride;
    private EnumProperty<VisualizationWindow.DISPLAY> visualizerDisplay;
    private EnumProperty<VisualizationThread.AnimationSpeed> visualizerSpeed;
    private BooleanProperty visualizerOverlayEnabled;
    private EnumProperty<FontFamily> visualizerOverlayFont;
    private IntegerProperty visualizerOverlayFontSize;
    private EnumProperty<VisualizationOverlay.OverlaySize> visualizerOverlaySize;
    private DecimalProperty visualizerOverlayOpacity;
    private IntegerProperty visualizerOverlayBorderWidth;
    private BooleanProperty visualizerOverlayOverrideTheme;
    private ColorProperty visualizerOverlayBackground;
    private ColorProperty visualizerOverlayForeground;
    private ColorProperty visualizerOverlayHeader;
    private ColorProperty visualizerOverlayProgressBackground;
    private ColorProperty visualizerOverlayProgressForeground;

    public enum ButtonSize {
        XSMALL(16, "Extra small"),
        SMALL(20, "Small"),
        NORMAL(24, "Normal"),
        LARGE(30, "Large"),
        XLARGE(36, "Huge");

        final private int buttonSize;
        final private String label;

        ButtonSize(int btnSize, String label) {
            buttonSize = btnSize;
            this.label = label;
        }

        public int getButtonSize() {
            return buttonSize;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public enum ControlAlignment {
        LEFT("Left"),
        CENTER("Center"),
        RIGHT("Right");

        private final String label;

        ControlAlignment(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public enum WaveformResolution {
        LOW(512, "Low"),
        MEDIUM(1024, "Medium"),
        HIGH(2048, "High"),
        SUPER_HIGH(4096, "Super high");

        private final int xLimit;
        private final String label;

        WaveformResolution(int xLimit, String label) {
            this.xLimit = xLimit;
            this.label = label;
        }

        public int getXLimit() {
            return xLimit;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public enum FontFamily {
        SANS_SERIF(Font.SANS_SERIF),
        SERIF(Font.SERIF),
        MONOSPACED(Font.MONOSPACED);

        public final String familyName;

        FontFamily(String label) {
            this.familyName = label;
        }

        @Override
        public String toString() {
            return familyName;
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

    public void saveAndReloadUI() {
        save();
        ReloadUIAction.getInstance().actionPerformed(null);
    }

    /**
     * We need to expose the properties manager so that extensions can get access
     * to the underlying properties instance for manual property lookups.
     * This is to deal with the bug described here:
     * https://github.com/scorbo2/app-extensions/issues/5
     *
     * @return Our PropertiesManager instance
     */
    public PropertiesManager getPropertiesManager() {
        return propsManager;
    }

    @Override
    public void load() {
        super.load();
        ReloadUIAction.getInstance().actionPerformed(null);
    }

    /**
     * Basically only invoked once on app startup to kick things off
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
        return (lastBrowseDir.getDirectory() != null && lastBrowseDir.getDirectory().exists()) ? lastBrowseDir.getDirectory() : null;
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

    public FontFamily getVisualizerOverlayFont() {
        return visualizerOverlayFont.getSelectedItem();
    }

    public int getVisualizerOverlayFontSize() {
        return visualizerOverlayFontSize.getValue();
    }

    public VisualizationOverlay.OverlaySize getVisualizationOverlaySize() {
        return visualizerOverlaySize.getSelectedItem();
    }

    public float getVisualizationOverlayOpacity() {
        return (float) visualizerOverlayOpacity.getValue();
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

    public Color getVisualizerOverlayForeground() {
        return visualizerOverlayForeground.getColor();
    }

    public Color getVisualizerOverlayHeader() {
        return visualizerOverlayHeader.getColor();
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

    @Override
    protected List<AbstractProperty> createInternalProperties() {
        buttonSize = new EnumProperty<ButtonSize>("UI.General.buttonSize", "Control size:", ButtonSize.LARGE);
        controlAlignment = new EnumProperty<ControlAlignment>("UI.General.controlAlignment", "Control alignment:", ControlAlignment.CENTER);

        List<String> options = new ArrayList<>();
        for (AudioPanelIdleAnimation.Animation animation : AudioPanelIdleAnimation.getInstance().getAll()) {
            options.add(animation.getName());
        }
        int selectedIndex = (options.size() == 1) ? 0 : 1; // pick the first non-standard animation if there is one.
        idleAnimation = new ComboProperty("UI.General.idleAnimation", "Idle animation:", options, selectedIndex, false);

        options = new ArrayList<>();
        options.add("Use waveform settings from application theme");
        options.add("Override application theme with custom settings");
        overrideAppThemeWaveform = new ComboProperty("Waveform.Waveform graphics.override", "Waveform:", options, 0, false);

        WaveformConfig defaultConfig = new WaveformConfig();
        waveformBgColor = new ColorProperty("Waveform.Waveform graphics.bgColor", "Background:", ColorProperty.ColorType.SOLID, defaultConfig.getBgColor());
        waveformFillColor = new ColorProperty("Waveform.Waveform graphics.fillColor", "Fill:", ColorProperty.ColorType.SOLID, defaultConfig.getFillColor());
        waveformOutlineColor = new ColorProperty("Waveform.Waveform graphics.outlineColor", "Outline:", ColorProperty.ColorType.SOLID, defaultConfig.getOutlineColor());
        waveformOutlineThickness = new IntegerProperty("Waveform.Waveform graphics.outlineWidth", "Outline width:", defaultConfig.getOutlineThickness(), 0, 24, 1);

        options = new ArrayList<>();
        for (WaveformResolution res : WaveformResolution.values()) {
            options.add(res.name());
        }
        waveformResolution = new EnumProperty<WaveformResolution>("Waveform.Resolution.resolution", "Resolution:", WaveformResolution.HIGH);

        options = new ArrayList<>();
        for (AppTheme.Theme theme : AppTheme.getAll()) {
            options.add(theme.getName());
        }
        selectedIndex = (options.size() == 1) ? 0 : 1; // pick the first non-standard theme if there is one.
        applicationTheme = new ComboProperty("UI.Theme.theme", "Theme:", options, selectedIndex, false);

        shuffleEnabled = new BooleanProperty("hidden.props.shuffleEnabled", "shuffleEnabled", false);
        repeatEnabled = new BooleanProperty("hidden.props.repeatEnabled", "repeatEnabled", false);
        shuffleEnabled.setExposed(false);
        repeatEnabled.setExposed(false);

        windowWidth = new IntegerProperty("hidden.props.windowWidth", "windowWidth", MainWindow.DEFAULT_WIDTH);
        windowHeight = new IntegerProperty("hidden.props.windowHeight", "windowHeight", MainWindow.DEFAULT_HEIGHT);
        windowWidth.setExposed(false);
        windowHeight.setExposed(false);

        lastBrowseDir = new DirectoryProperty("hidden.props.browseDir", "browseDir", true);
        lastBrowseDir.setExposed(false);

        options = new ArrayList<>();
        for (VisualizationManager.Visualizer visualizer : VisualizationManager.getAll()) {
            options.add(visualizer.getName());
        }
        selectedIndex = (options.size() == 1) ? 0 : 1; // pick the first non-standard one if there is one
        visualizerType = new ComboProperty("Visualization.General.visualizer", "Visualizer:", options, selectedIndex, false);
        stopVisualizerOnFocusLost = new BooleanProperty("Visualization.General.stopOnFocusLost", "In single-monitor mode, stop visualizer on window focus lost", true);
        allowVisualizerOverride = new BooleanProperty("Visualization.General.allowOverride", "Allow override of selected visualizer based on file triggers", true);
        visualizerDisplay = new EnumProperty<>("Visualization.General.preferredDisplay", "Preferred display:", VisualizationWindow.DISPLAY.PRIMARY);
        visualizerSpeed = new EnumProperty<>("Visualization.General.animationSpeed", "Animation speed:", VisualizationThread.AnimationSpeed.HIGH);
        visualizerOverlayEnabled = new BooleanProperty("Visualization.Overlay.enabled", "Enable visualizer overlay for current track info", true);
        visualizerOverlayFont = new EnumProperty<>("Visualization.Overlay.fontFamily", "Overlay font:", FontFamily.SANS_SERIF);
        visualizerOverlayFontSize = new IntegerProperty("Visualization.Overlay.fontSize", "Font point size:", 12, 8, 99, 2);
        visualizerOverlaySize = new EnumProperty<>("Visualization.Overlay.size", "Overlay size:", VisualizationOverlay.OverlaySize.SMALL);
        visualizerOverlayOpacity = new DecimalProperty("Visualization.Overlay.opacity", "Overlay opacity:", 1.0, 0.0, 1.0, 0.1);
        visualizerOverlayBorderWidth = new IntegerProperty("Visualization.Overlay.borderWidth", "Border width:", 2, 0, 10, 1);
        visualizerOverlayOverrideTheme = new BooleanProperty("Visualization.Overlay.overrideTheme", "Override app theme and use the following colors:", false);
        visualizerOverlayBackground = new ColorProperty("Visualization.Overlay.normalBg", "Text background:", ColorProperty.ColorType.SOLID, Color.BLACK);
        visualizerOverlayForeground = new ColorProperty("Visualization.Overlay.normalFg", "Text foreground:", ColorProperty.ColorType.SOLID, Color.LIGHT_GRAY);
        visualizerOverlayHeader = new ColorProperty("Visualization.Overlay.header", "Header text:", ColorProperty.ColorType.SOLID, Color.WHITE);
        visualizerOverlayProgressBackground = new ColorProperty("Visualization.Overlay.progressBg", "Progress background:", ColorProperty.ColorType.SOLID, Color.DARK_GRAY);
        visualizerOverlayProgressForeground = new ColorProperty("Visualization.Overlay.progressFg", "Progress foreground:", ColorProperty.ColorType.SOLID, Color.BLUE);

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
                stopVisualizerOnFocusLost,
                allowVisualizerOverride,
                visualizerDisplay,
                visualizerSpeed,
                visualizerOverlayEnabled,
                visualizerOverlayFont,
                visualizerOverlayFontSize,
                visualizerOverlaySize,
                visualizerOverlayOpacity,
                visualizerOverlayBorderWidth,
                visualizerOverlayOverrideTheme,
                visualizerOverlayBackground,
                visualizerOverlayForeground,
                visualizerOverlayHeader,
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
     * Adds custom logic to enable/disable the waveform cosmetic properties depending on the value
     * of the override combo box, and sets the initial state of all relevant components.
     * This can only be done before the form panel is rendered.
     *
     * @param formPanels A list of unrendered FormPanels.
     */
    private void addWaveformOverrideFormBehaviour(List<FormPanel> formPanels) {
        final ComboField combo = (ComboField) PropertiesManager.findFormField("Waveform.Waveform graphics.override", formPanels);
        final FormField bgColorField = PropertiesManager.findFormField("Waveform.Waveform graphics.bgColor", formPanels);
        final FormField fillColorField = PropertiesManager.findFormField("Waveform.Waveform graphics.fillColor", formPanels);
        final FormField outlineColorField = PropertiesManager.findFormField("Waveform.Waveform graphics.outlineColor", formPanels);
        final FormField outlineWidthField = PropertiesManager.findFormField("Waveform.Waveform graphics.outlineWidth", formPanels);
        if (combo == null || bgColorField == null || fillColorField == null || outlineColorField == null || outlineWidthField == null) {
            logger.warning("addWaveformOverrideFormBehaviour: unable to locate required form fields.");
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
        CheckBoxField allowOverride = (CheckBoxField) PropertiesManager.findFormField("Visualization.Overlay.overrideTheme", formPanels);
        FormField overlayBg = PropertiesManager.findFormField("Visualization.Overlay.normalBg", formPanels);
        FormField overlayFg = PropertiesManager.findFormField("Visualization.Overlay.normalFg", formPanels);
        FormField headerFg = PropertiesManager.findFormField("Visualization.Overlay.header", formPanels);
        FormField progressBg = PropertiesManager.findFormField("Visualization.Overlay.progressBg", formPanels);
        FormField progressFg = PropertiesManager.findFormField("Visualization.Overlay.progressFg", formPanels);
        if (allowOverride == null || overlayBg == null || overlayFg == null || headerFg == null || progressFg == null || progressBg == null) {
            logger.warning("addOverlayOverrideFormBehaviour: unable to locate required form fields.");
            return;
        }

        // set initial state for these fields:
        overlayBg.setEnabled(allowOverride.isChecked());
        overlayFg.setEnabled(allowOverride.isChecked());
        headerFg.setEnabled(allowOverride.isChecked());
        progressBg.setEnabled(allowOverride.isChecked());
        progressFg.setEnabled(allowOverride.isChecked());

        // now allow it to change based on the override checkbox:
        allowOverride.addValueChangedAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                overlayBg.setEnabled(allowOverride.isChecked());
                overlayFg.setEnabled(allowOverride.isChecked());
                headerFg.setEnabled(allowOverride.isChecked());
                progressBg.setEnabled(allowOverride.isChecked());
                progressFg.setEnabled(allowOverride.isChecked());
            }
        });
    }
}
