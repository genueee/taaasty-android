
package ru.taaasty.rest.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;


// Испрользуем везде строки потому что за каким-то хером API возвращает "?", если значение неизвестно.
// Если вдруг API исправят - поменять на int
public class Stats implements Parcelable {

    @SerializedName("entries_count")
    private String entriesCount;

    @SerializedName("public_entries_in_day_count")
    private String publicEntriesInDayCount;

    @SerializedName("users_count")
    private String usersCount;

    @SerializedName("users_in_day_count")
    private String usersInDayCount;

    @SerializedName("comments_count")
    private String commentsCount;

    @SerializedName("comments_in_day_count")
    private String commentsInDayCount;

    @SerializedName("users_in_anonymous_count")
    private String usersInAnonymousCount;

    @SerializedName("users_in_best_count")
    private String usersInBestCount;

    @SerializedName("users_in_live_count")
    private String usersInLiveCount;

    @SerializedName("total_anonymous_entries_count")
    private String totalAnonymousEntriesCount;

    @SerializedName("anonymous_entries_in_day_count")
    private String anonymousEntriesInDayCount;

    @SerializedName("votes_in_day_count")
    private String votesInDayCount;

    @SerializedName("best_entries_in_day_count")
    private String bestEntriesInDayCount;

    @SerializedName("users_votes_in_day_count")
    private String usersVotesInDayCount;

    @SerializedName("flows_count")
    private String flowsCount;


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.entriesCount);
        dest.writeString(this.publicEntriesInDayCount);
        dest.writeString(this.usersCount);
        dest.writeString(this.usersInDayCount);
        dest.writeString(this.commentsCount);
        dest.writeString(this.commentsInDayCount);
        dest.writeString(this.usersInAnonymousCount);
        dest.writeString(this.usersInBestCount);
        dest.writeString(this.usersInLiveCount);
        dest.writeString(this.totalAnonymousEntriesCount);
        dest.writeString(this.anonymousEntriesInDayCount);
        dest.writeString(this.votesInDayCount);
        dest.writeString(this.bestEntriesInDayCount);
        dest.writeString(this.usersVotesInDayCount);
        dest.writeString(this.flowsCount);
    }

    public Stats() {
    }

    private Stats(Parcel in) {
        this.entriesCount = in.readString();
        this.publicEntriesInDayCount = in.readString();
        this.usersCount = in.readString();
        this.usersInDayCount = in.readString();
        this.commentsCount = in.readString();
        this.commentsInDayCount = in.readString();
        this.usersInAnonymousCount = in.readString();
        this.usersInBestCount = in.readString();
        this.usersInLiveCount = in.readString();
        this.totalAnonymousEntriesCount = in.readString();
        this.anonymousEntriesInDayCount = in.readString();
        this.votesInDayCount = in.readString();
        this.bestEntriesInDayCount = in.readString();
        this.usersVotesInDayCount = in.readString();
        this.flowsCount = in.readString();
    }

    public static final Creator<Stats> CREATOR = new Creator<Stats>() {
        public Stats createFromParcel(Parcel source) {
            return new Stats(source);
        }

        public Stats[] newArray(int size) {
            return new Stats[size];
        }
    };


    private Integer parseIntSilent(String longVal) {
        try {
            return Integer.parseInt(longVal);
        } catch (Throwable e) {
            return null;
        }
    }

    public Integer getPublicEntriesInDayCount() {
        return parseIntSilent(publicEntriesInDayCount);
    }

    public Integer getBestEntriesInDayCount() {
        return parseIntSilent(bestEntriesInDayCount);
    }

    public Integer getAnonymousEntriesInDayCount() {
        return parseIntSilent(anonymousEntriesInDayCount);
    }

    public Integer getFlowsTotal() {
        return parseIntSilent(flowsCount);
    }
}
