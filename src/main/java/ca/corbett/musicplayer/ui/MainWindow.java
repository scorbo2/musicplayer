package ca.corbett.musicplayer.ui;

import ca.corbett.extras.MessageUtil;
import ca.corbett.extras.image.ImageUtil;
import ca.corbett.musicplayer.Version;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainWindow extends JFrame {

    private static final Logger logger = Logger.getLogger(MainWindow.class.getName());

    private static MainWindow instance;
    private MessageUtil messageUtil;
    private boolean playlistVisible = false;
    int compactViewHeight = 250;
    int expandedViewHeight = 450;
    private JPanel mediaPanel;

    private MainWindow() {
        super(Version.FULL_NAME);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(new Dimension(420, compactViewHeight));
        setLayout(new BorderLayout());

        mediaPanel = new JPanel();
        mediaPanel.setLayout(new BorderLayout());
        mediaPanel.add(AudioPanel.getInstance(), BorderLayout.CENTER);
        mediaPanel.add(ControlPanel.getInstance(), BorderLayout.SOUTH);
        add(mediaPanel, BorderLayout.CENTER);
        //add(Playlist.getInstance(), BorderLayout.CENTER);
    }

    public void togglePlaylistVisible() {
        if (playlistVisible) {
            remove(mediaPanel);
            remove(Playlist.getInstance());
            int audioPanelHeight = AudioPanel.getInstance().getHeight();
            setSize(new Dimension(getWidth(), compactViewHeight));
            AudioPanel.getInstance().setPreferredSize(new Dimension(1, audioPanelHeight));
            add(mediaPanel, BorderLayout.CENTER);
            playlistVisible = false;
        } else {
            remove(mediaPanel);
            int audioPanelHeight = AudioPanel.getInstance().getHeight();
            setSize(new Dimension(getWidth(), expandedViewHeight));
            AudioPanel.getInstance().setPreferredSize(new Dimension(1, audioPanelHeight));
            add(mediaPanel, BorderLayout.NORTH);
            add(Playlist.getInstance(), BorderLayout.CENTER);
            playlistVisible = true;
        }
        rejigger(this);
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            Insets insets = this.getInsets();
            compactViewHeight = mediaPanel.getHeight() +
                    mediaPanel.getInsets().top +
                    mediaPanel.getInsets().bottom +
                    insets.top +
                    insets.bottom;
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
