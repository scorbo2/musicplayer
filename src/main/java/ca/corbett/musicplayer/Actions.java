package ca.corbett.musicplayer;

import ca.corbett.musicplayer.actions.AboutAction;
import ca.corbett.musicplayer.actions.ExtensionsManagerAction;
import ca.corbett.musicplayer.actions.FullScreenAction;
import ca.corbett.musicplayer.actions.NextAction;
import ca.corbett.musicplayer.actions.PauseAction;
import ca.corbett.musicplayer.actions.PlayAction;
import ca.corbett.musicplayer.actions.PlaylistAddAction;
import ca.corbett.musicplayer.actions.PlaylistOpenAction;
import ca.corbett.musicplayer.actions.PlaylistRemoveAllAction;
import ca.corbett.musicplayer.actions.PlaylistRemoveOneAction;
import ca.corbett.musicplayer.actions.PlaylistSaveAction;
import ca.corbett.musicplayer.actions.PlaylistSortAction;
import ca.corbett.musicplayer.actions.PlaylistTrackInfoAction;
import ca.corbett.musicplayer.actions.PrevAction;
import ca.corbett.musicplayer.actions.RepeatAction;
import ca.corbett.musicplayer.actions.SettingsAction;
import ca.corbett.musicplayer.actions.ShuffleAction;
import ca.corbett.musicplayer.actions.StopAction;
import ca.corbett.musicplayer.extensions.MusicPlayerExtensionManager;
import ca.corbett.musicplayer.ui.MainWindow;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Groups all the media player and playlist actions together into one place
 * to make it easier to expose them to the user (attaching them to buttons
 * or menu items or keyboard shortcuts or whatever we need).
 * <p>
 * The built-in actions are augmented by whatever actions are supplied by
 * loaded and enabled extensions, if any.
 * </p>
 *
 * @author scorbo2
 * @since 2025-03-23
 */
public final class Actions {

    public static final MPAction[] mediaPlayerActions = {
            new MPAction("Play", "Play (space)", "media-playback-start.png", new PlayAction()),
            new MPAction("Pause", "Pause (space)", "media-playback-pause.png", new PauseAction()),
            new MPAction("Stop", "Stop (esc)", "media-playback-stop.png", new StopAction()),
            new MPAction("Previous", "Previous (left)", "media-skip-backward.png", new PrevAction()),
            new MPAction("Next", "Next (right)", "media-skip-forward.png", new NextAction()),
            new MPAction("Fullscreen", "Fullscreen (v)", "icon-fullscreen.png", new FullScreenAction()),
            new MPAction("Settings", "Preferences (ctrl+p)", "icon-preferences.png", new SettingsAction()),
            new MPAction("Extensions", "Extension Manager", "icon-extensions2.png", new ExtensionsManagerAction()),
            new MPAction("About", "About (ctrl+a)", "logo.png", new AboutAction())
    };

    public static final MPAction[] playlistActions = {
            new MPAction("Open", "Open (ctrl+o)", "icon-open.png", new PlaylistOpenAction()),
            new MPAction("Save", "Save (ctrl+s)", "icon-save.png", new PlaylistSaveAction()),
            new MPAction("Add", "Add to playlist", "icon-add.png", new PlaylistAddAction()),
            new MPAction("Remove selected", "Remove selected", "icon-remove-single.png", new PlaylistRemoveOneAction()),
            new MPAction("Clear", "Clear playlist", "icon-remove-all.png", new PlaylistRemoveAllAction()),
        new MPAction("Playlist sort", "Sort playlist", "icon-sort.png", new PlaylistSortAction()),
            new MPAction("Track info", "Track info", "icon-popup.png", new PlaylistTrackInfoAction()),
            new MPAction("Shuffle", "Shuffle", "media-playlist-shuffle.png", new ShuffleAction()),
            new MPAction("Repeat", "Repeat", "media-playlist-repeat.png", new RepeatAction())
    };

    /**
     * Returns all MPActions that relate to the media player.
     * This includes all the built-in actions, combined with
     * any actions supplied by musicplayer extensions, if any.
     *
     * @return An array of MPActions.
     */
    public static MPAction[] getMediaPlayerActions() {
        // Ugly kludge alert: I want any extension-supplied actions to show up before
        // the "settings" and "about" buttons, because those feel to me like they
        // should be the last actions on the toolbar. So, I have to fiddle with
        // the array of built-in actions and the List of extension-supplied actions
        // to splice them together in just the right way:
        List<MPAction> extensionActions = MusicPlayerExtensionManager.getInstance().getMediaPlayerActions();
        MPAction[] combinedArray = new MPAction[mediaPlayerActions.length + extensionActions.size()];

        // Start by adding all the built-in ones:
        System.arraycopy(mediaPlayerActions, 0, combinedArray, 0, mediaPlayerActions.length);

        // If there are no extension actions, then we're done at this point:
        if (extensionActions.isEmpty()) {
            return combinedArray;
        }

        // Make sure "settings" and "about" show up at the end of the array:
        System.arraycopy(mediaPlayerActions, mediaPlayerActions.length - 2, combinedArray, combinedArray.length - 2, 2);

        // Now insert all the extension-supplied ones in between:
        int index = mediaPlayerActions.length - 2;
        for (MPAction action : extensionActions) {
            combinedArray[index++] = action;
        }

        return combinedArray;
    }

    /**
     * Returns all MPActions that relate to the playlist.
     * This includes all the built-in actions, combined with
     * any actions supplied by musicplayer extensions, if any.
     *
     * @return An array of MPActions.
     */
    public static MPAction[] getPlaylistActions() {
        List<MPAction> extensionActions = MusicPlayerExtensionManager.getInstance().getPlaylistActions();
        MPAction[] combinedArray = new MPAction[playlistActions.length + extensionActions.size()];
        System.arraycopy(playlistActions, 0, combinedArray, 0, playlistActions.length);
        int index = playlistActions.length;
        for (MPAction action : extensionActions) {
            combinedArray[index++] = action;
        }
        return combinedArray;
    }

    /**
     * Given an MPAction, this will build a toolbar button to represent that
     * action. That means sizing the button appropriately (as per the currently
     * configured control size in AppConfig), and setting the correct
     * button icon and tooltip. The button will also be equipped with
     * the appropriate Action to be triggered when the button is pressed.
     *
     * @param action An MPAction instance.
     * @return A JButton suitable for a toolbar.
     */
    public static JButton buildButton(Actions.MPAction action) {
        int btnSize = AppConfig.getInstance().getButtonSize().getButtonSize();
        int iconSize = btnSize - 2; // small margin for icon to fit within button
        JButton button = new JButton(action.action);
        button.setName(action.name);
        button.setText("");
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusable(false);
        button.setPreferredSize(new Dimension(btnSize, btnSize));
        BufferedImage iconImage = MainWindow.loadIconResource(action.iconResource, iconSize, iconSize);
        ImageIcon icon = new ImageIcon(iconImage, action.description);
        button.setIcon(icon);
        button.setToolTipText(action.description);

        return button;
    }

    /**
     * Simple wrapper class to encapsulate the stuff we need to represent
     * a MusicPlayer action. The fields in this class used to be final.
     * But, in the interest of allowing extensions to do more interesting
     * things, I have made them public and mutable. This allows an extension
     * to muck with them, to change the tooltip or the associated action,
     * or even to re-skin the application by supplying a different icon resource.
     */
    public static class MPAction {
        public String iconResource;
        public String name;
        public String description;
        public AbstractAction action;

        /**
         * Creates a new MPAction with the specified parameters.
         * It's public here so extensions can provide custom actions.
         *
         * @param name         A unique descriptive name for this action.
         * @param description  A very brief description of the action, used in tooltips.
         * @param iconResource A resource path for loading the icon for this action.
         *                     Refer to the bundled icon-*.png icons for sizing and palette matching.
         *                     Example value: "/ca/corbett/musicplayer/images/icon-preferences.png".
         * @param action       An AbstractAction to be invoked when the users selects this action.
         */
        public MPAction(String name, String description, String iconResource, AbstractAction action) {
            this.iconResource = "/ca/corbett/musicplayer/images/" + iconResource;
            this.name = name;
            this.description = description;
            this.action = action;
            this.action.putValue(Action.NAME, name);
            this.action.putValue(Action.SHORT_DESCRIPTION, description);
        }
    }
}
