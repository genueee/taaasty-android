package ru.taaasty.rest.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by alexey on 07.08.14.
 */
public class RelationshipsSummary implements Parcelable {

    public static RelationshipsSummary DUMMY = new RelationshipsSummary();

    public long followingsCount;

    public long guessesCount;

    public long followersCount;

    public long ignoredCount;


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.followingsCount);
        dest.writeLong(this.guessesCount);
        dest.writeLong(this.followersCount);
        dest.writeLong(this.ignoredCount);
    }

    public RelationshipsSummary() {
    }

    private RelationshipsSummary(Parcel in) {
        this.followingsCount = in.readLong();
        this.guessesCount = in.readLong();
        this.followersCount = in.readLong();
        this.ignoredCount = in.readLong();
    }

    public static final Parcelable.Creator<RelationshipsSummary> CREATOR = new Parcelable.Creator<RelationshipsSummary>() {
        public RelationshipsSummary createFromParcel(Parcel source) {
            return new RelationshipsSummary(source);
        }

        public RelationshipsSummary[] newArray(int size) {
            return new RelationshipsSummary[size];
        }
    };

    @Override
    public String toString() {
        return "RelationshipsSummary{" +
                "followingsCount=" + followingsCount +
                ", guessesCount=" + guessesCount +
                ", followersCount=" + followersCount +
                ", ignoredCount=" + ignoredCount +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RelationshipsSummary that = (RelationshipsSummary) o;

        if (followingsCount != that.followingsCount) return false;
        if (guessesCount != that.guessesCount) return false;
        if (followersCount != that.followersCount) return false;
        return ignoredCount == that.ignoredCount;

    }

    @Override
    public int hashCode() {
        int result = (int) (followingsCount ^ (followingsCount >>> 32));
        result = 31 * result + (int) (guessesCount ^ (guessesCount >>> 32));
        result = 31 * result + (int) (followersCount ^ (followersCount >>> 32));
        result = 31 * result + (int) (ignoredCount ^ (ignoredCount >>> 32));
        return result;
    }
}
