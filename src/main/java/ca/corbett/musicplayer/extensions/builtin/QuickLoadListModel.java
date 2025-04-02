package ca.corbett.musicplayer.extensions.builtin;

import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * A ListModel specifically for the quickload playlist dialog.
 *
 * @author scorbo2
 * @since 2019-11-08
 */
public class QuickLoadListModel implements ListModel<File> {

    private List<String> list;
    private List<File> actualList;
    private List<ListDataListener> listeners;

    public QuickLoadListModel() {
        actualList = new ArrayList<>();
        list = new ArrayList<>();
        listeners = new ArrayList<>();
    }

    public void clear() {
        actualList.clear();
        list.clear();
        for (ListDataListener listener : listeners) {
            listener.contentsChanged(new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, 0));
        }
    }

    public void add(File item) {
        actualList.add(item);
        list.add(item.getName());
        for (ListDataListener listener : listeners) {
            listener.intervalAdded(new ListDataEvent(this, ListDataEvent.INTERVAL_ADDED, list.size() - 1, list.size()));
        }
    }

    public void remove(String item) {
        int index = list.indexOf(item);
        list.remove(item);
        actualList.remove(index);
        if (index != -1) {
            for (ListDataListener listener : listeners) {
                listener.intervalRemoved(new ListDataEvent(this, ListDataEvent.INTERVAL_REMOVED, index, index + 1));
            }
        }
    }

    @Override
    public int getSize() {
        return list.size();
    }

    @Override
    public File getElementAt(int index) {
        return actualList.get(index);
    }

    @Override
    public void addListDataListener(ListDataListener arg0) {
        listeners.add(arg0);
    }

    @Override
    public void removeListDataListener(ListDataListener arg0) {
        listeners.remove(arg0);
    }

}
