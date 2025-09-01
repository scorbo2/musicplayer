package ca.corbett.musicplayer;

import ca.corbett.extras.about.AboutInfo;

import java.io.File;

public final class Version {

    private static final AboutInfo aboutInfo;

    public static String NAME = "MusicPlayer";
    public static String VERSION = "2.9";
    public static String FULL_NAME = NAME + " " + VERSION;
    public static String COPYRIGHT = "Copyright © 2017-2025 Steve Corbett";
    public static String PROJECT_URL = "https://github.com/scorbo2/musicplayer";
    public static String LICENSE = "https://opensource.org/license/mit";

    /**
     * The directory where MusicPlayer was installed -
     * caution, this might be null! We can't guess a
     * value for this property, it has to be supplied
     * by the launcher script, but the launcher script
     * might have been modified by the user, or the user
     * might have started the app without using the launcher.
     * <p>
     * The installer script for linux defaults this
     * to /opt/MusicPlayer, but the user can override that.
     * </p>
     */
    public static final File INSTALL_DIR;

    /**
     * The directory where application configuration and
     * log files can go. If not given to us explicitly by
     * the launcher script, we default it a directory named
     * .MusicPlayer in the user's home directory.
     */
    public static final File SETTINGS_DIR;

    /**
     * The directory to scan for extension jars at startup.
     * If not given to us explicitly by the launcher script,
     * we default it to a directory called "extensions"
     * inside of SETTINGS_DIR.
     */
    public static final File EXTENSIONS_DIR;

    static {
        aboutInfo = new AboutInfo();
        aboutInfo.applicationName = NAME;
        aboutInfo.applicationVersion = VERSION;
        aboutInfo.copyright = COPYRIGHT;
        aboutInfo.license = LICENSE;
        aboutInfo.projectUrl = PROJECT_URL;
        aboutInfo.showLogConsole = true;
        aboutInfo.releaseNotesLocation = "/ca/corbett/musicplayer/ReleaseNotes.txt";
        aboutInfo.logoImageLocation = "/ca/corbett/musicplayer/images/logo_wide.jpg";
        aboutInfo.shortDescription = "Extensible music player with cool visualizations!";
        aboutInfo.logoDisplayMode = AboutInfo.LogoDisplayMode.STRETCH;

        String installDir = System.getProperty("INSTALL_DIR", null);
        INSTALL_DIR = installDir == null ? null : new File(installDir);

        String appDir = System.getProperty("SETTINGS_DIR",
                                           new File(System.getProperty("user.home"), "." + NAME).getAbsolutePath());
        SETTINGS_DIR = new File(appDir);
        if (!SETTINGS_DIR.exists()) {
            SETTINGS_DIR.mkdirs();
        }

        String extDir = System.getProperty("EXTENSIONS_DIR", new File(SETTINGS_DIR, "extensions").getAbsolutePath());
        EXTENSIONS_DIR = new File(extDir);
        if (!EXTENSIONS_DIR.exists()) {
            EXTENSIONS_DIR.mkdirs();
        }
    }

    public static AboutInfo getAboutInfo() {
        return aboutInfo;
    }
}
