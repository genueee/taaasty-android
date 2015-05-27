package ru.taaasty.rest.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
* Created by alexey on 01.08.14.
*/
public class Rating implements Parcelable {

    public static final Rating DUMMY = new Rating();

    public int votes;

    public float rating;

    public long entryId;

    public boolean isVoted;

    public boolean isVoteable;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.votes);
        dest.writeFloat(this.rating);
        dest.writeLong(this.entryId);
        dest.writeByte(isVoted ? (byte) 1 : (byte) 0);
        dest.writeByte(isVoteable ? (byte) 1 : (byte) 0);
    }

    public Rating() {
    }

    private Rating(Parcel in) {
        this.votes = in.readInt();
        this.rating = in.readFloat();
        this.entryId = in.readLong();
        this.isVoted = in.readByte() != 0;
        this.isVoteable = in.readByte() != 0;
    }

    public static final Parcelable.Creator<Rating> CREATOR = new Parcelable.Creator<Rating>() {
        public Rating createFromParcel(Parcel source) {
            return new Rating(source);
        }

        public Rating[] newArray(int size) {
            return new Rating[size];
        }
    };

    @Override
    public String toString() {
        return "Rating{" +
                "votes=" + votes +
                ", rating=" + rating +
                ", entityId=" + entryId +
                ", isVoted=" + isVoted +
                ", isVoteable=" + isVoteable +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Rating rating1 = (Rating) o;

        if (votes != rating1.votes) return false;
        if (Float.compare(rating1.rating, rating) != 0) return false;
        if (entryId != rating1.entryId) return false;
        if (isVoted != rating1.isVoted) return false;
        return isVoteable == rating1.isVoteable;

    }

    @Override
    public int hashCode() {
        int result = votes;
        result = 31 * result + (rating != +0.0f ? Float.floatToIntBits(rating) : 0);
        result = 31 * result + (int) (entryId ^ (entryId >>> 32));
        result = 31 * result + (isVoted ? 1 : 0);
        result = 31 * result + (isVoteable ? 1 : 0);
        return result;
    }
}
