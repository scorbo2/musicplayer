package ca.corbett.musicplayer.ui;

import ca.corbett.extras.MessageUtil;
import ca.corbett.forms.Alignment;
import ca.corbett.forms.FormPanel;
import ca.corbett.forms.fields.LabelField;
import ca.corbett.forms.fields.NumberField;
import ca.corbett.forms.fields.ShortTextField;
import ca.corbett.musicplayer.audio.AudioMetadata;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Provides an editable view of an AudioMetadata instance.
 * If the given source file is an mp3 file, then changes in this dialog
 * can be written back to the source file's id3 tags.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class TrackEditDialog extends TrackInfoDialog {

    private static final Logger log = Logger.getLogger(TrackEditDialog.class.getName());

    private MessageUtil messageUtil;
    private final FormPanel formPanel;
    private final LabelField fileNameField;
    private final ShortTextField titleField;
    private final ShortTextField authorField;
    private final ShortTextField albumField;
    private final ShortTextField genreField;
    private final NumberField trackNumberField;

    public TrackEditDialog(AudioMetadata metadata) {
        super(metadata);
        setSize(new Dimension(420, 320));
        setMinimumSize(new Dimension(420, 320));
        setResizable(true);
        setLocationRelativeTo(MainWindow.getInstance());

        setLayout(new BorderLayout());
        formPanel = new FormPanel(Alignment.CENTER);
        formPanel.setBorderMargin(8);
        fileNameField = new LabelField("File name:", metadata.getSourceFile() == null ? "(no file specified)" : metadata
            .getSourceFile().getName());
        titleField = new ShortTextField("Title:", 20);
        titleField.setAllowBlank(false); // technically, you could have a blank title, but it just seems weird to me
        titleField.setText(metadata.getTitle());
        authorField = new ShortTextField("Artist:", 20);
        authorField.setAllowBlank(true);
        authorField.setText(metadata.getAuthor());
        albumField = new ShortTextField("Album:", 20);
        albumField.setAllowBlank(true);
        albumField.setText(metadata.getAlbum());
        genreField = new ShortTextField("Genre:", 20);
        genreField.setAllowBlank(true);
        genreField.setText(metadata.getGenre());
        trackNumberField = new NumberField("Track number:", metadata.getTrackNumber(), 0, Integer.MAX_VALUE, 1);

        // Disable all if this is not an mp3 file:
        if (!metadata.isMp3()) {
            titleField.setEnabled(false);
            authorField.setEnabled(false);
            albumField.setEnabled(false);
            genreField.setEnabled(false);
            trackNumberField.setEnabled(false);
        }

        formPanel.add(fileNameField);
        formPanel.add(titleField);
        formPanel.add(authorField);
        formPanel.add(albumField);
        formPanel.add(genreField);
        formPanel.add(trackNumberField);

        add(formPanel, BorderLayout.CENTER);
        add(buildButtonPanel(), BorderLayout.SOUTH);
    }

    private void buttonHandler(boolean okay) {
        if (okay) {
            if (metadata.isMp3()) {
                if (formPanel.isFormValid()) {
                    metadata.setTitle(titleField.getText());
                    metadata.setAuthor(authorField.getText());
                    metadata.setAlbum(albumField.getText());
                    metadata.setGenre(genreField.getText());
                    metadata.setTrackNumber(trackNumberField.getCurrentValue().intValue());
                    try {
                        // The metadata instance itself will broadcast a change
                        // event so that any UI component that is displaying
                        // this metadata can update itself accordingly.
                        metadata.saveMetadata();
                    }
                    catch (IOException ioe) {
                        getMessageUtil().error("Save error", "Unable to save audio metadata: " + ioe.getMessage(), ioe);
                        return; // keep dialog open so user can try again or cancel
                    }
                }
                else {
                    // Validation errors are already showing in the form.
                    // We can just return here to keep the dialog open until the form
                    // is valid or until the user cancels.
                    return;
                }
            }
        }
        dispose();
    }

    private JPanel buildButtonPanel() {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        JButton button = new JButton("OK");
        button.addActionListener(e -> buttonHandler(true));
        button.setPreferredSize(new Dimension(100, 24));
        buttonPanel.add(button);
        button = new JButton("Cancel");
        button.addActionListener(e -> buttonHandler(false));
        button.setPreferredSize(new Dimension(100, 24));
        buttonPanel.add(button);
        buttonPanel.setBorder(BorderFactory.createRaisedBevelBorder());
        return buttonPanel;
    }

    private MessageUtil getMessageUtil() {
        if (messageUtil == null) {
            messageUtil = new MessageUtil(this, log);
        }
        return messageUtil;
    }
}
