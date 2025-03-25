package ca.corbett.musicplayer.ui;

import java.awt.Color;

public class PlaylistTheme {

    public static final Theme MATRIX = new Theme("Matrix", Color.BLACK, Color.GREEN, Color.GREEN, Color.BLACK);
    public static final Theme STANDARD = new Theme("Standard", Color.WHITE, Color.BLACK, Color.BLUE, Color.WHITE);

    /**
     * TODO also interrogate extensions
     * TODO is this class just for the playlist? can we extrapolate to an application-wide theme?
     *
     * @return An array of all supported playlist themes.
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
        public final Color bgColor;
        public final Color fgColor;
        public final Color selectedBgColor;
        public final Color selectedFgColor;

        public Theme(String name, Color bg, Color fg, Color selectedBg, Color selectedFg) {
            this.name = name;
            bgColor = bg;
            fgColor = fg;
            selectedBgColor = selectedBg;
            selectedFgColor = selectedFg;
        }
    }
}
