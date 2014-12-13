package ru.taaasty.model;


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