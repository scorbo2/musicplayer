package ca.corbett.musicplayer.ui;

import ca.corbett.extras.properties.PropertiesDialog;
import ca.corbett.forms.Alignment;
import ca.corbett.forms.FormPanel;
import ca.corbett.forms.fields.LabelField;
import ca.corbett.musicplayer.audio.AudioMetadata;

import javax.swing.JFrame;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.File;

/**
 * Displays a dialog showing information about a music track.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class TrackInfoDialog extends JFrame {

    public TrackInfoDialog(File trackFile) {
        super("Track info");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(new Dimension(375, 340));
        setResizable(true);
        setLocationRelativeTo(MainWindow.getInstance());
        AudioMetadata meta = AudioMetadata.fromFile(trackFile);

        setLayout(new BorderLayout());
        FormPanel formPanel = new FormPanel(Alignment.CENTER);
        if (trackFile == null) {
            formPanel.add(new LabelField("File name:", "(no file specified"));
        }
        else {
            formPanel.add(new LabelField("File name:", trimString(trackFile.getName())));
            formPanel.add(new LabelField("File size:", getSizeString(trackFile.length())));
        }
        if (trackFile != null && trackFile.getParentFile() != null) {
            formPanel.add(new LabelField("Location:", trimString(trackFile.getParentFile().getAbsolutePath())));
        }
        formPanel.add(new LabelField("Title:", meta.getTitle()));
        formPanel.add(new LabelField("Artist:", meta.getAuthor()));
        formPanel.add(new LabelField("Album:", meta.getAlbum()));
        formPanel.add(new LabelField("Genre:", meta.getGenre()));
        formPanel.add(new LabelField("Length:", meta.getDurationFormatted()));
        add(PropertiesDialog.buildScrollPane(formPanel), BorderLayout.CENTER);
    }

    protected static String trimString(String input) {
        return trimString(input, 199);
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
