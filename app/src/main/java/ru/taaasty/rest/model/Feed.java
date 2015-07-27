package ru.taaasty.rest.model;


public class Feed {

    public Entry[] entries;

    @Override
    public String toString() {
        return "Feed{" +
                "entries=" + entries +
                '}';
    }
}