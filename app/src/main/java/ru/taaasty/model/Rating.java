package ru.taaasty.model;

/**
* Created by alexey on 01.08.14.
*/
public class Rating {

    public static final Rating DUMMY = new Rating();

    public int votes;

    public float rating;

    public long entryId;

    public boolean isVoted;

    public boolean isVoteable;

}
