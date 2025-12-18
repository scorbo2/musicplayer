package ca.corbett.musicplayer.ui;

import ca.corbett.extras.MessageUtil;
import ca.corbett.extras.progress.MultiProgressDialog;
import ca.corbett.musicplayer.Actions;
import ca.corbett.musicplayer.AppConfig;
import ca.corbett.musicplayer.actions.PlaylistSortAction;
import ca.corbett.musicplayer.actions.ReloadUIAction;
import ca.corbett.musicplayer.audio.AudioData;
import ca.corbett.musicplayer.audio.AudioMetadata;
import ca.corbett.musicplayer.audio.AudioUtil;
import ca.corbett.musicplayer.audio.PlaylistUtil;

import javax.swing.DefaultListModel;
import javax.swing.DropMode;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.TransferHandler;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Represents a list of zero or more files containing audio data.
 * The user can select a specific file in the list to play, and
 * the media player actions "next" and "previous" will move forward
 * or backwards through the list. Shuffle and Repeat are also
 * available as options. The currently showing list of files can be
 * saved for easy retrieval later.
 * <p>
 *     The formatting of the list items is handled by PlaylistCellRenderer,
 *     and is user-configurable via the playlist item format setting in
 *     the application preferences. The default format is "[artist] - title (01:23)"
 *     but this can be changed in AppConfig.
 * </p>
 *
 * @author scorbo
 * @since 2025-03-23
 */
public class Playlist extends JPanel implements UIReloadable {

    private static final Logger logger = Logger.getLogger(Playlist.class.getName());
    private static Playlist instance;
    private MessageUtil messageUtil;
    private final JPanel buttonPanel;
    private final JList<AudioMetadata> fileList;
    private final DefaultListModel<AudioMetadata> fileListModel;

    public enum SortAttribute {
        Genre("%g"),
        Artist("%a"),
        Album("%b"),
        Title("%t"),
        TrackNumber("%n"),
        FilePath("%F");

        private final String formatKey;

        SortAttribute(String formatKey) {
            this.formatKey = formatKey;
        }

        public String getFormatKey() {
            return formatKey;
        }

        public static SortAttribute fromFormatKey(String key) {
            for (SortAttribute attr : SortAttribute.values()) {
                if (attr.getFormatKey().equalsIgnoreCase(key)) {
                    return attr;
                }
            }
            return null;
        }
    }

    protected Playlist() {
        buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridBagLayout());

        fileListModel = new DefaultListModel<>();
        fileList = new JList<>(fileListModel);
        fileList.setCellRenderer(new PlaylistCellRenderer());
        fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        fileList.addMouseListener(new DoubleClickListener());

        // Enable drag and drop for reordering
        fileList.setDragEnabled(true);
        fileList.setDropMode(DropMode.INSERT);
        fileList.setTransferHandler(new PlaylistTransferHandler());

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
        addItem(AudioMetadata.fromFile(file));
    }

    /**
     * Adds a single item to the list. Uniqueness checks are not done here,
     * so it's possible to add the same file multiple times if you want.
     * Audio data is not loaded at this point! It's possible to load
     * an invalid (corrupt, wrong format, etc) file here without
     * realizing it until you try to actually play it.
     */
    public void addItem(AudioMetadata meta) {
        fileListModel.addElement(meta);
        revalidate();
        repaint();
    }

    /**
     * Inserts a single item at the given index in the list.
     * Uniqueness checks are not done here, so it's possible to add
     * the same file multiple times if you want.
     * Audio data is not loaded at this point! It's possible to load
     * an invalid (corrupt, wrong format, etc) file here without
     * realizing it until you try to actually play it.
     */
    public void insertItemAt(File file, int index) {
        insertItemAt(AudioMetadata.fromFile(file), index);
    }

    /**
     * Inserts a single item at the given index in the list.
     * Uniqueness checks are not done here, so it's possible to add
     * the same file multiple times if you want.
     * Audio data is not loaded at this point! It's possible to load
     * an invalid (corrupt, wrong format, etc) file here without
     * realizing it until you try to actually play it.
     */
    public void insertItemAt(AudioMetadata meta, int index) {
        fileListModel.add(index, meta);
        revalidate();
        repaint();
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
            AudioMetadata selectedMeta = fileList.getSelectedValue();
            File selected = (selectedMeta != null) ? selectedMeta.getSourceFile() : null;
            AudioData currentlyLoaded = AudioPanel.getInstance().getAudioData();
            if (currentlyLoaded != null && Objects.equals(currentlyLoaded.getSourceFile(), selected)) {
                AudioPanel.getInstance().stop();
                AudioPanel.getInstance().setAudioData(null);
            }

            fileListModel.removeElementAt(fileList.getSelectedIndex());

            revalidate();
            repaint();
        }
    }

    /**
     * Reports the number of items currently in the playlist.
     */
    public int getItemCount() {
        return fileListModel.size();
    }

    /**
     * Returns the AudioMetadata object at the given index in the playlist.
     */
    public AudioMetadata getItemAt(int index) {
        if (index < 0 || index >= fileListModel.size()) {
            return null;
        }
        return fileListModel.get(index);
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
        revalidate();
        repaint();

        // Arbitrary decision: stop and unload any loaded track:
        AudioPanel.getInstance().stop();
        AudioPanel.getInstance().setAudioData(null);
    }

    /**
     * Programmatically reverses the sort order of the current playlist.
     */
    public void reverseSort() {
        List<AudioMetadata> metas = new ArrayList<>();
        for (int i = 0; i < fileListModel.size(); i++) {
            metas.add(fileListModel.get(i));
        }
        fileListModel.clear();
        for (int i = metas.size() - 1; i >= 0; i--) {
            fileListModel.addElement(metas.get(i));
        }

        revalidate();
        repaint();
    }

    /**
     * Sorts the current playlist using the given List of SortKeys.
     */
    public void sort(List<SortKey> sortKeys) {
        if (sortKeys == null || sortKeys.isEmpty()) {
            return;
        }
        List<AudioMetadata> metas = new ArrayList<>();
        for (int i = 0; i < fileListModel.size(); i++) {
            metas.add(fileListModel.get(i));
        }
        sortList(metas, sortKeys);

        fileListModel.clear();
        for (AudioMetadata meta : metas) {
            fileListModel.addElement(meta);
        }

        revalidate();
        repaint();
    }

    /**
     * Returns the File object associated with the currently selected track, if there
     * is a track selected, otherwise null.
     *
     * @return A File object, or null.
     */
    public File getSelectedTrackFile() {
        if (fileList.getSelectedIndex() != -1) {
            AudioMetadata meta = fileList.getSelectedValue();
            return (meta != null) ? meta.getSourceFile() : null;
        }
        return null;
    }

    /**
     * Ignoring whatever selection is set in the list, let's search for the index of whatever
     * track is currently playing, and return its index. Will return -1 if there is no track
     * playing, or if our list does not contain the currently playing track (which is entirely
     * possible, if the user removed it).
     */
    public int getIndexOfCurrentlyPlayingTrack() {
        AudioData audioData = AudioPanel.getInstance().getAudioData();
        if (audioData == null || audioData.getMetadata() == null || audioData.getMetadata().getSourceFile() == null) {
            return -1;
        }
        String sourcePath = audioData.getMetadata().getSourceFile().getAbsolutePath();
        for (int i = 0; i < fileListModel.size(); i++) {
            AudioMetadata meta = fileListModel.get(i);
            if (meta != null
                && meta.getSourceFile() != null
                && sourcePath.equals(meta.getSourceFile().getAbsolutePath())) {
                return i;
            }
        }
        return -1;
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

        // If there's nothing selected in the list, try to find the
        // index of whatever's playing right now (user may have manually
        // unselected it in the list):
        if (index == -1) {
            index = getIndexOfCurrentlyPlayingTrack();
        }

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
        // Convert to metadata objects
        for (File f : newTracks) {
            fileListModel.addElement(AudioMetadata.fromFile(f));
        }
        revalidate();
        repaint();
        AudioPanel.getInstance().next();
    }

    /**
     * Appends the contents of the given playlist to the end of the current playlist.
     */
    public void appendPlaylist(File playlistFile) {
        List<File> loaded = PlaylistUtil.loadPlaylist(playlistFile);
        for (File f : loaded) {
            fileListModel.addElement(AudioMetadata.fromFile(f));
        }
        revalidate();
        repaint();
    }

    /**
     * Inserts the contents of the given playlist at the given index.
     *
     * @return The count of items actually inserted into the list.
     */
    public int insertPlaylistAt(File playlistFile, int index) {
        int count = 0;
        List<File> loaded = PlaylistUtil.loadPlaylist(playlistFile);
        for (File f : loaded) {
            AudioMetadata meta = AudioMetadata.fromFile(f);
            fileListModel.add(index, meta);
            index++;
            count++;
        }
        revalidate();
        repaint();
        return count;
    }

    /**
     * Saves the contents of the current playlist to the target file.
     *
     * @param targetFile a destination save file. Will be overwritten if exists.
     */
    public void savePlaylist(File targetFile) {
        List<File> list = new ArrayList<>();
        for (int i = 0; i < fileListModel.size(); i++) {
            AudioMetadata meta = fileListModel.get(i);
            if (meta != null && meta.getSourceFile() != null) {
                list.add(meta.getSourceFile());
            }
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
        AudioMetadata selectedMeta = fileList.getSelectedValue();
        File selected = (selectedMeta != null) ? selectedMeta.getSourceFile() : null;
        fileList.ensureIndexIsVisible(fileList.getSelectedIndex());
        if (fileListModel.isEmpty() || selected == null) {
            return;
        }

        MultiProgressDialog progress = new MultiProgressDialog(MainWindow.getInstance(), "Loading audio data...");
        progress.setInitialShowDelayMS(AppConfig.getInstance().getLoadProgressBarShowDelayMS());
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
        fileList.invalidate();
        fileList.revalidate();
        fileList.repaint();
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

            // Special case our playlist sort button:
            // We need to add the button as the owner component of the action,
            // so it can show its popup menu correctly.
            if (button.getName().equalsIgnoreCase("playlist sort")) {
                for (ActionListener listener : button.getActionListeners()) {
                    if (listener instanceof PlaylistSortAction) {
                        ((PlaylistSortAction)listener).setOwnerComponent(button);
                    }
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
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Custom TransferHandler for drag-and-drop reordering of playlist items.
     */
    protected static class PlaylistTransferHandler extends TransferHandler {

        private static final DataFlavor AUDIO_METADATA_FLAVOR = new DataFlavor(AudioMetadata.class, "AudioMetadata");

        @Override
        public int getSourceActions(JComponent c) {
            return COPY_OR_MOVE;
        }

        @Override
        protected Transferable createTransferable(JComponent c) {
            @SuppressWarnings("unchecked")
            JList<AudioMetadata> list = (JList<AudioMetadata>)c;
            AudioMetadata selectedValue = list.getSelectedValue();
            if (selectedValue == null) {
                return null;
            }
            return new AudioMetadataTransferable(selectedValue);
        }

        @Override
        protected void exportDone(JComponent c, Transferable data, int action) {
            // Clean up is handled in importData
        }

        @Override
        public boolean canImport(TransferSupport support) {
            // Support both file list drags (from file explorer) and internal reordering:
            return support.isDrop() && (
                support.isDataFlavorSupported(DataFlavor.javaFileListFlavor) ||
                    support.isDataFlavorSupported(AUDIO_METADATA_FLAVOR)
            );
        }

        @Override
        public boolean importData(TransferSupport support) {
            if (!canImport(support)) {
                return false;
            }

            JList.DropLocation dropLocation = (JList.DropLocation)support.getDropLocation();
            int dropIndex = dropLocation.getIndex();

            // Handle file list drops (from file explorer):
            // Note: this is also handled at the MainWindow level, but the difference
            // is that here, the user can choose where to insert the files into the playlist.
            // At the MainWindow level, files are always appended to the end of the playlist.
            if (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                try {
                    @SuppressWarnings("unchecked")
                    List<File> files = (List<File>)support.getTransferable()
                                                          .getTransferData(DataFlavor.javaFileListFlavor);

                    for (File file : files) {
                        if (AudioUtil.isValidAudioFile(file)) {
                            Playlist.getInstance().insertItemAt(file, dropIndex);
                            dropIndex++; // Increment drop index for next insert
                        }
                        else if (AudioUtil.isValidPlaylist(file)) {
                            int countInserted = Playlist.getInstance().insertPlaylistAt(file, dropIndex);
                            dropIndex += countInserted; // Increment drop index for next insert
                        }
                    }

                    return true;
                }
                catch (UnsupportedFlavorException | IOException e) {
                    logger.warning("Failed to import file list during drag and drop: " + e.getMessage());
                    return false;
                }
            }

            // Handle internal playlist reordering:
            try {
                AudioMetadata metadata = (AudioMetadata)support.getTransferable()
                                                               .getTransferData(AUDIO_METADATA_FLAVOR);

                @SuppressWarnings("unchecked")
                JList<AudioMetadata> list = (JList<AudioMetadata>)support.getComponent();
                DefaultListModel<AudioMetadata> model = (DefaultListModel<AudioMetadata>)list.getModel();

                // Find the current index of the item being dragged
                int sourceIndex = -1;
                for (int i = 0; i < model.getSize(); i++) {
                    if (model.getElementAt(i) == metadata) {
                        sourceIndex = i;
                        break;
                    }
                }

                if (sourceIndex == -1) {
                    return false;
                }

                // Remove from old position
                model.remove(sourceIndex);

                // Adjust drop index if necessary (if we removed an item before the drop location)
                if (sourceIndex < dropIndex) {
                    dropIndex--;
                }

                // Insert at new position
                model.add(dropIndex, metadata);

                // Select the moved item
                list.setSelectedIndex(dropIndex);
                list.ensureIndexIsVisible(dropIndex);

                return true;
            }
            catch (UnsupportedFlavorException | IOException e) {
                logger.warning("Failed to import data during drag and drop: " + e.getMessage());
                return false;
            }
        }

        /**
         * Wrapper class to make AudioMetadata transferable for drag-and-drop operations.
         */
        private static class AudioMetadataTransferable implements Transferable {
            private final AudioMetadata metadata;

            public AudioMetadataTransferable(AudioMetadata metadata) {
                this.metadata = metadata;
            }

            @Override
            public DataFlavor[] getTransferDataFlavors() {
                return new DataFlavor[]{AUDIO_METADATA_FLAVOR};
            }

            @Override
            public boolean isDataFlavorSupported(DataFlavor flavor) {
                return AUDIO_METADATA_FLAVOR.equals(flavor);
            }

            @Override
            public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
                if (!isDataFlavorSupported(flavor)) {
                    throw new UnsupportedFlavorException(flavor);
                }
                return metadata;
            }
        }
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

    protected void sortList(List<AudioMetadata> toSort, List<SortKey> sortKeys) {
        if (toSort == null || sortKeys == null || toSort.isEmpty() || sortKeys.isEmpty()) {
            return;
        }
        toSort.sort((a, b) -> {
            for (SortKey sortKey : sortKeys) {
                int comparison = switch (sortKey.attribute) {
                    case Genre -> compareStrings(a.getGenre(), b.getGenre());
                    case Artist -> compareStrings(a.getAuthor(), b.getAuthor());
                    case Album -> compareStrings(a.getAlbum(), b.getAlbum());
                    case Title -> compareStrings(a.getTitle(), b.getTitle());
                    case TrackNumber -> Integer.compare(a.getTrackNumber(), b.getTrackNumber());
                    case FilePath -> compareFilePaths(a.getSourceFile(), b.getSourceFile());
                };
                if (comparison != 0) {
                    return sortKey.isAscending ? comparison : -comparison;
                }
            }
            return 0;
        });
    }

    private static int compareStrings(String a, String b) {
        if (a == null && b == null) { return 0; }
        if (a == null) {
            return -1;  // nulls sort first
        }
        if (b == null) { return 1; }
        return a.compareToIgnoreCase(b);
    }

    private static int compareFilePaths(File a, File b) {
        if (a == null && b == null) { return 0; }
        if (a == null) {
            return -1;  // nulls sort first
        }
        if (b == null) { return 1; }
        return a.getAbsolutePath().compareToIgnoreCase(b.getAbsolutePath());
    }

    public static class SortKey {
        public final SortAttribute attribute;
        public final boolean isAscending;

        public SortKey(SortAttribute attribute, boolean isAscending) {
            this.attribute = attribute;
            this.isAscending = isAscending;
        }

        public static SortKey asc(SortAttribute attribute) {
            return new SortKey(attribute, true);
        }

        public static SortKey desc(SortAttribute attribute) {
            return new SortKey(attribute, false);
        }
    }

    private MessageUtil getMessageUtil() {
        if (messageUtil == null) {
            messageUtil = new MessageUtil(MainWindow.getInstance(), logger);
        }

        return messageUtil;
    }
}
