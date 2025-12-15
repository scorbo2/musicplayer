package ca.corbett.musicplayer.ui;

import ca.corbett.musicplayer.AppConfig;
import ca.corbett.musicplayer.audio.AudioMetadata;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import java.awt.Component;

public class PlaylistCellRenderer extends JLabel implements ListCellRenderer<AudioMetadata> {
    @Override
    public Component getListCellRendererComponent(JList<? extends AudioMetadata> list, AudioMetadata value, int index, boolean isSelected, boolean cellHasFocus) {
        if (isSelected) {
            setBackground(AppConfig.getInstance().getAppTheme().selectedBgColor);
            setForeground(AppConfig.getInstance().getAppTheme().selectedFgColor);
        }
        else {
            setBackground(AppConfig.getInstance().getAppTheme().normalBgColor);
            setForeground(AppConfig.getInstance().getAppTheme().normalFgColor);
        }
        setOpaque(true);

        // Defensive: value might be null in some edge cases
        if (value == null) {
            setText(" (unknown)");
        }
        else {
            setText(" " + value.getFormatted()); // cheesy - adding a space for padding
        }

        return this;
    }
}
