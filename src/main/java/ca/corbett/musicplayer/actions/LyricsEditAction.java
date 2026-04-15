package ca.corbett.musicplayer.actions;

import ca.corbett.extras.EnhancedAction;
import ca.corbett.extras.MessageUtil;
import ca.corbett.extras.PopupTextDialog;
import ca.corbett.musicplayer.audio.AudioMetadata;
import ca.corbett.musicplayer.extensions.MusicPlayerExtensionManager;
import ca.corbett.musicplayer.ui.LyricsEditDialog;
import ca.corbett.musicplayer.ui.MainWindow;
import ca.corbett.musicplayer.ui.Playlist;

import javax.swing.JOptionPane;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * An action to bring up the lyrics editor for the selected track in the playlist.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since MusicPlayer 4.0
 */
public class LyricsEditAction extends EnhancedAction {

    private static final Logger log = Logger.getLogger(LyricsEditAction.class.getName());
    private MessageUtil messageUtil;

    public LyricsEditAction() {
        super("Lyrics editor");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        AudioMetadata selectedTrack = Playlist.getInstance().getSelectedTrackMetadata();
        if (selectedTrack == null) {
            JOptionPane.showMessageDialog(MainWindow.getInstance(), "Nothing selected.");
            return;
        }
        if (!selectedTrack.isMp3()) {
            JOptionPane.showMessageDialog(MainWindow.getInstance(), "Lyrics editing is only supported for mp3 files.");
            return;
        }

        // See if any registered extensions can do this work for us:
        LyricsEditDialog dialog = MusicPlayerExtensionManager.getInstance().getLyricsEditDialog(selectedTrack);

        // If not, we'll do it ourselves:
        if (dialog == null) {
            // Our extremely basic built-in editor can just make use of the PopupTextDialog
            // from our swing-extras library. We could provide a basic implementation
            // of LyricsEditDialog, but it would end up looking pretty much exactly like this, but with more code:
            PopupTextDialog lyricsDialog = new PopupTextDialog(MainWindow.getInstance(),
                                                               "Lyrics editor - " + selectedTrack.getTitle(),
                                                               selectedTrack.getLyrics(),
                                                               true);
            lyricsDialog.getTextArea().setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
            lyricsDialog.setClipboardEnabled(true);
            lyricsDialog.setSize(new Dimension(700, 400));
            lyricsDialog.setVisible(true);
            if (lyricsDialog.wasOkayed()) {
                String oldLyrics = selectedTrack.getLyrics();
                selectedTrack.setLyrics(lyricsDialog.getText());
                try {
                    selectedTrack.saveMetadata();
                }
                catch (IOException ex) {
                    log.severe("Error saving lyrics: " + ex.getMessage());
                    getMessageUtil().error("Save error", "Error saving lyrics: " + ex.getMessage(), ex);
                    selectedTrack.setLyrics(oldLyrics); // restore old lyrics if save failed
                }
            }
            return;
        }

        dialog.setVisible(true);
    }

    private MessageUtil getMessageUtil() {
        if (messageUtil == null) {
            messageUtil = new MessageUtil(MainWindow.getInstance(), log);
        }
        return messageUtil;
    }
}
