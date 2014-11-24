package ru.taaasty.utils;

public class Objects {

    /**
     * Null-safe equivalent of {@code a.equals(b)}.
     */
    public static boolean equals(Object a, Object b) {
        return (a == null) ? (b == null) : a.equals(b);
    }

    public static int compare(long a, long b) {
        return (a < b) ? -1 : ((a > b) ? 1 : 0);
    }

    public static int unsignedCompare(long l1, long l2) {
        return compare(flip(l1), flip(l2));
    }

    /**
     * A (self-inverse) bijection which converts the ordering on unsigned longs to the ordering on
     * longs, that is, {@code a <= b} as unsigned longs if and only if {@code flip(a) <= flip(b)}
     * as signed longs.
     */
    private static long flip(long a) {
        return a ^ Long.MIN_VALUE;
    }


}
