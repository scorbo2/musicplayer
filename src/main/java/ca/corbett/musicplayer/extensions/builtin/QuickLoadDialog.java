package ca.corbett.musicplayer.extensions.builtin;

import ca.corbett.extras.io.FileSystemUtil;
import ca.corbett.musicplayer.ui.MainWindow;
import ca.corbett.musicplayer.ui.Playlist;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.border.BevelBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;

/**
 * A keyboard-friendly alternative to the standard java file open dialog,
 * specifically for choosing a playlist to open. Playlists are stored
 * in the configured quicksave directory. A boring directory browser
 * is provided if your playlist is located elsewhere.
 *
 * @author scorbo2
 * @since 2019-11-08
 */
public class QuickLoadDialog extends JDialog {

    private static QuickLoadDialog instance;
    private JFileChooser fileChooser;
    private QuickLoadListModel playlistListModel;
    private JList playlistList;

    public QuickLoadDialog() {
        super(MainWindow.getInstance(), true);
        setTitle("Quickload Playlist");
        setSize(300, 500);
        setMinimumSize(new Dimension(300, 400));
        setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
        fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setMultiSelectionEnabled(false);
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.setFileFilter(new FileNameExtensionFilter("MusicPlayer playlists (*.mplist)", "mplist"));
        fileChooser.setCurrentDirectory(QuickLoadExtension.getQuickDir());
        playlistListModel = new QuickLoadListModel();
        initComponents();
    }

    /**
     * Singleton accessor.
     *
     * @return The single instance of this dialog.
     */
    public static QuickLoadDialog getInstance() {
        if (instance == null) {
            instance = new QuickLoadDialog();
        }
        instance.setLocationRelativeTo(MainWindow.getInstance());
        return instance;
    }

    private void openSelectedPlaylist() {
        if (playlistList.getSelectedIndex() != -1) {
            Playlist.getInstance().loadPlaylists(List.of(playlistListModel.getElementAt(playlistList.getSelectedIndex())));
        }
        setVisible(false);
    }

    /**
     * Overridden here so we can reload the list each time the dialog is shown.
     *
     * @param visible whether to show or hide the dialog.
     */
    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            final JDialog thisFrame = this;
            playlistListModel.clear();
            playlistList.requestFocus();
            File quickDir = QuickLoadExtension.getQuickDir();
            if (!quickDir.exists() || !quickDir.isDirectory() || !quickDir.canRead()) {
                JOptionPane.showMessageDialog(thisFrame, "Playlist quick load dir does not exist or is not readable.\nCheck application settings.",
                        "Can't read playlist", JOptionPane.ERROR_MESSAGE);
            }
            List<File> list = FileSystemUtil.findFiles(QuickLoadExtension.getQuickDir(), false, "mplist");
            list.sort((one, two) -> one.getName().toLowerCase().compareTo(two.getName().toLowerCase()));
            for (File file : list) {
                playlistListModel.add(file);
            }
            if (!list.isEmpty()) {
                playlistList.setSelectedIndex(0);
            }
        }
        super.setVisible(visible);
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        add(buildPlaylistSelectionPanel(), BorderLayout.CENTER);
        add(buildButtonPanel(), BorderLayout.SOUTH);
    }

    private JPanel buildPlaylistSelectionPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        playlistList = new JList(playlistListModel);

        // Add the key listener once:
        playlistList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_ESCAPE:
                        instance.setVisible(false);
                        break;

                    case KeyEvent.VK_ENTER:
                        instance.openSelectedPlaylist();
                        break;
                }
            }

        });

        playlistList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    instance.openSelectedPlaylist();
                }
            }

        });

        playlistList.setCellRenderer(new ListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = new JLabel();
                label.setText(((File) value).getName().replace(".mplist", ""));
                if (isSelected) {
                    label.setBackground(list.getSelectionBackground());
                    label.setForeground(list.getSelectionForeground());
                } else {
                    label.setBackground(list.getBackground());
                    label.setForeground(list.getForeground());
                }
                label.setEnabled(list.isEnabled());
                label.setFont(list.getFont().deriveFont(27f));
                label.setOpaque(true);
                return label;
            }

        });
        playlistList.setFont(playlistList.getFont().deriveFont(18f));
        JScrollPane scrollPane = new JScrollPane(playlistList);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Builds and returns the button panel for the bottom of the form.
     *
     * @return A standard button panel with ok and cancel options.
     */
    private JPanel buildButtonPanel() {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        panel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        JButton otherButton = new JButton("Other...");
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");
        otherButton.setPreferredSize(new Dimension(90, 28));
        okButton.setPreferredSize(new Dimension(90, 28));
        cancelButton.setPreferredSize(new Dimension(90, 28));
        panel.add(otherButton);
        panel.add(okButton);
        panel.add(cancelButton);

        otherButton.addActionListener(e -> {
            if (fileChooser.showOpenDialog(instance) == JFileChooser.APPROVE_OPTION) {
                Playlist.getInstance().loadPlaylists(List.of(fileChooser.getSelectedFile()));
                setVisible(false);
            }
        });
        okButton.addActionListener(e -> openSelectedPlaylist());
        cancelButton.addActionListener(e -> setVisible(false));

        return panel;
    }
}
