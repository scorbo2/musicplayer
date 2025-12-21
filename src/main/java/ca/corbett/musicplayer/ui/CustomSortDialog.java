package ca.corbett.musicplayer.ui;

import ca.corbett.extras.properties.PropertiesDialog;
import ca.corbett.forms.FormPanel;
import ca.corbett.forms.fields.CheckBoxField;
import ca.corbett.forms.fields.ComboField;
import ca.corbett.forms.fields.PanelField;
import ca.corbett.musicplayer.AppConfig;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.Arrays;
import java.util.List;

/**
 * Dialog for defining a custom sort order for the playlist.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since MusicPlayer 3.1
 */
public class CustomSortDialog extends JDialog {

    private ComboField<Playlist.SortAttribute> sortKeyField1;
    private ComboField<Playlist.SortAttribute> sortKeyField2;
    private ComboField<Playlist.SortAttribute> sortKeyField3;
    private ComboField<Playlist.SortAttribute> sortKeyField4;
    private ComboField<Playlist.SortAttribute> sortKeyField5;
    private JButton btnAddLevel;
    private JButton btnRemoveLevel;
    private ComboField<String> sortOrderField;
    private CheckBoxField updatePlaylistFormat;

    public CustomSortDialog() {
        setTitle("Custom Sort");
        setSize(380, 360);
        setLocationRelativeTo(MainWindow.getInstance());
        setMinimumSize(new Dimension(380, 200));
        setModal(true);
        setLayout(new BorderLayout());
        add(PropertiesDialog.buildScrollPane(buildFormPanel()), BorderLayout.CENTER);
        add(buildButtonPanel(), BorderLayout.SOUTH);
        restorePreviousSettings();
    }

    private FormPanel buildFormPanel() {
        FormPanel formPanel = new FormPanel();
        formPanel.setBorderMargin(16);

        sortKeyField1 = new ComboField<>("Sort by:", Arrays.asList(Playlist.SortAttribute.values()), 0);
        sortKeyField2 = new ComboField<>("Then by:", Arrays.asList(Playlist.SortAttribute.values()), 0);
        sortKeyField3 = new ComboField<>("Then by:", Arrays.asList(Playlist.SortAttribute.values()), 0);
        sortKeyField4 = new ComboField<>("Then by:", Arrays.asList(Playlist.SortAttribute.values()), 0);
        sortKeyField5 = new ComboField<>("Then by:", Arrays.asList(Playlist.SortAttribute.values()), 0);
        sortKeyField2.setVisible(false);
        sortKeyField3.setVisible(false);
        sortKeyField4.setVisible(false);
        sortKeyField5.setVisible(false);
        formPanel.add(sortKeyField1);
        formPanel.add(sortKeyField2);
        formPanel.add(sortKeyField3);
        formPanel.add(sortKeyField4);
        formPanel.add(sortKeyField5);

        PanelField panelField = new PanelField(new FlowLayout(FlowLayout.LEFT));
        btnAddLevel = new JButton("Add sort key");
        btnAddLevel.addActionListener(e -> addSortLevel());
        panelField.getPanel().add(btnAddLevel);
        btnRemoveLevel = new JButton("Remove sort key");
        btnRemoveLevel.addActionListener(e -> removeSortLevel());
        btnRemoveLevel.setVisible(false);
        panelField.getPanel().add(btnRemoveLevel);
        formPanel.add(panelField);

        sortOrderField = new ComboField<>("Sort order:", List.of("Ascending", "Descending"), 0);
        formPanel.add(sortOrderField);

        updatePlaylistFormat = new CheckBoxField("Update playlist format to match sort order", true);
        formPanel.add(updatePlaylistFormat);

        return formPanel;
    }

    private void addSortLevel() {
        btnRemoveLevel.setVisible(true);
        if (!sortKeyField2.isVisible()) {
            sortKeyField2.setVisible(true);
        } else if (!sortKeyField3.isVisible()) {
            sortKeyField3.setVisible(true);
        } else if (!sortKeyField4.isVisible()) {
            sortKeyField4.setVisible(true);
        } else if (!sortKeyField5.isVisible()) {
            sortKeyField5.setVisible(true);
            btnAddLevel.setVisible(false);
        }
    }

    private void removeSortLevel() {
        btnAddLevel.setVisible(true);
        if (sortKeyField5.isVisible()) {
            sortKeyField5.setVisible(false);
        } else if (sortKeyField4.isVisible()) {
            sortKeyField4.setVisible(false);
        } else if (sortKeyField3.isVisible()) {
            sortKeyField3.setVisible(false);
        } else if (sortKeyField2.isVisible()) {
            sortKeyField2.setVisible(false);
            btnRemoveLevel.setVisible(false);
        }
    }

    /**
     * If the dialog was opened previously and settings were saved,
     * restore those settings in the dialog fields.
     */
    private void restorePreviousSettings() {
        String formatString = AppConfig.getInstance().getPlaylistCustomSortString();
        if (formatString == null || formatString.isBlank()) {
            return;
        }

        String[] keys = formatString.split(",");
        for (int i = 0; i < keys.length && i < 5; i++) {
            Playlist.SortAttribute attribute = Playlist.SortAttribute.fromFormatKey(keys[i].trim());
            if (attribute != null) {
                switch (i) {
                    case 0 -> sortKeyField1.setSelectedItem(attribute);
                    case 1 -> {
                        sortKeyField2.setVisible(true);
                        sortKeyField2.setSelectedItem(attribute);
                        btnRemoveLevel.setVisible(true);
                    }
                    case 2 -> {
                        sortKeyField3.setVisible(true);
                        sortKeyField3.setSelectedItem(attribute);
                    }
                    case 3 -> {
                        sortKeyField4.setVisible(true);
                        sortKeyField4.setSelectedItem(attribute);
                    }
                    case 4 -> {
                        sortKeyField5.setVisible(true);
                        sortKeyField5.setSelectedItem(attribute);
                        btnAddLevel.setVisible(false);
                    }
                }
            }
        }
    }

    /**
     * Apply the selected sort keys to the playlist and close the dialog.
     * Also saves the custom sort string to the application configuration,
     * so the next time the dialog is opened, the previous settings are restored.
     */
    private void applyAndClose() {
        final boolean isAscending = sortOrderField.getSelectedIndex() == 0;

        List<Playlist.SortKey> sortKeys = Arrays.stream(new ComboField[]{
            sortKeyField1,
            sortKeyField2,
            sortKeyField3,
            sortKeyField4,
            sortKeyField5
        })
            .filter(ComboField::isVisible)
            .map(field -> isAscending ?
                Playlist.SortKey.asc((Playlist.SortAttribute) field.getSelectedItem()) :
                Playlist.SortKey.desc((Playlist.SortAttribute) field.getSelectedItem()))
            .toList();

        // Update playlist format string if requested:
        if (updatePlaylistFormat.isChecked()) {
            AppConfig.getInstance().setPlaylistFormatString(buildFormatString(sortKeys));
        }

        // Convert List<SortKey> to comma-separated String for storage:
        String sortKeyString = sortKeys.stream()
            .map(key -> key.attribute.getFormatKey())
            .reduce((a, b) -> a + "," + b)
            .orElse("");
        AppConfig.getInstance().setPlaylistCustomSortString(sortKeyString);

        AppConfig.getInstance().save();

        // Sort the playlist:
        Playlist.getInstance().sort(sortKeys);

        dispose();
    }

    /**
     * Builds a playlist format string based on the given sort keys.
     * Note: we unconditionally append duration (%D) at the end.
     */
    private String buildFormatString(List<Playlist.SortKey> sortKeys) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < sortKeys.size(); i++) {
            builder.append(sortKeys.get(i).attribute.getFormatKey());
            if (i < sortKeys.size() - 1) {
                builder.append(" - ");
            }
        }
        builder.append(" (%D)"); // Append duration at the end
        return builder.toString();
    }

    private JPanel buildButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton button = new JButton("OK");
        button.addActionListener(e -> applyAndClose());
        button.setPreferredSize(new Dimension(100,24));
        panel.add(button);

        button = new JButton("Cancel");
        button.addActionListener(e -> dispose());
        button.setPreferredSize(new Dimension(100,24));
        panel.add(button);

        return panel;
    }
}
