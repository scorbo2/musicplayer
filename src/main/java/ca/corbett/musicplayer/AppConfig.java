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
import javax.swing.JTabbedPane;
import java.awt.Color;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

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
 * TODO add a property for extension load dir (requires restart)
 *      but if extension manager is created before appconfig is loaded, how does it know where to go? hmm...
 *      it might have to be a system property, and we just display it read-only on the properties dialog
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
    private EnumProperty<VisualizationWindow.DISPLAY> visualizerDisplay;
    private EnumProperty<VisualizationThread.AnimationSpeed> visualizerSpeed;
    private BooleanProperty visualizerOverlayEnabled;
    private EnumProperty<FontFamily> visualizerOverlayFont;
    private IntegerProperty visualizerOverlayFontSize;
    private EnumProperty<VisualizationOverlay.OverlaySize> visualizerOverlaySize;
    private DecimalProperty visualizerOverlayOpacity;

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
        visualizerDisplay = new EnumProperty<>("Visualization.General.preferredDisplay", "Preferred display:", VisualizationWindow.DISPLAY.PRIMARY);
        visualizerSpeed = new EnumProperty<>("Visualization.General.animationSpeed", "Animation speed:", VisualizationThread.AnimationSpeed.HIGH);
        visualizerOverlayEnabled = new BooleanProperty("Visualization.Overlay.enabled", "Enable visualizer overlay for current track info", true);
        visualizerOverlayFont = new EnumProperty<>("Visualization.Overlay.fontFamily", "Overlay font:", FontFamily.SANS_SERIF);
        visualizerOverlayFontSize = new IntegerProperty("Visualization.Overlay.fontSize", "Font point size:", 12, 8, 99, 2);
        visualizerOverlaySize = new EnumProperty<>("Visualization.Overlay.size", "Overlay size:", VisualizationOverlay.OverlaySize.SMALL);
        visualizerOverlayOpacity = new DecimalProperty("Visualization.Overlay.opacity", "Overlay opacity:", 1.0, 0.0, 1.0, 0.1);

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
                visualizerDisplay,
                visualizerSpeed,
                visualizerOverlayEnabled,
                visualizerOverlayFont,
                visualizerOverlayFontSize,
                visualizerOverlaySize,
                visualizerOverlayOpacity);
    }

    /**
     * HACK HACK HACK Kludge alert! In order to add custom logic to the generated
     * properties dialog, we have to override this and add a bunch of hacky code.
     * I've created a ticket to deal with this in the upstream swing-extras
     * and app-extensions libraries, but until I get to those tickets, this
     * code exists here in the downstream application.
     *
     * @param owner The owning Frame (so we can make the dialog modal to that Frame).
     * @return true if the user OK'd the dialog with changes.
     */
    @Override
    public boolean showPropertiesDialog(Frame owner) {
        List<String> categories = propsManager.getCategories();
        if (categories.isEmpty()) {
            return false; // won't happen in our case but whatever
        }
        List<FormPanel> formPanels = new ArrayList<>();
        for (String category : categories) {
            formPanels.add(propsManager.generateUnrenderedFormPanel(category, true, 24));
        }
        addCustomFormBehaviour(formPanels);

        // If there's only one category, just wrap it in a single form panel:
        PropertiesDialog dialog;
        if (formPanels.size() == 1) {
            formPanels.get(0).render();
            dialog = new PropertiesDialog(propsManager, owner, Version.NAME + " properties", formPanels.get(0));
        }

        // If there's more than one category, wrap it all in a tab pane:
        else {
            JTabbedPane tabPane = new JTabbedPane();
            int index = 0;
            for (String category : categories) {
                FormPanel formPanel = formPanels.get(index++);
                formPanel.render();
                tabPane.addTab(category, formPanel);
            }
            dialog = new PropertiesDialog(propsManager, owner, Version.NAME + " properties", tabPane);
        }

        dialog.setVisible(true);
        if (dialog.wasOkayed()) {
            save();
        }

        return dialog.wasOkayed();
    }

    private void addCustomFormBehaviour(List<FormPanel> formPanels) {
        final ComboField combo = (ComboField) findFormField(formPanels, "Waveform.Waveform graphics.override");
        final FormField bgColorField = findFormField(formPanels, "Waveform.Waveform graphics.bgColor");
        final FormField fillColorField = findFormField(formPanels, "Waveform.Waveform graphics.fillColor");
        final FormField outlineColorField = findFormField(formPanels, "Waveform.Waveform graphics.outlineColor");
        final FormField outlineWidthField = findFormField(formPanels, "Waveform.Waveform graphics.outlineWidth");
        if (combo != null && bgColorField != null && fillColorField != null && outlineColorField != null && outlineWidthField != null) {

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
        } else {
            logger.warning("addCustomFormBehaviour: unable to locate required form fields.");
        }
    }

    private FormField findFormField(List<FormPanel> formPanels, String fieldIdentifier) {
        for (FormPanel formPanel : formPanels) {
            List<FormField> fields = formPanel.getFormFields();
            for (FormField field : fields) {
                String id = field.getIdentifier();
                if (id != null && id.equals(fieldIdentifier)) {
                    return field;
                }
            }
        }
        return null;
    }
}
