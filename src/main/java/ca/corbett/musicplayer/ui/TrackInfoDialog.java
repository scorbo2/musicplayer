package ca.corbett.musicplayer.ui;

import ca.corbett.extras.properties.PropertiesDialog;
import ca.corbett.forms.Alignment;
import ca.corbett.forms.FormPanel;
import ca.corbett.forms.fields.LabelField;

import javax.swing.JFrame;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.File;

public class TrackInfoDialog extends JFrame {

    private final File trackFile;

    public TrackInfoDialog(File trackFile) {
        super("Track info");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(new Dimension(375, 140));
        setResizable(false);
        setLocationRelativeTo(MainWindow.getInstance());
        this.trackFile = trackFile;

        setLayout(new BorderLayout());
        FormPanel formPanel = new FormPanel(Alignment.CENTER);
        formPanel.add(new LabelField("File name:", trimString(trackFile.getName(), 199)));
        formPanel.add(new LabelField("File size:", getSizeString(trackFile.length())));
        formPanel.add(
                new LabelField("Location:", trimString(trackFile.getParentFile().getAbsolutePath(), 199)));
        add(PropertiesDialog.buildScrollPane(formPanel), BorderLayout.CENTER);
    }

    protected static String trimString(String input, int lengthLimit) {
        if (input.length() > lengthLimit) {
            return input.substring(0, lengthLimit) + "...";
        }
        return input;
    }

    protected static String getSizeString(long bytes) {
        if (bytes > (1024 * 1024 * 1024)) {
            float size = (float)bytes / (1024 * 1024 * 1024);
            return String.format("%.2f GB", size);
        }

        if (bytes > (1024 * 1024)) {
            float size = (float)bytes / (1024 * 1024);
            return String.format("%.2f MB", size);
        }

        if (bytes > 1024) {
            float size = (float)bytes / 1024f;
            return String.format("%.2f KB", size);
        }

        return bytes + " bytes";
    }
}
