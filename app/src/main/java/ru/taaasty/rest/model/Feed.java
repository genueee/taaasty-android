package ru.taaasty.rest.model;


import java.util.List;

public class Feed {

    public List<Entry> entries;

    @Override
    public String toString() {
        return "Feed{" +
                "entries=" + entries +
                '}';
    }
}