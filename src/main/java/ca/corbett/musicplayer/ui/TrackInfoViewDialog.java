package ca.corbett.musicplayer.ui;

import ca.corbett.extras.ScrollUtil;
import ca.corbett.extras.io.FileSystemUtil;
import ca.corbett.forms.Alignment;
import ca.corbett.forms.FormPanel;
import ca.corbett.forms.fields.LabelField;
import ca.corbett.musicplayer.audio.AudioMetadata;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.File;

/**
 * Provides a read-only view of an AudioMetadata instance.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class TrackInfoViewDialog extends TrackInfoDialog {

    public TrackInfoViewDialog(AudioMetadata metadata) {
        super(metadata);
        setSize(new Dimension(375, 340));
        setMinimumSize(new Dimension(300, 250));
        setResizable(true);
        setLocationRelativeTo(MainWindow.getInstance());

        setLayout(new BorderLayout());
        FormPanel formPanel = new FormPanel(Alignment.CENTER);
        File trackFile = metadata.getSourceFile();
        if (trackFile == null) {
            formPanel.add(new LabelField("File name:", "(no file specified"));
        }
        else {
            formPanel.add(new LabelField("File name:", trimString(trackFile.getName())));
            formPanel.add(new LabelField("File size:", FileSystemUtil.getPrintableSize(trackFile.length())));
        }
        if (trackFile != null && trackFile.getParentFile() != null) {
            formPanel.add(new LabelField("Location:", trimString(trackFile.getParentFile().getAbsolutePath())));
        }
        formPanel.add(new LabelField("Title:", metadata.getTitle()));
        formPanel.add(new LabelField("Artist:", metadata.getAuthor()));
        formPanel.add(new LabelField("Album:", metadata.getAlbum()));
        formPanel.add(new LabelField("Genre:", metadata.getGenre()));
        formPanel.add(new LabelField("Track:", Integer.toString(metadata.getTrackNumber())));
        formPanel.add(new LabelField("Length:", metadata.getDurationFormatted()));
        add(ScrollUtil.buildScrollPane(formPanel), BorderLayout.CENTER);
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

}
