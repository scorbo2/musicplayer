package ca.corbett.musicplayer.ui;

import ca.corbett.extras.MessageUtil;
import ca.corbett.musicplayer.Actions;
import ca.corbett.musicplayer.AppConfig;
import ca.corbett.musicplayer.actions.ReloadUIAction;
import ca.corbett.musicplayer.audio.AudioData;
import ca.corbett.musicplayer.audio.AudioUtil;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
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
     *
     * @return A populated AudioData instance, or null.
     */
    public AudioData getNext() {
        if (fileListModel.isEmpty()) {
            return null;
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
                    return null; // we're done here.
                }
                index = 0;
            }
        }

        // Select whatever we landed on and return it:
        fileList.setSelectedIndex(index);
        return getSelected();
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
     *
     * @return A populated AudioData instance, or null.
     */
    public AudioData getPrev() {
        if (fileListModel.isEmpty()) {
            return null;
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
                    return null; // we're done here.
                }
                index = fileListModel.size() - 1;
            }
        }

        // Select whatever we landed on and return it:
        fileList.setSelectedIndex(index);
        return getSelected();
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
     * Parses the selected file's audio data and returns it in an AudioData instance.
     * If nothing is selected, you get null. If the audio data cannot be parsed
     * for whatever reason, an error will be raised and you will get null.
     *
     * @return Either a populated AudioData instance, or null.
     */
    public AudioData getSelected() {
        File selected = fileList.getSelectedValue();
        if (fileListModel.isEmpty() || selected == null) {
            return null;
        }

        try {
            return AudioUtil.load(selected);
        } catch (Exception e) {
            getMessageUtil().error("Problem loading file: " + e.getMessage(), e);
        }
        return null;
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
        AppConfig.ControlAlignment controlAlignment = AppConfig.getInstance().getControlAlignment();
        boolean biasStart = controlAlignment == AppConfig.ControlAlignment.LEFT;
        boolean biasCenter = controlAlignment == AppConfig.ControlAlignment.CENTER;
        boolean biasEnd = controlAlignment == AppConfig.ControlAlignment.RIGHT;

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
                    button.setBackground(getDarkerColor(AppConfig.getInstance().getAppTheme().dialogBgColor));
                }
            }
            if (button.getName().equalsIgnoreCase("repeat")) {
                if (AppConfig.getInstance().isRepeatEnabled()) {
                    button.setContentAreaFilled(true);
                    button.setBackground(getDarkerColor(AppConfig.getInstance().getAppTheme().dialogBgColor));
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

    /**
     * Returns a slightly darker shade of whatever color you supply.
     * I'm trying to make toggle buttons look like toggle buttons, but this
     * is a bit wonky. Yeah, I know JToggleButton is a thing but I don't
     * want to have borders on these toolbar buttons.
     * <p>
     *     Update: there's a wonky case where you might have created
     *     a theme with a black background, and of course we can't
     *     make a darker shade of black. So, if your supplied color
     *     is black, or very close to black, this method will instead
     *     return a lighter shade of that color, despite the method name.
     *     The joys of supporting a highly customizable environment!
     * </p>
     *
     * @param input Any color.
     * @return A slightly darker shade of that color.
     */
    private Color getDarkerColor(Color input) {
        int delta = -35;

        // wonky special case: don't try to darken an already dark color:
        if (input.getRed() < 35 && input.getGreen() < 35 && input.getBlue() < 35) {
            delta = 35;
        }

        int red = Math.max(input.getRed() + delta, 0);
        int green = Math.max(input.getGreen() + delta, 0);
        int blue = Math.max(input.getBlue() + delta, 0);
        return new Color(red, green, blue);
    }

    protected JComponent buildListPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        JScrollPane scrollPane = new JScrollPane(fileList);
        scrollPane.getVerticalScrollBar().setBlockIncrement(32);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private MessageUtil getMessageUtil() {
        if (messageUtil == null) {
            messageUtil = new MessageUtil(MainWindow.getInstance(), logger);
        }

        return messageUtil;
    }
}
