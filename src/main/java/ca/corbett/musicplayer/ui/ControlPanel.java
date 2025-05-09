package ca.corbett.musicplayer.ui;

import ca.corbett.extras.MessageUtil;
import ca.corbett.musicplayer.Actions;
import ca.corbett.musicplayer.AppConfig;
import ca.corbett.musicplayer.actions.ReloadUIAction;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.logging.Logger;

public class ControlPanel extends JPanel implements UIReloadable {

    private final static Logger logger = Logger.getLogger(ControlPanel.class.getName());

    private static ControlPanel instance;
    private MessageUtil messageUtil;

    private ControlPanel() {
        rebuildControls();
        ReloadUIAction.getInstance().registerReloadable(this);
    }

    public static ControlPanel getInstance() {
        if (instance == null) {
            instance = new ControlPanel();
        }

        return instance;
    }

    /**
     * Rebuild our controls and repaint everything in the event that extensions have been
     * enabled/disabled or app preferences have changed.
     */
    @Override
    public void reloadUI() {
        rebuildControls();
    }

    public void rebuildControls() {
        removeAll();
        setBackground(AppConfig.getInstance().getAppTheme().dialogBgColor);
        setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();

        // This is holdover code from AudioWaveformPanel, on which this is based.
        ControlAlignment controlAlignment = AppConfig.getInstance().getControlAlignment();
        boolean biasStart = controlAlignment == ControlAlignment.LEFT;
        boolean biasCenter = controlAlignment == ControlAlignment.CENTER;
        boolean biasEnd = controlAlignment == ControlAlignment.RIGHT;

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

        for (Actions.MPAction action : Actions.getMediaPlayerActions()) {
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

    /**
     * Used to control the sizes of buttons on the toolbars.
     * The icons scale up and down as needed based on these
     * arbitrary pre-selected sizes.
     */
    public enum ButtonSize {
        XSMALL(16, "Extra small"),
        SMALL(20, "Small"),
        NORMAL(24, "Normal"),
        LARGE(30, "Large"),
        XLARGE(36, "Huge");

        final private int buttonSize;
        final private String label;

        ButtonSize(int btnSize, String label) {
            buttonSize = btnSize;
            this.label = label;
        }

        public int getButtonSize() {
            return buttonSize;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    /**
     * Controls the positioning of buttons within the toolbars.
     */
    public enum ControlAlignment {
        LEFT("Left"),
        CENTER("Center"),
        RIGHT("Right");

        private final String label;

        ControlAlignment(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
