package ca.corbett.musicplayer.extensions.builtin;

import ca.corbett.musicplayer.ui.MainWindow;
import ca.corbett.musicplayer.ui.Playlist;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.BevelBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;

/**
 * A simplified playlist save dialog that just asks for a name, and then
 * saves the current playlist into the configurable playlist quicksave directory.
 * A manual "Other..." button is available so there's still a way to
 * pick an arbitrary location.
 *
 * @author scorbo2
 * @since 2019-11-09
 */
public class QuickSaveDialog extends JDialog {

    private static QuickSaveDialog instance;
    private final JFileChooser fileChooser;
    private JTextField nameTextField;

    public QuickSaveDialog() {
        super(MainWindow.getInstance(), false);
        setTitle("Quicksave Playlist");
        setSize(400, 120);
        setResizable(false);
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        fileChooser = new JFileChooser();
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.setFileFilter(new FileNameExtensionFilter("MusicPlayer playlists (*.mplist)", "mplist"));
        initComponents();
    }

    public static QuickSaveDialog getInstance() {
        if (instance == null) {
            instance = new QuickSaveDialog();
        }
        instance.setLocationRelativeTo(MainWindow.getInstance());
        return instance;
    }

    /**
     * Overridden here so we can blank out the name text field when we're shown.
     *
     * @param visible Whether to show the dialog or hide it
     */
    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            nameTextField.setText("");
            nameTextField.requestFocus();
        }
        super.setVisible(visible);
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        add(buildPlaylistSavePanel(), BorderLayout.CENTER);
        add(buildButtonPanel(), BorderLayout.SOUTH);
    }

    private JPanel buildPlaylistSavePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();

        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 2;
        constraints.weighty = 0.5;
        JLabel label = new JLabel("");
        panel.add(label, constraints);

        label = new JLabel("Name:");
        constraints.gridwidth = 1;
        constraints.gridy = 1;
        constraints.weighty = 0;
        panel.add(label, constraints);

        constraints.gridx = 1;
        constraints.anchor = GridBagConstraints.WEST;
        nameTextField = new JTextField(20);
        nameTextField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    save();
                }
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    setVisible(false);
                }
            }

        });
        panel.add(nameTextField, constraints);

        label = new JLabel("");
        constraints.gridy = 10;
        constraints.gridwidth = 2;
        constraints.weighty = 0.5;
        panel.add(label, constraints);

        return panel;
    }

    private void save() {
        final JDialog thisFrame = this;
        File saveDir = QuickLoadExtension.getQuickDir();
        if (!saveDir.exists() || !saveDir.isDirectory() || !saveDir.canWrite()) {
            JOptionPane.showMessageDialog(thisFrame, "Playlist quick save dir does not exist or is not writable.\nCheck application settings.",
                    "Can't write playlist", JOptionPane.ERROR_MESSAGE);
            return;
        }
        File destFile = new File(saveDir, nameTextField.getText() + ".mplist");
        if (destFile.exists()) {
            int result = JOptionPane.showConfirmDialog(thisFrame, "Do you wish to overwrite the existing file?",
                    "Confirm overwrite", JOptionPane.YES_NO_OPTION);
            if (result != JOptionPane.YES_OPTION) {
                return;
            }
        }
        Playlist.getInstance().savePlaylist(destFile);
        setVisible(false);
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

        final JDialog thisFrame = this;
        otherButton.addActionListener(e -> {
            if (fileChooser.showSaveDialog(instance) == JFileChooser.APPROVE_OPTION) {
                File f = fileChooser.getSelectedFile();
                if (!f.getName().toLowerCase().endsWith(".mplist")) {
                    f = new File(f.getAbsolutePath() + ".mplist");
                }
                if (f.exists()) {
                    int result = JOptionPane.showConfirmDialog(thisFrame, "Do you wish to overwrite the existing file?",
                            "Confirm overwrite", JOptionPane.YES_NO_OPTION);
                    if (result != JOptionPane.YES_OPTION) {
                        return;
                    }
                }
                Playlist.getInstance().savePlaylist(f);
                setVisible(false);
            }
        });

        okButton.addActionListener(e -> save());
        cancelButton.addActionListener(e -> setVisible(false));

        return panel;
    }
}
