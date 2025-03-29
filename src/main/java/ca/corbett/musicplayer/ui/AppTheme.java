package ca.corbett.musicplayer.ui;

import ca.corbett.extras.audio.WaveformConfig;
import ca.corbett.musicplayer.extensions.MusicPlayerExtensionManager;

import java.awt.Color;
import java.util.List;

/**
 * Represents a way to change the appearance of UI elements that are
 * used throughout the UI. Go through AppConfig.getAppTheme() to find
 * the current theme chosen by the user. Extensions can supply
 * additional themes that the user can select in AppConfig.
 * <p>
 *     Reloading the UI<br>
 *     When the user changes the currently selected theme in AppConfig,
 *     we reload the entire UI so that the new theme is instantly
 *     displayed across the application. Any UI class that wants to
 *     respond to such an event can register itself with the
 *     ReloadUIAction class to receive notification when
 *     the theme has changed.
 * </p>
 *
 * @author scorbo2
 * @since 2025-03-25
 */
public class AppTheme {

    public static final Theme MATRIX = new MatrixTheme();
    public static final Theme STANDARD = new StandardTheme();

    /**
     * Returns a list of all available application themes - this is both the built-in
     * themes that we support out of the box, along with any custom themes supplied
     * by any registered extensions. The built-in themes will always appear first
     * in the returned array. The user can choose between these themes in the
     * application settings dialog.
     *
     * @return An array of all supported themes.
     */
    public static Theme[] getAll() {
        List<Theme> extensionThemes = MusicPlayerExtensionManager.getInstance().getCustomThemes();
        Theme[] allThemes = new Theme[2 + extensionThemes.size()];
        allThemes[0] = MATRIX;
        allThemes[1] = STANDARD;
        int index = 2;
        for (Theme theme : extensionThemes) {
            allThemes[index++] = theme;
        }
        return allThemes;
    }

    public static Theme getTheme(String name) {
        for (Theme theme : getAll()) {
            if (theme.name.equals(name)) {
                return theme;
            }
        }

        return MATRIX; // arbitrary default in case of garbage data
    }

    /**
     * Extensions can extend this class and return an instance of it to supply
     * a new custom theme that the user can select in AppConfig.
     */
    public static abstract class Theme {
        protected final String name;
        protected Color dialogBgColor;
        protected Color normalBgColor;
        protected Color normalFgColor;
        protected Color selectedBgColor;
        protected Color selectedFgColor;
        protected Color headerBgColor;
        protected Color headerFgColor;
        protected WaveformConfig waveformConfig;

        /**
         * A theme must have a name that will be displayed to the
         * user when selecting the application theme. Ideally the
         * name should be unique and not horribly long.
         *
         * @param name A name for this theme.
         */
        public Theme(String name) {
            this.name = (name == null || name.isBlank()) ? "Unnamed theme" : name;
            waveformConfig = new WaveformConfig(); // default, can be overridden
        }

        /**
         * Returns the hopefully unique and hopefully descriptive
         * name of this theme.
         *
         * @return The theme name
         */
        public String getName() {
            return name;
        }

        /**
         * Returns the color that is used as the background of MOST dialogs
         * and panels.
         *
         * @return A background color for dialogs.
         */
        public Color getDialogBgColor() {
            return dialogBgColor;
        }

        /**
         * Returns the background color for "normal" text throughout the application.
         * This is text that is not selected or highlighted in some way.
         *
         * @return A normal text background color.
         */
        public Color getNormalBgColor() {
            return normalBgColor;
        }

        /**
         * Returns the foreground color for "normal" text throughout the application.
         * This is text that is not selected or highlighted in some way.
         *
         * @return A normal text foreground color.
         */
        public Color getNormalFgColor() {
            return normalFgColor;
        }

        /**
         * Returns the background color for "selected" text throughout the application.
         * This is text that is selected or highlighted in some way.
         *
         * @return A selected text background color.
         */
        public Color getSelectedBgColor() {
            return selectedBgColor;
        }

        /**
         * Returns the foreground color for "selected" text throughout the application.
         * This is text that is selected or highlighted in some way.
         *
         * @return A selected text foreground color.
         */
        public Color getSelectedFgColor() {
            return selectedFgColor;
        }

        /**
         * Returns the background color for "header" text throughout the application.
         * This is text that is being used as some kind of section header.
         *
         * @return A header text background color.
         */
        public Color getHeaderBgColor() {
            return headerBgColor;
        }

        /**
         * Returns the foreground color for "header" text throughout the application.
         * This is text that is being used as some kind of section header.
         *
         * @return A header text foreground color.
         */
        public Color getHeaderFgColor() {
            return headerFgColor;
        }

        /**
         * Returns a waveform config for this application theme. If your theme
         * does not specify one, a default will be used. The user can optionally
         * override the config specified here by specifying their own in AppConfig.
         *
         * @return A WaveformConfig, or null.
         */
        public WaveformConfig getWaveformConfig() {
            return waveformConfig;
        }
    }

    public static class MatrixTheme extends Theme {
        public MatrixTheme() {
            super("Matrix");
            dialogBgColor = Color.GRAY;
            normalBgColor = Color.BLACK;
            normalFgColor = Color.GREEN;
            selectedBgColor = Color.GREEN;
            selectedFgColor = Color.BLACK;
            headerBgColor = Color.GRAY;
            headerFgColor = Color.LIGHT_GRAY;
            waveformConfig.setFillColor(new Color(0, 128, 0));
            waveformConfig.setOutlineColor(Color.GREEN);
            waveformConfig.setOutlineThickness(1);
            waveformConfig.setBgColor(Color.BLACK);
            waveformConfig.setBaselineEnabled(false);
        }
    }

    public static class StandardTheme extends Theme {
        public StandardTheme() {
            super("Standard");
            dialogBgColor = Color.LIGHT_GRAY;
            normalBgColor = Color.WHITE;
            normalFgColor = Color.BLACK;
            selectedBgColor = Color.BLUE;
            selectedFgColor = Color.WHITE;
            headerBgColor = Color.LIGHT_GRAY;
            headerFgColor = Color.BLACK;
        }
    }
}
