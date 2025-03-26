package ca.corbett.musicplayer.ui;

import java.awt.Color;

/**
 * Represents a way to change the appearance (mostly colors) that are
 * used throughout the UI. Go through AppConfig.getAppTheme() to find
 * the current theme chosen by the user. Register your UI class with
 * ReloadUIAction to receive a message when the theme has changed
 * so you can redraw yourself.
 *
 * @author scorbo2
 * @since 2025-03-25
 */
public class AppTheme {

    public static final Theme MATRIX = new Theme("Matrix", Color.GRAY, Color.BLACK, Color.GREEN, Color.GREEN, Color.BLACK, Color.GRAY, Color.LIGHT_GRAY);
    public static final Theme STANDARD = new Theme("Standard", Color.LIGHT_GRAY, Color.WHITE, Color.BLACK, Color.BLUE, Color.WHITE, Color.LIGHT_GRAY, Color.BLACK);

    /**
     * TODO also interrogate extensions
     *
     * @return An array of all supported themes.
     */
    public static Theme[] getAll() {
        return new Theme[]{
                MATRIX,
                STANDARD
        };
    }

    public static Theme getTheme(String name) {
        for (Theme theme : getAll()) {
            if (theme.name.equals(name)) {
                return theme;
            }
        }

        return MATRIX; // arbitrary default in case of garbage data
    }

    public static class Theme {
        public final String name;
        public final Color dialogBgColor;
        public final Color normalBgColor;
        public final Color normalFgColor;
        public final Color selectedBgColor;
        public final Color selectedFgColor;
        public final Color headerBgColor;
        public final Color headerFgColor;

        public Theme(String name, Color dialogBg, Color bg, Color fg, Color selectedBg, Color selectedFg, Color headerBgColor, Color headerFgColor) {
            this.name = name;
            dialogBgColor = dialogBg;
            normalBgColor = bg;
            normalFgColor = fg;
            selectedBgColor = selectedBg;
            selectedFgColor = selectedFg;
            this.headerBgColor = headerBgColor;
            this.headerFgColor = headerFgColor;
        }
    }
}
