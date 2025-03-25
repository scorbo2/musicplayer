package ca.corbett.musicplayer.ui;

import ca.corbett.musicplayer.AppConfig;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import java.awt.Component;
import java.io.File;

public class PlaylistCellRenderer extends JLabel implements ListCellRenderer<File> {
    @Override
    public Component getListCellRendererComponent(JList<? extends File> list, File value, int index, boolean isSelected, boolean cellHasFocus) {
        if (isSelected) {
            setBackground(AppConfig.getInstance().getPlaylistTheme().selectedBgColor);
            setForeground(AppConfig.getInstance().getPlaylistTheme().selectedFgColor);
        }
        else {
            setBackground(AppConfig.getInstance().getPlaylistTheme().bgColor);
            setForeground(AppConfig.getInstance().getPlaylistTheme().fgColor);
        }
        setOpaque(true);
        setText(value.getName());

        return this;
    }
}
