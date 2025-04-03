package ca.corbett.musicplayer.ui;

import ca.corbett.extensions.AppExtensionInfo;
import ca.corbett.extras.properties.AbstractProperty;
import ca.corbett.musicplayer.extensions.MusicPlayerExtension;
import ca.corbett.musicplayer.extensions.MusicPlayerExtensionManager;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppThemeTest {

    @Test
    public void getAll_withNoExtensions_shouldReturnBuiltInThemes() {
        // GIVEN a stock setup with no registered extensions:
        // WHEN we ask it for all themes:
        AppTheme.Theme[] themes = AppTheme.getAll();

        // THEN we should only see the built-in themes:
        assertEquals(5, themes.length);
        assertEquals("Standard", themes[0].getName());
        assertEquals("Matrix", themes[1].getName());
    }

    @Test
    public void getAll_withExtension_shouldReturnCustomThemes() {
        // GIVEN a setup with an extension that supplies custom themes:
        MusicPlayerExtensionManager.getInstance().addExtension(new TestExtension(), true);

        // WHEN we ask it for a list of themes:
        AppTheme.Theme[] themes = AppTheme.getAll();

        // THEN we should see our custom themes in addition to the built-in ones:
        assertEquals(7, themes.length);
        assertEquals("test1", themes[5].getName());
        assertEquals("test2", themes[6].getName());

        // AND when we disable our extension, those themes should go away:
        MusicPlayerExtensionManager.getInstance().setExtensionEnabled(TestExtension.class.getName(), false);
        themes = AppTheme.getAll();
        assertEquals(5, themes.length);
    }

    private static class TestExtension extends MusicPlayerExtension {

        @Override
        public AppExtensionInfo getInfo() {
            return new AppExtensionInfo.Builder("textextension").build();
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
            return List.of(new AppTheme.Theme("test1") {
            }, new AppTheme.Theme("test2") {
            });
        }
    }
}