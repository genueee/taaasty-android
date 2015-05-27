package ru.taaasty.events;

import ru.taaasty.rest.model.Comment;
import ru.taaasty.rest.model.Entry;

/**
 * Добавлен или обновлен комментарий
 */
public class CommentChanged {

    public final Entry entry;

    public final Comment comment;

    public CommentChanged(Entry entry, Comment comment) {
        this.entry = entry;
        this.comment = comment;
    }

}
