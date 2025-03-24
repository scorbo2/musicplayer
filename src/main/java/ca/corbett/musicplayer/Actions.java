package ca.corbett.musicplayer;

import ca.corbett.musicplayer.actions.AboutAction;
import ca.corbett.musicplayer.actions.AddAction;
import ca.corbett.musicplayer.actions.FullScreenAction;
import ca.corbett.musicplayer.actions.NextAction;
import ca.corbett.musicplayer.actions.OpenAction;
import ca.corbett.musicplayer.actions.PauseAction;
import ca.corbett.musicplayer.actions.PlayAction;
import ca.corbett.musicplayer.actions.PlaylistAction;
import ca.corbett.musicplayer.actions.PrevAction;
import ca.corbett.musicplayer.actions.RemoveAllAction;
import ca.corbett.musicplayer.actions.RemoveOneAction;
import ca.corbett.musicplayer.actions.RepeatAction;
import ca.corbett.musicplayer.actions.SaveAction;
import ca.corbett.musicplayer.actions.SettingsAction;
import ca.corbett.musicplayer.actions.ShuffleAction;
import ca.corbett.musicplayer.actions.StopAction;
import ca.corbett.musicplayer.ui.MainWindow;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import java.awt.Dimension;
import java.awt.image.BufferedImage;

public final class Actions {

    public static final MPAction PLAY;
    public static final MPAction PAUSE;
    public static final MPAction STOP;
    public static final MPAction PREV;
    public static final MPAction NEXT;
    public static final MPAction FULLSCREEN;
    public static final MPAction PLAYLIST;
    public static final MPAction SETTINGS;
    public static final MPAction ABOUT;

    public static final MPAction OPEN;
    public static final MPAction SAVE;
    public static final MPAction SHUFFLE;
    public static final MPAction REPEAT;
    public static final MPAction REMOVE_ONE;
    public static final MPAction REMOVE_ALL;
    public static final MPAction ADD;

    static {
        PLAY = new MPAction("Play", "Play (space)", "media-playback-start.png", new PlayAction());
        PAUSE = new MPAction("Pause", "Pause (space)", "media-playback-pause.png", new PauseAction());
        STOP = new MPAction("Stop", "Stop (esc)", "media-playback-stop.png", new StopAction());
        PREV = new MPAction("Previous", "Previous (left/up)", "media-skip-backward.png", new PrevAction());
        NEXT = new MPAction("Next", "Next (right/down)", "media-skip-forward.png", new NextAction());
        FULLSCREEN = new MPAction("Fullscreen", "Fullscreen (v)", "icon-fullscreen.png", new FullScreenAction());
        PLAYLIST = new MPAction("Playlist", "Show/hide playlist (L)", "media-eject.png", new PlaylistAction());
        SETTINGS = new MPAction("Settings", "Preferences (ctrl+p)", "icon-preferences.png", new SettingsAction());
        ABOUT = new MPAction("About", "About (ctrl+a)", "logo.png", new AboutAction());

        OPEN = new MPAction("Open", "Open (o)", "icon-open.png", new OpenAction());
        SAVE = new MPAction("Save", "Save (s)", "icon-save.png", new SaveAction());
        SHUFFLE = new MPAction("Shuffle", "Shuffle", "media-playlist-shuffle.png", new ShuffleAction());
        REPEAT = new MPAction("Repeat", "Repeat", "media-playlist-repeat.png", new RepeatAction());
        REMOVE_ONE = new MPAction("Remove selected", "Remove selected", "icon-remove-single.png", new RemoveOneAction());
        REMOVE_ALL = new MPAction("Clear", "Clear playlist", "icon-remove-all.png", new RemoveAllAction());
        ADD = new MPAction("Add", "Add to playlist", "icon-add.png", new AddAction());
    }

    public static MPAction[] getPlayerActions() {
        return new MPAction[]{
                PLAY,
                PAUSE,
                STOP,
                PREV,
                NEXT,
                PLAYLIST,
                FULLSCREEN,
                SETTINGS,
                ABOUT
        };
    }

    public static MPAction[] getPlaylistActions() {
        return new MPAction[]{
                OPEN,
                SAVE,
                ADD,
                REMOVE_ONE,
                REMOVE_ALL,
                SHUFFLE,
                REPEAT
        };
    }

    public static JButton buildButton(Actions.MPAction action) {
        int btnSize = AppConfig.getInstance().getButtonSize().getButtonSize();
        int iconSize = btnSize - 2; // small margin for icon to fit within button
        JButton button = new JButton(action.action);
        button.setText("");
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setPreferredSize(new Dimension(btnSize, btnSize));
        BufferedImage iconImage = MainWindow.loadIconResource(action.iconResource, iconSize, iconSize);
        ImageIcon icon = new ImageIcon(iconImage, action.description);
        button.setIcon(icon);
        button.setToolTipText(action.description);

        return button;
    }

    public static class MPAction {
        public final String iconResource;
        public final String name;
        public final String description;
        public final AbstractAction action;

        MPAction(String name, String description, String iconResource, AbstractAction action) {
            this.iconResource = "/ca/corbett/musicplayer/images/" + iconResource;
            this.name = name;
            this.description = description;
            this.action = action;
            this.action.putValue(Action.NAME, name);
            this.action.putValue(Action.SHORT_DESCRIPTION, description);
        }
    }
}
