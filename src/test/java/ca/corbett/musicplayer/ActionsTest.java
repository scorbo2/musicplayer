package ca.corbett.musicplayer;

import ca.corbett.extensions.AppExtensionInfo;
import ca.corbett.extras.properties.AbstractProperty;
import ca.corbett.musicplayer.extensions.MusicPlayerExtension;
import ca.corbett.musicplayer.extensions.MusicPlayerExtensionManager;
import org.junit.jupiter.api.Test;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ActionsTest {

    @Test
    public void getMediaPlayerActions_withExtension_shouldReturnExtensionAction() {
        // GIVEN the presence of an extension that supplies a mediaplayer action:
        MusicPlayerExtensionManager.getInstance().addExtension(new SomeExtension(), true);

        // WHEN we get the array of media player actions:
        Actions.MPAction[] actions = Actions.getMediaPlayerActions();

        // THEN we should find our custom action in there:
        assertNotNull(findActionOrNull("TESTMP", actions));

        // AND when we disable the extension:
        MusicPlayerExtensionManager.getInstance().setExtensionEnabled(SomeExtension.class.getName(), false);
        actions = Actions.getMediaPlayerActions();

        // THEN we should no longer find our action:
        assertNull(findActionOrNull("TESTMP", actions));
    }

    @Test
    public void getPlaylistActions_withExtension_shouldReturnExtensionAction() {
        // GIVEN the presence of an extension that supplies a playlist action:
        MusicPlayerExtensionManager.getInstance().addExtension(new SomeExtension(), true);

        // WHEN we get the array of playlist actions:
        Actions.MPAction[] actions = Actions.getPlaylistActions();

        // THEN we should find our custom action in there:
        assertNotNull(findActionOrNull("TESTPL", actions));

        // AND when we disable the extension:
        MusicPlayerExtensionManager.getInstance().setExtensionEnabled(SomeExtension.class.getName(), false);
        actions = Actions.getPlaylistActions();

        // THEN we should no longer find our action:
        assertNull(findActionOrNull("TESTPL", actions));
    }

    private Actions.MPAction findActionOrNull(String name, Actions.MPAction[] arr) {
        for (Actions.MPAction action : arr) {
            if (name.equals(action.name)) {
                return action;
            }
        }
        return null;
    }

    private static class SomeExtension extends MusicPlayerExtension {

        @Override
        public AppExtensionInfo getInfo() {
            return null;
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
        public List<Actions.MPAction> getMediaPlayerActions() {
            List<Actions.MPAction> list = new ArrayList<>();
            list.add(new Actions.MPAction("TESTMP", "TESTMP", "", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                }
            }));
            return list;
        }

        @Override
        public List<Actions.MPAction> getPlaylistActions() {
            List<Actions.MPAction> list = new ArrayList<>();
            list.add(new Actions.MPAction("TESTPL", "TESTPL", "", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                }
            }));
            return list;
        }
    }

}