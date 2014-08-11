package ru.taaasty.model;

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
                ", entryId=" + entryId +
                ", isVoted=" + isVoted +
                ", isVoteable=" + isVoteable +
                '}';
    }
}
