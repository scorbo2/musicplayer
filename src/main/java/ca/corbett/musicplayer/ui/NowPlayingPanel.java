package ca.corbett.musicplayer.ui;

import ca.corbett.musicplayer.AppConfig;
import ca.corbett.musicplayer.actions.ReloadUIAction;
import ca.corbett.musicplayer.audio.AudioData;

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

    private final AudioData.Metadata NOTHING_PLAYING;

    private NowPlayingPanel() {
        NOTHING_PLAYING = new AudioData.Metadata("(n/a)", "(n/a)", "(n/a)", 0);
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
        AudioData.Metadata meta = data == null ? NOTHING_PLAYING : data.getMetadata();
        titleValue.setText(meta.title);
        artistValue.setText(meta.author);
        durationValue.setText(meta.durationSeconds <= 0 ? " (n/a) " : " " + formatSeconds(meta.durationSeconds) + " ");
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

    /**
     * Given a value in seconds, return a more human-friendly version of it.
     * For example, 2461 should return "41:01" indicating minutes:hours.
     *
     * @param timeValueSeconds A length value, in seconds.
     * @return A human-friendly display version of that duration.
     */
    public static String formatSeconds(int timeValueSeconds) {

        // special case for small values:
        if (timeValueSeconds < 60) {
            return "00:" + String.format("%02d", timeValueSeconds);
        }

        int hours = 0;
        int minutes = 0;
        int seconds = 0;
        while (timeValueSeconds >= 3600) {
            timeValueSeconds -= 3600;
            hours++;
        }
        while (timeValueSeconds >= 60) {
            timeValueSeconds -= 60;
            minutes++;
        }
        seconds = timeValueSeconds;

        String hoursStr = "";
        String minutesStr = "";
        String secondsStr = "";
        if (hours > 0) {
            hoursStr = hours + ":";
        }
        if (minutes > 0 || hours > 0) {
            minutesStr = String.format("%02d", minutes) + ":";
        }
        if (seconds > 0 || minutes > 0 || hours > 0) {
            secondsStr = String.format("%02d", seconds);
        }

        return hoursStr + minutesStr + secondsStr;
    }
}
