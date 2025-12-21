package ca.corbett.musicplayer.ui;

import ca.corbett.musicplayer.AppConfig;
import ca.corbett.musicplayer.actions.ReloadUIAction;
import ca.corbett.musicplayer.audio.AudioData;
import ca.corbett.musicplayer.audio.AudioMetadata;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Shows the currently loaded song info (title, length).
 *
 * @author scorbo2
 * @since 2025-03-25
 */
public class NowPlayingPanel extends JPanel implements UIReloadable {
    private static NowPlayingPanel instance;

    private final JLabel titleLabel;
    private final JLabel titleValue;
    private final JLabel durationLabel;
    private final JLabel durationValue;
    private final JLabel artistLabel;
    private final JLabel artistValue;

    private NowPlayingPanel() {
        setLayout(new GridBagLayout());
        titleLabel = buildLabel("Title:", Font.BOLD, SwingConstants.RIGHT);
        durationLabel = buildLabel("Duration:", Font.BOLD, SwingConstants.RIGHT);
        artistLabel = buildLabel("Artist:", Font.BOLD, SwingConstants.RIGHT);
        titleValue = buildLabel("", Font.PLAIN, SwingConstants.LEFT);
        durationValue = buildLabel("", Font.PLAIN, SwingConstants.LEFT);
        artistValue = buildLabel("", Font.PLAIN, SwingConstants.LEFT);
        initLayout();
        reloadUI();
        setNowPlaying(null);
        ReloadUIAction.getInstance().registerReloadable(this);
    }

    public static NowPlayingPanel getInstance() {
        if (instance == null) {
            instance = new NowPlayingPanel();
        }

        return instance;
    }

    public void setNowPlaying(AudioData data) {
        AudioMetadata meta = data == null ? AudioMetadata.NOTHING_PLAYING : data.getMetadata();
        titleValue.setText(meta.getTitle());
        artistValue.setText(meta.getAuthor());
        durationValue.setText(" " + meta.getDurationFormatted() + " ");
    }

    @Override
    public void reloadUI() {
        AppTheme.Theme theme = AppConfig.getInstance().getAppTheme();
        setBackground(theme.dialogBgColor);

        titleLabel.setBackground(theme.headerBgColor);
        artistLabel.setBackground(theme.headerBgColor);
        durationLabel.setBackground(theme.headerBgColor);
        titleLabel.setForeground(theme.headerFgColor);
        artistLabel.setForeground(theme.headerFgColor);
        durationLabel.setForeground(theme.headerFgColor);

        titleValue.setBackground(theme.normalBgColor);
        titleValue.setForeground(theme.normalFgColor);
        artistValue.setBackground(theme.normalBgColor);
        artistValue.setForeground(theme.normalFgColor);
        durationValue.setBackground(theme.normalBgColor);
        durationValue.setForeground(theme.normalFgColor);
    }

    private void initLayout() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy = 0;
        gbc.weightx = 0;
        add(artistLabel, gbc);
        gbc.gridy = 1;
        add(titleLabel, gbc);
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        add(artistValue, gbc);
        gbc.gridy = 1;
        gbc.weightx = 0;
        gbc.gridwidth = 3;
        add(titleValue, gbc);
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        add(durationLabel, gbc);
        gbc.gridx = 3;
        add(durationValue, gbc);
    }

    private JLabel buildLabel(String text, int fontStyle, int textAlign) {
        JLabel label = new JLabel(text);
        label.setOpaque(true);
        label.setFont(label.getFont().deriveFont(fontStyle));
        label.setHorizontalTextPosition(textAlign);
        return label;
    }
}
