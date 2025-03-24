package ca.corbett.musicplayer.extensions;

import ca.corbett.extensions.ExtensionManager;

public class MusicPlayerExtensionManager extends ExtensionManager<MusicPlayerExtension> {

    private static MusicPlayerExtensionManager instance;

    protected MusicPlayerExtensionManager() {

    }

    public static MusicPlayerExtensionManager getInstance() {
        if (instance == null) {
            instance = new MusicPlayerExtensionManager();
        }

        return instance;
    }
}
