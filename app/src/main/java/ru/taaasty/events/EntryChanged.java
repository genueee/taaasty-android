package ru.taaasty.events;

import ru.taaasty.model.Entry;

/**
 * Обновился или добавился пост
 */
public class EntryChanged {

    public final Entry postEntry;

    public EntryChanged(Entry postEntry) {
        this.postEntry = postEntry;
    }
}
