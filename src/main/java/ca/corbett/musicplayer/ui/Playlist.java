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
import java.util.logging.Logger;

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

    public void addItem(File file) {
        fileListModel.addElement(file);
    }

    public void removeSelected() {
        if (fileList.getSelectedIndex() != -1) {
            fileListModel.removeElementAt(fileList.getSelectedIndex());
        }
    }

    public void clear() {
        fileListModel.clear();
    }

    public void toggleShuffle() {
        AppConfig.getInstance().setShuffleEnabled(!AppConfig.getInstance().isShuffleEnabled());
        AppConfig.getInstance().save();
    }

    public void toggleRepeat() {
        AppConfig.getInstance().setRepeatEnabled(!AppConfig.getInstance().isRepeatEnabled());
        AppConfig.getInstance().save();
    }

    public AudioData getSelected() {
        if (fileListModel.isEmpty()) {
            return null;
        }

        File selected = fileList.getSelectedValue();
        if (selected == null) {
            selected = fileListModel.get(0); // grab 1st in list if nothing selected
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

    /**
     * Returns a slightly darker shade of whatever color you supply.
     * I'm trying to make toggle buttons look like toggle buttons, but this
     * is a bit wonky. Yeah, I know JToggleButton is a thing but I don't
     * want to have borders on these toolbar buttons.
     *
     * @param input Any color.
     * @return A slightly darker shade of that color.
     */
    private Color getDarkerColor(Color input) {
        int red = Math.max(input.getRed() - 35, 0);
        int green = Math.max(input.getGreen() - 35, 0);
        int blue = Math.max(input.getBlue() - 35, 0);
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
