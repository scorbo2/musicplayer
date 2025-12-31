package ca.corbett.musicplayer;

import ca.corbett.extras.LookAndFeelManager;
import ca.corbett.extras.SingleInstanceManager;
import ca.corbett.musicplayer.ui.MainWindow;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.SwingUtilities;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * The entry point for the application. There are no command line parameters,
 * but you can specify a few system properties to achieve different things:
 * <ul>
 *     <li><b>java.util.logging.config.file</b> - if set, this is the full path
 *     and name of your custom logging.properties file. If not set, we will
 *     look for a logging.properties file in your application settings dir;
 *     if not found there, then finally, as a last resort, the default
 *     logging.properties will be used (from the application jar file).
 *     By default, all log output goes to the console. You can specify a custom
 *     logging.properties to easily change that.</li>
 *     <li><b>SETTINGS_DIR</b> - This defaults to a directory named ".MusicPlayer"
 *     in the user's home directory, but can be overridden. The application
 *     configuration file lives here.</li>
 *     <li><b>EXTENSIONS_DIR</b> - This defaults to a directory named "extensions"
 *     inside SETTINGS_DIR, but can be overridden. This is the
 *     directory from which extension jars will be loaded.</li>
 * </ul>
 * <p>
 *     <b>Note:</b> If you used the installer script to install MusicPlayer,
 *     these system properties will already have been set for you in the
 *     launcher script, and you don't have to worry about them.
 * </p>
 *
 * @author scorbo2
 * @since 2025-03-23
 */
public class Main {

    /**
     * Don't use the default port unless you have to!
     * It will conflict with any other application that was built with swing-extras.
     * The best practice is for each application to pick its own unique port number.
     */
    public static final int SINGLE_INSTANCE_PORT = 44884;

    public static void main(String[] args) {
        // Before we do anything else, set up logging:
        configureLogging();

        // Ensure only a single instance is running (if configured to do so):
        boolean isSingleInstanceEnabled = Boolean.parseBoolean(AppConfig.peek("UI.General.singleInstance"));
        if (isSingleInstanceEnabled) {
            SingleInstanceManager instanceManager = SingleInstanceManager.getInstance();
            if (!instanceManager.tryAcquireLock(Main::handleStartArgs, SINGLE_INSTANCE_PORT)) {
                // Another instance is already running, let's send our args to it and exit:
                // Send even if empty, as this will force the main window to the front.
                instanceManager.sendArgsToRunningInstance(args);
                return;
            }
        }

        // We are the only instance running, so we can start up normally:
        Logger logger = Logger.getLogger(Main.class.getName());
        logger.log(Level.INFO,
                   Version.FULL_NAME + " starting up: installDir={0}, settingsDir={1}, extensionsDir={2}",
                   new Object[]{Version.INSTALL_DIR, Version.SETTINGS_DIR, Version.EXTENSIONS_DIR});
        checkJavaRuntime();
        LookAndFeelManager.installExtraLafs();
        AppConfig.getInstance().loadWithoutUIReload();

        SwingUtilities.invokeLater(() -> {
            LookAndFeelManager.switchLaf(FlatLightLaf.class.getName());
            MainWindow.getInstance().setVisible(true);
            MainWindow.getInstance().processStartArgs(Arrays.asList(args));
        });
    }

    /**
     * Logging can use the built-in configuration, or you can supply your own logging properties file.
     * This code will look for logging configuration in the following order:
     * <ol>
     *     <li>System property <b>java.util.logging.config.file</b> - if set, this file will be read
     *     for logging configuration.</li>
     *     <li>A <b>logging.properties</b> file in your application settings dir (via the SETTINGS_DIR variable).
     *     If this file is found, it will be read.</li>
     *     <li><b>Built-in logging.properties</b>: the jar file comes packaged with a default logging.properties
     *     file that you can use. If neither of the above checks succeeded, this log configuration will be used.</li>
     * </ol>
     */
    private static void configureLogging() {
        // If the java.util.logging.config.file System property exists, do nothing.
        // It will be used automatically.
        if (System.getProperties().containsKey("java.util.logging.config.file")) {
            //System.out.println("Using custom log file: " + System.getProperty("java.util.logging.config.file"));
            return;
        }

        // Otherwise, see if we can spot a logging.properties file in the application settings dir:
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
     * Invoked internally to handle start arguments on the EDT.
     */
    private static void handleStartArgs(List<String> args) {
        SwingUtilities.invokeLater(() -> MainWindow.getInstance().processStartArgs(args));
    }

    /**
     * Not all JREs are created equally! Query the current JRE vendor, and show a warning
     * if it's one of the ones that have animation problems (screen flicker).
     * See <A HREF="https://github.com/scorbo2/musicplayer/issues/18">Issue 18</A> for details.
     */
    private static void checkJavaRuntime() {
        // Diagnostic information for debugging:
        Logger logger = Logger.getLogger(Main.class.getName());
        logger.fine("Java Runtime Info: ");
        logger.fine("  java.version: " + System.getProperty("java.version"));
        logger.fine("  java.vendor: " + System.getProperty("java.vendor"));
        logger.fine("  java.vm.specification.version: " + System.getProperty("java.vm.specification.version"));
        logger.fine("  java.vm.specification.vendor: " + System.getProperty("java.vm.specification.vendor"));
        logger.fine("  java.vm.specification.name: " + System.getProperty("java.vm.specification.name"));
        logger.fine("  java.vm.vendor: " + System.getProperty("java.vm.vendor"));
        logger.fine("  java.vm.name: " + System.getProperty("java.vm.name"));
        logger.fine("  java.runtime.name: " + System.getProperty("java.runtime.name"));
        logger.fine("  java.specification.vendor: " + System.getProperty("java.specification.vendor"));
        logger.fine("  java.specification.name: " + System.getProperty("java.specification.name"));

        String vendor = detectJREDistribution();
        if (vendor.toLowerCase().contains("openjdk") ||
            vendor.toLowerCase().contains("azul")) {
            logger.warning("Your JRE vendor \"" + vendor + "\" may have problems with full-screen animation." +
                               " If you experience screen flicker or poor performance, consider switching to " +
                               "Amazon Corretto or Eclipse Temurin.");
        }
    }

    /**
     * Make a best-effort guess at the JRE distribution vendor, based on system properties.
     */
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