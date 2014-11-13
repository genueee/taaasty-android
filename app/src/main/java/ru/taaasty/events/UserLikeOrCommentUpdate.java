package ru.taaasty.events;

import ru.taaasty.model.Entry;

/**
 * Обновились лайки или кол-во комментариев у поста
 */
public class UserLikeOrCommentUpdate {

    public final Entry postEntry;

    public UserLikeOrCommentUpdate(Entry postEntry) {
        this.postEntry = postEntry;
    }
}
