
package ru.taaasty.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;


public class Stats implements Parcelable {

    @SerializedName("entries_count")
    public long entriesCount;

    @SerializedName("public_entries_in_day_count")
    public int publicEntriesInDayCount;

    @SerializedName("users_count")
    public int usersCount;

    @SerializedName("users_in_day_count")
    public int usersInDayCount;

    @SerializedName("comments_count")
    public int commentsCount;

    @SerializedName("comments_in_day_count")
    public int commentsInDayCount;

    @SerializedName("users_in_anonymous_count")
    public int usersInAnonymousCount;

    @SerializedName("users_in_best_count")
    public int usersInBestCount;

    @SerializedName("users_in_live_count")
    public int usersInLiveCount;

    @SerializedName("total_anonymous_entries_count")
    public int totalAnonymousEntriesCount;

    @SerializedName("anonymous_entries_in_day_count")
    public int anonymousEntriesInDayCount;

    @SerializedName("votes_in_day_count")
    public int votesInDayCount;

    @SerializedName("best_entries_in_day_count")
    public int bestEntriesInDayCount;

    @SerializedName("users_votes_in_day_count")
    public int usersVotesInDayCount;


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.entriesCount);
        dest.writeInt(this.publicEntriesInDayCount);
        dest.writeInt(this.usersCount);
        dest.writeInt(this.usersInDayCount);
        dest.writeInt(this.commentsCount);
        dest.writeInt(this.commentsInDayCount);
        dest.writeInt(this.usersInAnonymousCount);
        dest.writeInt(this.usersInBestCount);
        dest.writeInt(this.usersInLiveCount);
        dest.writeInt(this.totalAnonymousEntriesCount);
        dest.writeInt(this.anonymousEntriesInDayCount);
        dest.writeInt(this.votesInDayCount);
        dest.writeInt(this.bestEntriesInDayCount);
        dest.writeInt(this.usersVotesInDayCount);
    }

    public Stats() {
    }

    private Stats(Parcel in) {
        this.entriesCount = in.readLong();
        this.publicEntriesInDayCount = in.readInt();
        this.usersCount = in.readInt();
        this.usersInDayCount = in.readInt();
        this.commentsCount = in.readInt();
        this.commentsInDayCount = in.readInt();
        this.usersInAnonymousCount = in.readInt();
        this.usersInBestCount = in.readInt();
        this.usersInLiveCount = in.readInt();
        this.totalAnonymousEntriesCount = in.readInt();
        this.anonymousEntriesInDayCount = in.readInt();
        this.votesInDayCount = in.readInt();
        this.bestEntriesInDayCount = in.readInt();
        this.usersVotesInDayCount = in.readInt();
    }

    public static final Creator<Stats> CREATOR = new Creator<Stats>() {
        public Stats createFromParcel(Parcel source) {
            return new Stats(source);
        }

        public Stats[] newArray(int size) {
            return new Stats[size];
        }
    };
}
