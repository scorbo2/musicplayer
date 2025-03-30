package ca.corbett.musicplayer;

import ca.corbett.musicplayer.ui.MainWindow;

/**
 * TODO fix logging... pack a default logging.properties and give option to override it, like the EMS app
 */
public class Main {
    public static void main(String[] args) {
        MainWindow.getInstance().setVisible(true);
    }
}