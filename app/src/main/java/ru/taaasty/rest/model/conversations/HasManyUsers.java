package ru.taaasty.rest.model.conversations;

import java.util.List;

import ru.taaasty.rest.model.User;

/**
 * Пользователей в чате несколько много (групповые чаты и публичные обсуждения)
 */
public interface HasManyUsers {

    List<User> getUsers();

    long[] getUsersLeft();

}
