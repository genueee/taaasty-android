package ru.taaasty.events;

import ru.taaasty.model.Entry;

public class UserLikeOrCommentUpdate {

    public final Entry postEntry;

    public UserLikeOrCommentUpdate(Entry postEntry) {
        this.postEntry = postEntry;
    }
}
