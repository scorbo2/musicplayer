package ca.corbett.musicplayer.ui;

import ca.corbett.extensions.AppExtensionInfo;
import ca.corbett.extras.properties.AbstractProperty;
import ca.corbett.musicplayer.extensions.MusicPlayerExtension;
import ca.corbett.musicplayer.extensions.MusicPlayerExtensionManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AudioPanelIdleAnimationTest {

    @BeforeAll
    public static void setup() {
        // Don't load extensions for unit tests!!!
        System.setProperty("EXTENSIONS_DIR", "/tmp");
    }

    @Test
    public void getAll_withNoExtensions_shouldReturnBuiltInAnimations() {
        // GIVEN a stock setup with no extensions:
        AudioPanelIdleAnimation anim = AudioPanelIdleAnimation.getInstance();

        // WHEN we ask it for its animation list:
        AudioPanelIdleAnimation.Animation[] animations = anim.getAll();

        // THEN we should only see the built-in ones:
        assertEquals(4, animations.length);
        assertEquals("Standard", animations[0].getName());
        assertEquals("Rolling color wave", animations[1].getName());
    }

    @Test
    public void getAll_withExtensions_shouldReturnCustomAnimations() {
        // GIVEN an extension that supplies custom animations:
        AudioPanelIdleAnimation anim = AudioPanelIdleAnimation.getInstance();
        MusicPlayerExtensionManager.getInstance().addExtension(new CustomAnimation(), true);

        // WHEN we ask it for its animation list:
        AudioPanelIdleAnimation.Animation[] animations = anim.getAll();

        // THEN we should find our custom animation:
        assertEquals(5, animations.length);
        assertEquals("Rolling color wave", animations[2].getName());

        // AND when we disable the extension, the custom animation should go away:
        MusicPlayerExtensionManager.getInstance().setExtensionEnabled(CustomAnimation.class.getName(), false);
        animations = anim.getAll();
        assertEquals(4, animations.length);
    }

    private static class CustomAnimation extends MusicPlayerExtension {

        @Override
        public AppExtensionInfo getInfo() {
            return new AppExtensionInfo.Builder("CustomAnimations").build();
        }

        @Override
        protected List<AbstractProperty> createConfigProperties() {
            return List.of();
        }

        @Override
        public List<AudioPanelIdleAnimation.Animation> getCustomIdleAnimations() {
            List<AudioPanelIdleAnimation.Animation> list = new ArrayList<>();

            list.add(new AudioPanelIdleAnimation.Animation("CustomAnimation") {
                @Override
                public void stop() {
                }

                @Override
                public void run() {
                }
            });

            return list;
        }
    }
}