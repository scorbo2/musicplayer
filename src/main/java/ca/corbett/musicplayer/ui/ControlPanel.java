package ca.corbett.musicplayer.ui;

import ca.corbett.extras.MessageUtil;
import ca.corbett.musicplayer.Actions;
import ca.corbett.musicplayer.AppConfig;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.logging.Logger;

public class ControlPanel extends JPanel {

    private final static Logger logger = Logger.getLogger(ControlPanel.class.getName());

    private static ControlPanel instance;
    private MessageUtil messageUtil;

    private ControlPanel() {
        rebuildControls();
    }

    public static ControlPanel getInstance() {
        if (instance == null) {
            instance = new ControlPanel();
        }

        return instance;
    }

    public void rebuildControls() {
        removeAll();
        setBackground(Color.GRAY);
        setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();

        // This is holdover code from AudioWaveformPanel, on which this is based.
        AppConfig.ControlAlignment controlAlignment = AppConfig.getInstance().getControlAlignment();
        boolean biasStart = controlAlignment == AppConfig.ControlAlignment.LEFT;
        boolean biasCenter = controlAlignment == AppConfig.ControlAlignment.CENTER;
        boolean biasEnd = controlAlignment == AppConfig.ControlAlignment.RIGHT;

        JLabel spacer = new JLabel("");
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.fill = GridBagConstraints.VERTICAL;
        constraints.weightx = biasStart ? 0 : (biasCenter ? 0.5 : 1.0);
        constraints.weighty = 0;
        add(spacer, constraints);

        constraints.gridx++;
        constraints.insets = new Insets(2, 2, 2, 2);
        constraints.weightx = 0;
        constraints.weighty = 0;

        for (Actions.MPAction action : Actions.getPlayerActions()) {
            add(Actions.buildButton(action), constraints);
            constraints.gridx++;
        }

        spacer = new JLabel("");
        constraints.gridx++;
        constraints.fill = GridBagConstraints.VERTICAL;
        constraints.weightx = biasEnd ? 0 : (biasCenter ? 0.5 : 1.0);
        constraints.weighty = 0;
        constraints.insets = new Insets(0, 0, 0, 0);
        add(spacer, constraints);

        MainWindow.rejigger(this);
    }

    private MessageUtil getMessageUtil() {
        if (messageUtil == null) {
            messageUtil = new MessageUtil(MainWindow.getInstance(), logger);
        }

        return messageUtil;
    }
}
