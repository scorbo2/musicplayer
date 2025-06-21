package ca.corbett.musicplayer;

import ca.corbett.musicplayer.ui.MainWindow;

import java.io.File;
import java.io.IOException;
import java.util.logging.LogManager;

/**
 * The entry point for the application. There are no command line parameters,
 * but you can specify a few system properties to achieve different things:
 * <ul>
 *     <li><b>java.util.logging.config.file</b> - if set, this is the full path
 *     and name of your custom logging.properties file. If not set, the default
 *     logging.properties will be used (from the application jar file). Note that
 *     we also check for a logging.properties in whatever directory you launch
 *     from, and unless the above property is set, we'll use that one if it exists.
 *     By default, all log output goes to the console. You can specify a custom
 *     logging.properties to easily change that.</li>
 *     <li><b>ca.corbett.musicplayer.props.file</b> - if set, this is the full
 *     path and name of your MusicPlayer.props file containing application settings.
 *     If not set, this file will be created as needed in your home directory.
 *     If the file does not exist on startup, the application will launch with
 *     all default properties, and the file will be created if you change any
 *     of the application settings.</li>
 *     <li><b>ca.corbett.musicplayer.extensions.dir</b> - if set, this is the
 *     directory from which extension jars will be loaded. There is no default value,
 *     so if you don't set this, you will be unable to load extension jars.</li>
 * </ul>
 *
 * @author scorbo2
 * @since 2025-03-23
 */
public class Main {
    public static void main(String[] args) {
        configureLogging();
        AppConfig.getInstance().loadWithoutUIReload();
        MainWindow.getInstance().setVisible(true);
    }

    /**
     * Logging can use the built-in configuration, or you can supply your own logging properties file.
     * <ol>
     *     <li><b>Built-in logging.properties</b>: the jar file comes packaged with a default logging.properties
     *     file that you can use. You don't need to do anything to activate this config: this is the default.</li>
     *     <li><b>Specify your own</b>: you can create a logging.properties file and put it in the directory
     *     from which you launch the application. It will be detected and used. OR you can start the application
     *     with the -Djava.util.logging.config.file= option, in which case you can point it to wherever your
     *     logging.properties file lives.</li>
     * </ol>
     */
    private static void configureLogging() {
        // If the java.util.logging.config.file System property exists, do nothing.
        // It will be used automatically.
        if (System.getProperties().containsKey("java.util.logging.config.file")) {
            //System.out.println("Using custom log file: " + System.getProperty("java.util.logging.config.file"));
            return;
        }

        // Otherwise, see if we can spot a logging.properties file in the application dir:
        File propsFile = new File(Version.APPLICATION_DIR, "logging.properties");
        if (propsFile.exists() && propsFile.canRead()) {
            System.setProperty("java.util.logging.config.file", propsFile.getAbsolutePath());
            //System.out.println("Using auto-detected log file: " + propsFile.getAbsolutePath());
            return;
        }

        // Otherwise, load the built-in config:
        try {
            //System.out.println("Using built-in logging.");
            LogManager.getLogManager().readConfiguration(Main.class.getResourceAsStream("/ca/corbett/musicplayer/logging.properties"));
        } catch (IOException ioe) {
            System.out.println("WARN: Unable to load log configuration: " + ioe.getMessage());
        }
    }
}