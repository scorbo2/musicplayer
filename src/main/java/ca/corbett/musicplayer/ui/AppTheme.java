package ca.corbett.musicplayer.ui;

import ca.corbett.extras.audio.WaveformConfig;
import ca.corbett.musicplayer.extensions.MusicPlayerExtensionManager;

import java.awt.Color;
import java.util.List;
import java.util.logging.Logger;

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

    private static final Logger logger = Logger.getLogger(AppTheme.class.getName());

    /**
     * Out of the box, we support one very boring theme.
     * But, our built-in ExtraThemes extension will bring
     * in a few extra ones on initial startup, and will
     * show off what we can do with application extensions.
     */
    public static final Theme STANDARD = new StandardTheme();

    /**
     * Returns a list of all available application themes - this is both the built-in
     * theme that we support out of the box, along with any custom themes supplied
     * by any registered extensions. The built-in theme will always appear first
     * in the returned array. The user can choose between these themes in the
     * application settings dialog.
     *
     * @return An array of all supported themes.
     */
    public static Theme[] getAll() {
        List<Theme> extensionThemes = MusicPlayerExtensionManager.getInstance().getCustomThemes();
        Theme[] allThemes = new Theme[1 + extensionThemes.size()];
        allThemes[0] = STANDARD;
        int index = 1;
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

        logger.warning("Requested theme \"" + name + "\" not found; using STANDARD as default.");
        return STANDARD; // arbitrary default in case of garbage data
    }

    public static Color getOffsetColor(Color input) {
        final int DEFAULT_OFFSET = -35;
        return getOffsetColor(input, DEFAULT_OFFSET);
    }

    /**
     * Returns a slightly darker shade of whatever color you supply.
     * I'm trying to make toggle buttons look like toggle buttons, but this
     * is a bit wonky. Yeah, I know JToggleButton is a thing but I don't
     * want to have borders on these toolbar buttons.
     * <p>
     * Update: there's a wonky case where you might have created
     * a theme with a black background, and of course we can't
     * make a darker shade of black. So, if your supplied color
     * is black, or very close to black, this method will instead
     * return a lighter shade of that color, despite the method name.
     * The joys of supporting a highly customizable environment!
     * </p>
     *
     * @param input Any color.
     * @return A slightly darker shade of that color.
     */
    public static Color getOffsetColor(Color input, int offset) {
        int delta = offset;

        // wonky special case if darkening: don't try to darken an already dark color:
        if (delta < 0) {
            final int limit = Math.abs(delta);
            if (input.getRed() < limit && input.getGreen() < limit && input.getBlue() < limit) {
                delta = limit;
            }
        }

        // wonky special case if lightening: don't try to lighten an already light color:
        else {
            final int limit = 255 - offset;
            if (input.getRed() > limit && input.getGreen() > limit && input.getBlue() > limit) {
                delta = -delta;
            }
        }

        // darken it:
        Color output;
        if (delta < 0) {
            int red = Math.max(input.getRed() + delta, 0);
            int green = Math.max(input.getGreen() + delta, 0);
            int blue = Math.max(input.getBlue() + delta, 0);
            output = new Color(red, green, blue);
        }

        // or lighten it:
        else {
            int red = Math.min(input.getRed() + delta, 255);
            int green = Math.min(input.getGreen() + delta, 255);
            int blue = Math.min(input.getBlue() + delta, 255);
            output = new Color(red, green, blue);
        }

        return output;
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

    /**
     * Our very boring built-in hard-coded theme.
     * Refer also to the ExtraThemes extension for more interesting options.
     */
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
