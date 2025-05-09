package ca.corbett.musicplayer.ui;

import ca.corbett.musicplayer.actions.AboutAction;
import ca.corbett.musicplayer.actions.FullScreenAction;
import ca.corbett.musicplayer.actions.NextAction;
import ca.corbett.musicplayer.actions.PlaylistOpenAction;
import ca.corbett.musicplayer.actions.PlaylistSaveAction;
import ca.corbett.musicplayer.actions.PrevAction;
import ca.corbett.musicplayer.actions.SettingsAction;
import ca.corbett.musicplayer.extensions.MusicPlayerExtensionManager;

import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.awt.event.KeyEvent;

/**
 * Utility class for handling global application keyboard shortcuts.
 *
 * @author scorbo2
 * @since 2024-12-30
 */
public final class KeyboardManager {

    private KeyboardManager() {
    }

    /**
     * Sets up the key listener for the given Window.
     * If the given Window is not active, no action will be taken.
     *
     * @param window the given window must be active in order to receive key events.
     */
    public static void addGlobalKeyListener(final Window window) {
        //Hijack the keyboard manager
        KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        manager.addKeyEventDispatcher(new KeyEventDispatcher() {
            @Override
            public boolean dispatchKeyEvent(KeyEvent e) {

                if (!window.isActive()) {
                    return false;
                }

                if (e.getID() == KeyEvent.KEY_PRESSED) {

                    switch (e.getKeyCode()) {

                        // Left for "previous track":
                        case KeyEvent.VK_LEFT:
                            new PrevAction().actionPerformed(null);
                            break;

                        // Right for "next track":
                        case KeyEvent.VK_RIGHT:
                            new NextAction().actionPerformed(null);
                            break;

                        // Space can mean either "play" or "pause:
                        case KeyEvent.VK_SPACE: {
                            if (AudioPanel.getInstance().getPanelState() == AudioPanel.PanelState.PLAYING ||
                                    AudioPanel.getInstance().getPanelState() == AudioPanel.PanelState.PAUSED) {
                                AudioPanel.getInstance().pause();
                            } else {
                                AudioPanel.getInstance().play();
                            }
                        }
                        break;

                        // V for Vendetta... uh, I mean "Visualization"
                        case KeyEvent.VK_V:
                            new FullScreenAction().actionPerformed(null);
                            break;

                        // ESC stops visualizer if it was running
                        case KeyEvent.VK_ESCAPE:
                            VisualizationWindow.getInstance().stopFullScreen();
                            break;

                        // Ctrl+P for Preferences
                        case KeyEvent.VK_P:
                            if ((e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) > 0) {
                                new SettingsAction().actionPerformed(null);
                            }
                            break;

                        // Ctrl+A for About
                        case KeyEvent.VK_A:
                            if ((e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) > 0) {
                                new AboutAction().actionPerformed(null);
                            }
                            break;

                        // Ctrl+O for Playlist open
                        case KeyEvent.VK_O:
                            if ((e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) > 0) {
                                new PlaylistOpenAction().actionPerformed(null);
                            }
                            break;

                        // Ctrl+S for Playlist save
                        case KeyEvent.VK_S:
                            if ((e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) > 0) {
                                new PlaylistSaveAction().actionPerformed(null);
                            }
                            break;

                        // Undocumented debug helper: press X to dump visualization thread info:
                        case KeyEvent.VK_X:
                            VisualizationWindow.getInstance().debugDump();
                            break;

                        default:
                            break;
                    }

                    // Give extensions a chance to handle this shortcut:
                    MusicPlayerExtensionManager.getInstance().handleKeyEvent(e);
                }

                //Allow the event to be redispatched
                return false;
            }
        });
    }
}
