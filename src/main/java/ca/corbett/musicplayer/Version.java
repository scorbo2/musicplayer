package ca.corbett.musicplayer;

import ca.corbett.extras.about.AboutInfo;

import java.io.File;

public final class Version {

    private static final AboutInfo aboutInfo;

    public static String NAME = "MusicPlayer";
    public static String VERSION = "2.6";
    public static String FULL_NAME = NAME + " " + VERSION;
    public static String COPYRIGHT = "Copyright © 2017 Steve Corbett";
    public static String PROJECT_URL = "https://github.com/scorbo2/musicplayer";
    public static String LICENSE = "https://opensource.org/license/mit";
    public static final File APPLICATION_DIR;
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

        String appDir = System.getProperty("APPLICATION_HOME", System.getProperty("user.home") + "." + NAME);
        APPLICATION_DIR = new File(appDir);
        if (!APPLICATION_DIR.exists()) {
            APPLICATION_DIR.mkdirs();
        }

        String extDir = System.getProperty("EXTENSIONS_DIR", appDir + File.separator + "extensions");
        EXTENSIONS_DIR = new File(extDir);
        if (!EXTENSIONS_DIR.exists()) {
            EXTENSIONS_DIR.mkdirs();
        }
    }

    public static AboutInfo getAboutInfo() {
        return aboutInfo;
    }
}
