package ca.corbett.musicplayer.ui;

import ca.corbett.extras.MessageUtil;
import ca.corbett.extras.image.ImageUtil;
import ca.corbett.musicplayer.AppConfig;
import ca.corbett.musicplayer.Version;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainWindow extends JFrame {

    private static final Logger logger = Logger.getLogger(MainWindow.class.getName());

    public static final int DEFAULT_WIDTH = 420;
    public static final int DEFAULT_HEIGHT = 250;
    public static final int DEFAULT_PLAYLIST_HEIGHT = 250;

    private static final int MIN_WIDTH = 420;
    private static final int MIN_HEIGHT = 220;

    private static MainWindow instance;
    private MessageUtil messageUtil;
    private boolean playlistVisible = false;
    final int expandedViewHeightDelta = DEFAULT_PLAYLIST_HEIGHT;
    private final JPanel mediaPanel;
    private final Timer resizeTimer;

    private MainWindow() {
        super(Version.FULL_NAME);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT));
        setMinimumSize(new Dimension(MIN_WIDTH, MIN_HEIGHT));
        setLayout(new BorderLayout());
        resizeTimer = new Timer(250, e -> saveWindowState());
        resizeTimer.setRepeats(false);
        resizeTimer.setCoalesce(false);
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                resizeTimer.restart();
            }
        });

        mediaPanel = new JPanel();
        mediaPanel.setLayout(new BorderLayout());
        mediaPanel.add(AudioPanel.getInstance(), BorderLayout.CENTER);
        mediaPanel.add(ControlPanel.getInstance(), BorderLayout.SOUTH);
        add(mediaPanel, BorderLayout.CENTER);
        //add(Playlist.getInstance(), BorderLayout.CENTER);
    }

    public void togglePlaylistVisible(boolean saveNewState) {
        int oldHeight = getHeight();
        int audioPanelHeight = AudioPanel.getInstance().getHeight();
        if (playlistVisible) {
            remove(mediaPanel);
            remove(Playlist.getInstance());
            //setMinimumSize(new Dimension(MIN_WIDTH, MIN_HEIGHT));
            setSize(new Dimension(getWidth(), oldHeight - expandedViewHeightDelta));
            AudioPanel.getInstance().setPreferredSize(new Dimension(1, audioPanelHeight));
            add(mediaPanel, BorderLayout.CENTER);
            playlistVisible = false;
        } else {
            remove(mediaPanel);
            setSize(new Dimension(getWidth(), oldHeight + expandedViewHeightDelta));
            AudioPanel.getInstance().setPreferredSize(new Dimension(1, audioPanelHeight));
            add(mediaPanel, BorderLayout.NORTH);
            add(Playlist.getInstance(), BorderLayout.CENTER);
            //setMinimumSize(new Dimension(MIN_WIDTH, MIN_HEIGHT + expandedViewHeightDelta));
            playlistVisible = true;
        }
        rejigger(this);

        if (saveNewState) {
            AppConfig.getInstance().setPlaylistVisible(playlistVisible);
            AppConfig.getInstance().save();
        }
    }

    /**
     * Overridden here so we can load our app config settings and
     * start the "idle" animation.
     *
     * @param visible If true, the MainWindow will be shown and the app will begin.
     */
    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            AppConfig.getInstance().load();
            loadWindowState();
            AudioPanelIdleAnimation.go();
        }
    }

    /**
     * Loads and returns an image icon resource, scaling up or down to the given size if needed.
     *
     * @param resourceName The path to the resource file containing the image.
     * @param width        The desired width of the image.
     * @param height       The desired height of the image.
     * @return An image, loaded and scaled, or null if the resource was not found.
     */
    public static BufferedImage loadIconResource(String resourceName, int width, int height) {
        BufferedImage image = null;
        try {
            URL url = MainWindow.class.getResource(resourceName);
            if (url == null) {
                throw new IOException("Image resource not found: " + resourceName);
            }
            image = ImageUtil.loadImage(url);

            // If the width or height don't match, scale it up or down as needed:
            if (image.getWidth() != width || image.getHeight() != height) {
                image = ImageUtil.generateThumbnailWithTransparency(image, width, height);
            }
        } catch (IOException ioe) {
            logger.log(Level.SEVERE, "Error loading image: " + ioe.getMessage(), ioe);
        }

        return image;
    }

    /**
     * Stupid swing nonsense... need to force a repaint sometimes to force components to
     * appear/disappear when added/removed.
     */
    public static void rejigger(final Component jiggy) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                jiggy.invalidate();
                jiggy.revalidate();
                jiggy.repaint();
            }
        });
    }

    private void saveWindowState() {
        AppConfig.getInstance().setWindowWidth(getWidth());
        AppConfig.getInstance().setWindowHeight(getHeight());
        AppConfig.getInstance().setPlaylistVisible(playlistVisible);
        AppConfig.getInstance().save();
    }

    private void loadWindowState() {
        setSize(new Dimension(AppConfig.getInstance().getWindowWidth(), AppConfig.getInstance().getWindowHeight()));
        if (playlistVisible != AppConfig.getInstance().isPlaylistVisible()) {
            togglePlaylistVisible(false);
        }
    }

    public static MainWindow getInstance() {
        if (instance == null) {
            instance = new MainWindow();
        }
        return instance;
    }

    private MessageUtil getMessageUtil() {
        if (messageUtil == null) {
            messageUtil = new MessageUtil(getInstance(), logger);
        }
        return messageUtil;
    }
}
