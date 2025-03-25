package ca.corbett.musicplayer.ui;

import ca.corbett.extras.MessageUtil;
import ca.corbett.musicplayer.Actions;
import ca.corbett.musicplayer.AppConfig;
import ca.corbett.musicplayer.audio.AudioData;
import ca.corbett.musicplayer.audio.AudioUtil;

import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.logging.Logger;

public class Playlist extends JPanel {

    private static final Logger logger = Logger.getLogger(Playlist.class.getName());
    private static Playlist instance;
    private MessageUtil messageUtil;
    private final JPanel buttonPanel;
    private final JList<File> fileList;
    private final DefaultListModel<File> fileListModel;

    protected Playlist() {
        buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridBagLayout());
        buttonPanel.setBackground(Color.GRAY);

        fileListModel = new DefaultListModel<>();
        fileList = new JList<>(fileListModel);
        fileList.setCellRenderer(new PlaylistCellRenderer());
        fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        initComponents();
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

    public void clear() {
        fileListModel.clear();
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

    public void rebuildControls() {
        fileList.setBackground(AppConfig.getInstance().getPlaylistTheme().bgColor);

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
            buttonPanel.add(Actions.buildButton(action), constraints);
            constraints.gridx++;
        }

        spacer = new JLabel("");
        constraints.gridx++;
        constraints.fill = GridBagConstraints.VERTICAL;
        constraints.weightx = biasEnd ? 0 : (biasCenter ? 0.5 : 1.0);
        constraints.weighty = 0;
        constraints.insets = new Insets(0, 0, 0, 0);
        buttonPanel.add(spacer, constraints);

        MainWindow.rejigger(buttonPanel);
    }

    protected JComponent buildListPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        JScrollPane scrollPane = new JScrollPane(fileList);
        scrollPane.getVerticalScrollBar().setBlockIncrement(32);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JButton buildButton(Actions.MPAction action) {
        int btnSize = AppConfig.getInstance().getButtonSize().getButtonSize();
        int iconSize = btnSize - 2; // small margin for icon to fit within button
        JButton button = new JButton(action.action);
        button.setText("");
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setPreferredSize(new Dimension(btnSize, btnSize));
        BufferedImage iconImage = MainWindow.getInstance().loadIconResource(action.iconResource, iconSize, iconSize);
        ImageIcon icon = new ImageIcon(iconImage, action.description);
        button.setIcon(icon);
        button.setToolTipText(action.description);

        return button;
    }

    private MessageUtil getMessageUtil() {
        if (messageUtil == null) {
            messageUtil = new MessageUtil(MainWindow.getInstance(), logger);
        }

        return messageUtil;
    }
}
