package ca.corbett.musicplayer.actions;

import ca.corbett.musicplayer.ui.CustomSortDialog;
import ca.corbett.musicplayer.ui.Playlist;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.logging.Logger;

/**
 * Shows the sort menu for the playlist.
 * There are a few built-in sort options that can be quickly selected,
 * or the user can choose to open the custom sort dialog to define
 * their own sort order.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since MusicPlayer 3.1
 */
public class PlaylistSortAction extends AbstractAction {

    private static final Logger log = Logger.getLogger(PlaylistSortAction.class.getName());

    private final JPopupMenu menu;
    private JComponent ownerComponent;

    private static final AbstractAction sortByTitleAction = new AbstractAction("Sort by title") {
        @Override
        public void actionPerformed(ActionEvent e) {
            Playlist.getInstance().sort(List.of(Playlist.SortKey.asc(Playlist.SortAttribute.Title)));
        }
    };

    private static final AbstractAction sortByArtistThenTitleAction = new AbstractAction("Sort by artist, then title") {
        @Override
        public void actionPerformed(ActionEvent e) {
            Playlist.getInstance().sort(List.of(
                Playlist.SortKey.asc(Playlist.SortAttribute.Artist),
                Playlist.SortKey.asc(Playlist.SortAttribute.Title))
            );
        }
    };
    private static final AbstractAction sortByArtistAlbumTitle = new AbstractAction("Sort by artist, album, then title") {
        @Override
        public void actionPerformed(ActionEvent e) {
            Playlist.getInstance().sort(List.of(
                Playlist.SortKey.asc(Playlist.SortAttribute.Artist),
                Playlist.SortKey.asc(Playlist.SortAttribute.Album),
                Playlist.SortKey.asc(Playlist.SortAttribute.Title))
            );
        }
    };
    private static final AbstractAction sortByGenreArtistAlbumTitle = new AbstractAction("Sort by genre, artist, album, then title") {
        @Override
        public void actionPerformed(ActionEvent e) {
            Playlist.getInstance().sort(List.of(
                Playlist.SortKey.asc(Playlist.SortAttribute.Genre),
                Playlist.SortKey.asc(Playlist.SortAttribute.Artist),
                Playlist.SortKey.asc(Playlist.SortAttribute.Album),
                Playlist.SortKey.asc(Playlist.SortAttribute.Title))
            );
        }
    };
    private static final AbstractAction reverseSortAction = new AbstractAction("Reverse current sort") {
        @Override
        public void actionPerformed(ActionEvent e) {
            Playlist.getInstance().reverseSort();
        }
    };
    private static final AbstractAction customSortAction = new AbstractAction("Custom sort...") {
        @Override
        public void actionPerformed(ActionEvent e) {
            new CustomSortDialog().setVisible(true);
        }
    };

    public PlaylistSortAction() {
        super("Sort playlist");
        menu = new JPopupMenu("Sort playlist");
        menu.add(sortByTitleAction);
        menu.add(sortByArtistThenTitleAction);
        menu.add(sortByArtistAlbumTitle);
        menu.add(sortByGenreArtistAlbumTitle);
        menu.addSeparator();
        menu.add(reverseSortAction);
        menu.add(customSortAction);
    }

    /**
     * Callers must provide an owner component for the popup menu to attach to.
     * If this is not set, the action will do nothing when performed.
     */
    public void setOwnerComponent(JComponent owner) {
        this.ownerComponent = owner;
        ownerComponent.setComponentPopupMenu(menu);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (ownerComponent != null) {
            menu.show(ownerComponent, 0, 0);
        }
        else {
            log.warning("No owner component set for PlaylistSortAction; cannot show sort menu.");
        }
    }
}
