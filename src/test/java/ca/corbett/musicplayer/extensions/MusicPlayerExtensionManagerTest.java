package ca.corbett.musicplayer.extensions;

import ca.corbett.extensions.AppExtensionInfo;
import ca.corbett.extras.properties.AbstractProperty;
import org.junit.jupiter.api.Test;

import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


class MusicPlayerExtensionManagerTest {

    @Test
    public void findExtensionForPlaylistFormat_withNoExtension_shouldOnlyKnowMplist() {
        // GIVEN a stock extension manager with no extensions:
        MusicPlayerExtensionManager manager = new MusicPlayerExtensionManager();

        // WHEN we ask it what playlist file formats it understands:
        List<FileNameExtensionFilter> filters = manager.getPlaylistFileExtensionFilters();

        // THEN it should support only the mplist format (built-in):
        assertEquals(1, filters.size());
        assertTrue("mplist".equalsIgnoreCase(filters.get(0).getExtensions()[0]));
    }

    @Test
    public void findExtensionForPlaylistFormat_withXmlExtension_shouldSucceed() {
        // GIVEN an extension manager with custom playlist io support:
        MusicPlayerExtensionManager manager = new MusicPlayerExtensionManager();
        manager.addExtension(new XmlPlaylistExtension(), true);

        // WHEN we ask it what playlist file formats it understands:
        List<FileNameExtensionFilter> filters = manager.getPlaylistFileExtensionFilters();

        // THEN we should see that xml files are supported:
        assertEquals(2, filters.size());
        assertTrue("mplist".equalsIgnoreCase(filters.get(0).getExtensions()[0]));
        assertTrue("xml".equalsIgnoreCase(filters.get(1).getExtensions()[0]));

        // AND our extension should be returned as the handler for that file type:
        File xmlFile = new File("someFile.xml");
        assertNotNull(manager.findExtensionForPlaylistFormat(xmlFile));

        // AND if we disable that extension, support for that format should end:
        manager.setExtensionEnabled(XmlPlaylistExtension.class.getName(), false);
        assertNull(manager.findExtensionForPlaylistFormat(xmlFile));
    }

    private static class XmlPlaylistExtension extends MusicPlayerExtension {

        @Override
        public AppExtensionInfo getInfo() {
            return new AppExtensionInfo.Builder("XmlPlay").build();
        }

        @Override
        public List<AbstractProperty> getConfigProperties() {
            return List.of();
        }

        @Override
        public void onActivate() {
        }

        @Override
        public void onDeactivate() {
        }

        @Override
        public FileNameExtensionFilter getCustomPlaylistExtensionFilter() {
            return new FileNameExtensionFilter("Xml files", "xml");
        }
    }
}