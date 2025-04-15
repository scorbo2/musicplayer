package ca.corbett.musicplayer.ui;

import ca.corbett.extras.MessageUtil;
import ca.corbett.extras.progress.MultiProgressDialog;
import ca.corbett.musicplayer.Actions;
import ca.corbett.musicplayer.AppConfig;
import ca.corbett.musicplayer.actions.ReloadUIAction;
import ca.corbett.musicplayer.audio.AudioData;
import ca.corbett.musicplayer.audio.PlaylistUtil;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Represents a list of zero or more files containing audio data.
 * The user can select a specific file in the list to play, and
 * the media player actions "next" and "previous" will move forward
 * or backwards through the list. Shuffle and Repeat are also
 * available as options. The currently showing list of files can be
 * saved for easy retrieval later.
 *
 * @author scorbo
 * @since 2025-03-23
 */
public class Playlist extends JPanel implements UIReloadable {

    private static final Logger logger = Logger.getLogger(Playlist.class.getName());
    private static Playlist instance;
    private MessageUtil messageUtil;
    private final JPanel buttonPanel;
    private final JList<File> fileList;
    private final DefaultListModel<File> fileListModel;

    protected Playlist() {
        buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridBagLayout());

        fileListModel = new DefaultListModel<>();
        fileList = new JList<>(fileListModel);
        fileList.setCellRenderer(new PlaylistCellRenderer());
        fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        fileList.addMouseListener(new DoubleClickListener());

        initComponents();
        ReloadUIAction.getInstance().registerReloadable(this);
    }

    public static Playlist getInstance() {
        if (instance == null) {
            instance = new Playlist();
        }

        return instance;
    }

    /**
     * Adds a single item to the list. Uniqueness checks are not done here,
     * so it's possible to add the same file multiple times if you want.
     * Audio data is not loaded at this point! It's possible to load
     * an invalid (corrupt, wrong format, etc) file here without
     * realizing it until you try to actually play it.
     *
     * @param file Any File.
     */
    public void addItem(File file) {
        fileListModel.addElement(file);
    }

    /**
     * Removes the selected file from the list. The list is single-select,
     * so you can't selectively remove a bunch of files at once.
     * You do however have the clear() option to remove everything.
     * Note that it's possible to remove the file that's currently
     * playing - that's not an error. The file is loaded into the
     * audio panel and will keep playing even if it's no longer in
     * the playlist. As soon as you hit next, the file will unload
     * from the audio panel automatically.
     */
    public void removeSelected() {
        if (fileList.getSelectedIndex() != -1) {

            // Arbitrary decision: if you remove the track that's currently
            // loaded in the audio panel, stop and unload it.
            File selected = fileList.getSelectedValue();
            AudioData currentlyLoaded = AudioPanel.getInstance().getAudioData();
            if (currentlyLoaded != null && currentlyLoaded.getSourceFile().equals(selected)) {
                AudioPanel.getInstance().stop();
                AudioPanel.getInstance().setAudioData(null);
            }

            fileListModel.removeElementAt(fileList.getSelectedIndex());
        }
    }

    /**
     * Remove all files from the list. Note that you can do this even
     * if something is currently playing - that's not an error.
     * Whatever audio was loaded into the audio panel will continue
     * playing until the end of the track, at which point it will
     * be unloaded from the audio panel automatically.
     */
    public void clear() {
        fileListModel.clear();

        // Arbitrary decision: stop and unload any loaded track:
        AudioPanel.getInstance().stop();
        AudioPanel.getInstance().setAudioData(null);
    }

    /**
     * Returns the File object associated with the currently selected track, if there
     * is a track selected, otherwise null.
     *
     * @return A File object, or null.
     */
    public File getSelectedTrackFile() {
        if (fileList.getSelectedIndex() != -1) {
            return fileList.getSelectedValue();
        }
        return null;
    }

    /**
     * Returns the "next" item in the playlist. The word "next" is in
     * quotes because it may not be what you expect. Normally, this will
     * simply be the next item after whatever is currently selected. But,
     * if the "shuffle" option is enabled, you may receive a random item
     * instead. If the current file is the last one in the list and
     * you hit this method, you will either get null, indicating the
     * end of the list, or it will wrap back to the beginning of the
     * list, depending on the value of the "repeat" option.
     * <p>
     * An attempt will be made to actually parse the audio data
     * from the file - if this fails, an error will be raised
     * and you get null.
     * </p>
     */
    public void loadNext() {
        if (fileListModel.isEmpty()) {
            return;
        }

        // Make note of whatever is currently selected:
        int index = fileList.getSelectedIndex();

        // If "shuffle" is enabled, pick something at random:
        if (AppConfig.getInstance().isShuffleEnabled()) {
            index = getRandomSelectionIndex();
        }

        // Otherwise, go sequentially:
        else {
            index++;
            boolean isRepeat = AppConfig.getInstance().isRepeatEnabled();

            // Did we hit the end of the list?
            if (index >= fileListModel.size()) {
                if (!isRepeat) {
                    return; // we're done here.
                }
                index = 0;
            }
        }

        // Select whatever we landed on and return it:
        fileList.setSelectedIndex(index);
        loadSelected();
    }

    /**
     * Returns the "previous" item in the playlist. The word "previous" is in
     * quotes because it may not be what you expect. Normally, this will
     * simply be the item before whatever is currently selected. But,
     * if the "shuffle" option is enabled, you may receive a random item
     * instead. If the current file is the first one in the list and
     * you hit this method, you will either get null, indicating the
     * end of the list, or it will wrap around to the end of the
     * list, depending on the value of the "repeat" option.
     * <p>
     * An attempt will be made to actually parse the audio data
     * from the file - if this fails, an error will be raised
     * and you get null.
     * </p>
     */
    public void loadPrev() {
        if (fileListModel.isEmpty()) {
            return;
        }

        // Make note of whatever is currently selected:
        int index = fileList.getSelectedIndex();

        // If "shuffle" is enabled, pick something at random:
        if (AppConfig.getInstance().isShuffleEnabled()) {
            index = getRandomSelectionIndex();
        }

        // Otherwise, go sequentially:
        else {
            index--;
            boolean isRepeat = AppConfig.getInstance().isRepeatEnabled();

            // Did we hit the start of the list?
            if (index < 0) {
                if (!isRepeat) {
                    return; // we're done here.
                }
                index = fileListModel.size() - 1;
            }
        }

        // Select whatever we landed on and return it:
        fileList.setSelectedIndex(index);
        loadSelected();
    }

    /**
     * Given a list of playlist files, attempt to load all tracks from all of them
     * and set the current content based on them. The current playlist is cleared
     * before loading the new stuff. If an error occurs on load, the current
     * playlist is left intact.
     *
     * @param playlistFiles a list of playlist files in any supported playlist format.
     */
    public void loadPlaylists(List<File> playlistFiles) {
        List<File> newTracks = PlaylistUtil.loadPlaylists(playlistFiles);
        if (newTracks.isEmpty()) {
            return;
        }

        fileListModel.clear();
        fileListModel.addAll(newTracks);
        AudioPanel.getInstance().next();
    }

    /**
     * Saves the contents of the current playlist to the target file.
     *
     * @param targetFile a destination save file. Will be overwritten if exists.
     */
    public void savePlaylist(File targetFile) {
        List<File> list = new ArrayList<>();
        for (int i = 0; i < fileListModel.size(); i++) {
            list.add(fileListModel.get(i));
        }

        // If there's nothing here, don't bother:
        if (list.isEmpty()) {
            return;
        }

        PlaylistUtil.savePlaylist(list, targetFile);
    }

    /**
     * Changes the state of the "shuffle" option between off or on.
     * If it's on, calling getNext() will return a random (or mostly random)
     * item from the list, instead of picking the next one sequentially.
     * If it's off, calling getNext() will give you the next item in the
     * list as you would expect, until you get to the end. Calling getNext()
     * at that point will either give you nothing, or wrap back to the
     * start of the list, depending on the value of the "repeat" option.
     */
    public void toggleShuffle() {
        AppConfig.getInstance().setShuffleEnabled(!AppConfig.getInstance().isShuffleEnabled());
        AppConfig.getInstance().saveAndReloadUI();
    }

    /**
     * Changes the state of the "repeat" option between off or on.
     * If it's on, the playlist will wrap back to the start of the list
     * if you call getNext() at the end of the list.
     * If it's off, calling getNext() at the end of the list will give
     * you nothing.
     */
    public void toggleRepeat() {
        AppConfig.getInstance().setRepeatEnabled(!AppConfig.getInstance().isRepeatEnabled());
        AppConfig.getInstance().saveAndReloadUI();
    }

    /**
     * Reports whether something is currently selected in the list.
     *
     * @return true if the list is non-empty and a file is currently selected.
     */
    public boolean hasSelection() {
        return (!fileListModel.isEmpty()) && fileList.getSelectedValue() != null;
    }

    /**
     * Loads audio data for whatever is currently selected in the playlist.
     * If the playlist is empty or nothing is selected, nothing happens.
     * The load will be done in a worker thread with a progress dialog,
     * so this method returns immediately while the data is still being
     * loaded. Upon completion, the resulting AudioData will be loaded
     * into the AudioPanel by the worker thread. If something goes wrong,
     * an error is logged and displayed to the user and nothing is loaded.
     */
    public void loadSelected() {
        File selected = fileList.getSelectedValue();
        fileList.ensureIndexIsVisible(fileList.getSelectedIndex());
        if (fileListModel.isEmpty() || selected == null) {
            return;
        }

        MultiProgressDialog progress = new MultiProgressDialog(MainWindow.getInstance(), "Loading audio data...");
        progress.runWorker(new AudioLoadThread(selected), true);
    }

    protected void initComponents() {
        setLayout(new BorderLayout());
        rebuildControls();
        add(buttonPanel, BorderLayout.SOUTH);
        add(buildListPanel(), BorderLayout.CENTER);
    }

    /**
     * Rebuild our controls and repaint everything in the event that extensions have been
     * enabled/disabled or app preferences have changed.
     */
    @Override
    public void reloadUI() {
        rebuildControls();
    }

    public void rebuildControls() {
        buttonPanel.setBackground(AppConfig.getInstance().getAppTheme().dialogBgColor);
        fileList.setBackground(AppConfig.getInstance().getAppTheme().normalBgColor);

        buttonPanel.removeAll();
        GridBagConstraints constraints = new GridBagConstraints();

        // This is holdover code from AudioWaveformPanel, on which this is based.
        ControlPanel.ControlAlignment controlAlignment = AppConfig.getInstance().getControlAlignment();
        boolean biasStart = controlAlignment == ControlPanel.ControlAlignment.LEFT;
        boolean biasCenter = controlAlignment == ControlPanel.ControlAlignment.CENTER;
        boolean biasEnd = controlAlignment == ControlPanel.ControlAlignment.RIGHT;

        JLabel spacer = new JLabel("");
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.fill = GridBagConstraints.VERTICAL;
        constraints.weightx = biasStart ? 0 : (biasCenter ? 0.5 : 1.0);
        constraints.weighty = 0;
        buttonPanel.add(spacer, constraints);

        constraints.gridx++;
        constraints.insets = new Insets(2, 2, 2, 2);
        constraints.weightx = 0;
        constraints.weighty = 0;

        for (Actions.MPAction action : Actions.getPlaylistActions()) {
            JButton button = Actions.buildButton(action);
            buttonPanel.add(button, constraints);

            // Special case our toggle buttons:
            if (button.getName().equalsIgnoreCase("shuffle")) {
                if (AppConfig.getInstance().isShuffleEnabled()) {
                    button.setContentAreaFilled(true);
                    button.setBackground(AppTheme.getOffsetColor(AppConfig.getInstance().getAppTheme().dialogBgColor));
                }
            }
            if (button.getName().equalsIgnoreCase("repeat")) {
                if (AppConfig.getInstance().isRepeatEnabled()) {
                    button.setContentAreaFilled(true);
                    button.setBackground(AppTheme.getOffsetColor(AppConfig.getInstance().getAppTheme().dialogBgColor));
                }
            }

            constraints.gridx++;
        }

        spacer = new JLabel("");
        constraints.gridx++;
        constraints.fill = GridBagConstraints.VERTICAL;
        constraints.weightx = biasEnd ? 0 : (biasCenter ? 0.5 : 1.0);
        constraints.weighty = 0;
        constraints.insets = new Insets(0, 0, 0, 0);
        buttonPanel.add(spacer, constraints);

        MainWindow.rejigger(this);
    }

    private int getRandomSelectionIndex() {
        if (fileListModel.isEmpty()) {
            return -1;
        }
        int index = fileList.getSelectedIndex();
        Random rand = new Random(System.currentTimeMillis());
        int newIndex = rand.nextInt(fileListModel.size());

        // Try to avoid returning the same thing that was already selected:
        if (fileListModel.size() > 1) {
            while (newIndex == index) {
                // According to math theory, this might loop infinitely.
                // But in practice, it'll eventually stop.
                newIndex = rand.nextInt(fileListModel.size());
            }
        }
        return newIndex;
    }

    protected JComponent buildListPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        JScrollPane scrollPane = new JScrollPane(fileList);
        scrollPane.getVerticalScrollBar().setBlockIncrement(32);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    protected static class DoubleClickListener extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            // If it's a double click and something is selected, force a load and play:
            // (this is a bit wonky if you double-click whatever's currently playing, but okay):
            if (e.getClickCount() == 2 && Playlist.getInstance().fileList.getSelectedIndex() != -1) {
                AudioPanel.getInstance().setAudioData(null);
                AudioPanel.getInstance().play();
            }
        }
    }

    private MessageUtil getMessageUtil() {
        if (messageUtil == null) {
            messageUtil = new MessageUtil(MainWindow.getInstance(), logger);
        }

        return messageUtil;
    }
}
