package ca.corbett.musicplayer;

import ca.corbett.extras.LookAndFeelManager;
import ca.corbett.musicplayer.ui.MainWindow;
import com.formdev.flatlaf.FlatLightLaf;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

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
        Logger logger = Logger.getLogger(Main.class.getName());
        logger.log(Level.INFO,
                   Version.FULL_NAME + " starting up: installDir={0}, settingsDir={1}, extensionsDir={2}",
                   new Object[]{Version.INSTALL_DIR, Version.SETTINGS_DIR, Version.EXTENSIONS_DIR});
        checkJavaRuntime();
        LookAndFeelManager.installExtraLafs();
        LookAndFeelManager.switchLaf(FlatLightLaf.class.getName());
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
        File propsFile = new File(Version.SETTINGS_DIR, "logging.properties");
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

    /**
     * Not all JREs are created equally! Query the current JRE vendor, and show a warning
     * if it's one of the ones that have animation problems (screen flicker).
     * See <A HREF="https://github.com/scorbo2/musicplayer/issues/18">Issue 18</A> for details.
     */
    private static void checkJavaRuntime() {

//        System.out.println("java.vendor: " + System.getProperty("java.vendor"));
//        System.out.println("java.vm.specification.version: " + System.getProperty("java.vm.specification.version"));
//        System.out.println("java.vm.specification.vendor: " + System.getProperty("java.vm.specification.vendor"));
//        System.out.println("java.vm.specification.name: " + System.getProperty("java.vm.specification.name"));
//        System.out.println("java.vm.vendor: " + System.getProperty("java.vm.vendor"));
//        System.out.println("java.runtime.name: " + System.getProperty("java.runtime.name"));
//        System.out.println("java.specification.vendor: " + System.getProperty("java.specification.vendor"));
//        System.out.println("java.specification.name: " + System.getProperty("java.specification.name"));

        String vendor = detectJREDistribution();
        Logger logger = Logger.getLogger(Main.class.getName());
        if (vendor.toLowerCase().contains("openjdk") ||
            vendor.toLowerCase().contains("azul")) {
            logger.warning("Your JRE vendor \"" + vendor + "\" may have problems with full-screen animation." +
                               " If you experience screen flicker or poor performance, consider switching to " +
                               "Amazon Corretto or Eclipse Temurin.");
        }
    }

    private static String detectJREDistribution() {
        String vendor = System.getProperty("java.vendor", "").toLowerCase();
        String vmName = System.getProperty("java.vm.name", "").toLowerCase();
        String javaRuntimeName = System.getProperty("java.runtime.name", "").toLowerCase();

        // Amazon Corretto
        if (vendor.contains("amazon") || javaRuntimeName.contains("corretto")) {
            return "Amazon Corretto";
        }

        // Azul Zulu
        if (vendor.contains("azul") || vmName.contains("zulu")) {
            return "Azul Zulu";
        }

        // Eclipse Temurin (formerly AdoptOpenJDK)
        if (vendor.contains("eclipse") || javaRuntimeName.contains("temurin")) {
            return "Eclipse Temurin";
        }

        // Oracle JDK vs Oracle's OpenJDK builds
        if (vendor.contains("oracle")) {
            // Oracle JDK typically has "Java(TM)" in the runtime name
            if (javaRuntimeName.contains("java(tm)")) {
                return "Oracle JDK";
            }
            else {
                return "Oracle OpenJDK";
            }
        }

        // Generic OpenJDK (other distributions)
        if (javaRuntimeName.contains("openjdk")) {
            return "OpenJDK (Generic)";
        }

        return "Unknown: " + vendor;
    }
}