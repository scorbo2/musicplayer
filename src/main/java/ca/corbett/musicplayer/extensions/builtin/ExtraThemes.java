package ca.corbett.musicplayer.extensions.builtin;

import ca.corbett.extensions.AppExtensionInfo;
import ca.corbett.extras.properties.AbstractProperty;
import ca.corbett.musicplayer.Version;
import ca.corbett.musicplayer.extensions.MusicPlayerExtension;
import ca.corbett.musicplayer.ui.AppTheme;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * A collection of extra themes for MusicPlayer 2.
 *
 * @author scorbo2
 * @since 2025-03-29
 */
public class ExtraThemes extends MusicPlayerExtension {
    private final AppExtensionInfo info;

    public ExtraThemes() {
        info = new AppExtensionInfo.Builder("Extra themes")
                .setAuthor("Steve Corbett")
                .setShortDescription("A collection of extra themes for MusicPlayer.")
                .setLongDescription("Enable this extension for an assortment of extra themes for MusicPlayer.")
                .setTargetAppName(Version.NAME)
                .setTargetAppVersion(Version.VERSION)
                .setReleaseNotes("[2025-03-29] for the MusicPlayer 2.0 release")
                .setVersion(Version.VERSION)
                .build();
    }

    @Override
    public AppExtensionInfo getInfo() {
        return info;
    }

    @Override
    public List<AbstractProperty> getConfigProperties() {
        return List.of();
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }

    @Override
    public List<AppTheme.Theme> getCustomThemes() {
        List<AppTheme.Theme> themes = new ArrayList<>();

        themes.add(new MatrixTheme());
        themes.add(new BluesTheme());
        themes.add(new HotDogStandTheme());
        themes.add(new GreyscaleTheme());

        return themes;
    }

    /**
     * A green and black theme possibly inspired by some kind of film.
     */
    public static class MatrixTheme extends AppTheme.Theme {
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

    /**
     * A dark-ish blue-ish theme.
     */
    public static class BluesTheme extends AppTheme.Theme {
        public BluesTheme() {
            super("I've got the blues");
            dialogBgColor = new Color(20, 20, 128);
            normalBgColor = new Color(20, 20, 76);
            normalFgColor = new Color(48, 48, 255);
            selectedBgColor = Color.BLUE;
            selectedFgColor = Color.WHITE;
            headerBgColor = new Color(48, 48, 196);
            headerFgColor = Color.WHITE;
            waveformConfig.setFillColor(new Color(0, 0, 128));
            waveformConfig.setOutlineColor(Color.BLUE);
            waveformConfig.setOutlineThickness(1);
            waveformConfig.setBgColor(Color.BLACK);
            waveformConfig.setBaselineEnabled(false);
        }
    }

    /**
     * A terrible theme included just for the fun of it, and for the name.
     */
    public static class HotDogStandTheme extends AppTheme.Theme {
        public HotDogStandTheme() {
            super("Hot dog stand");
            dialogBgColor = Color.YELLOW;
            normalBgColor = Color.ORANGE;
            normalFgColor = Color.RED;
            selectedBgColor = Color.RED;
            selectedFgColor = Color.ORANGE;
            headerBgColor = new Color(196, 64, 64);
            headerFgColor = Color.WHITE;
            waveformConfig.setFillColor(Color.RED);
            waveformConfig.setOutlineColor(Color.YELLOW);
            waveformConfig.setOutlineThickness(2);
            waveformConfig.setBgColor(Color.ORANGE);
            waveformConfig.setBaselineEnabled(false);
        }
    }

    public static class GreyscaleTheme extends AppTheme.Theme {
        public GreyscaleTheme() {
            super("50 shades of grayscale");
            dialogBgColor = Color.LIGHT_GRAY;
            normalBgColor = new Color(100, 100, 100);
            normalFgColor = Color.BLACK;
            selectedBgColor = Color.GRAY;
            selectedFgColor = Color.WHITE;
            headerBgColor = Color.LIGHT_GRAY;
            headerFgColor = Color.BLACK;
            waveformConfig.setFillColor(Color.LIGHT_GRAY);
            waveformConfig.setOutlineColor(Color.WHITE);
            waveformConfig.setOutlineThickness(1);
            waveformConfig.setBgColor(new Color(100, 100, 100));
            waveformConfig.setBaselineEnabled(false);
        }
    }
}
