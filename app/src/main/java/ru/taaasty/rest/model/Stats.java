
package ru.taaasty.rest.model;

import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;


// Испрользуем везде строки потому что за каким-то хером API возвращает "?", если значение неизвестно.
// Если вдруг API исправят - поменять на int
@AutoValue public abstract class Stats implements Parcelable {

    static Stats create(String publicEntriesInDayCount, String anonymousEntriesInDayCount, String bestEntriesInDayCount, String flowsCount) {
        return new AutoValue_Stats(publicEntriesInDayCount, anonymousEntriesInDayCount, bestEntriesInDayCount, flowsCount);
    }

    public static TypeAdapter<Stats> typeAdapter(Gson gson) {
        return new AutoValue_Stats.GsonTypeAdapter(gson);
    }

    @Nullable
    @SerializedName("public_entries_in_day_count")
    abstract String publicEntriesInDayCount();

    @Nullable
    @SerializedName("anonymous_entries_in_day_count")
    abstract String anonymousEntriesInDayCount();

    @Nullable
    @SerializedName("best_entries_in_day_count")
    abstract String bestEntriesInDayCount();

    @Nullable
    @SerializedName("flows_count")
    abstract String flowsCount();

    //@SerializedName("entries_count")
    //private String entriesCount;

    //@SerializedName("users_count")
    //private String usersCount;

    //@SerializedName("users_in_day_count")
    //private String usersInDayCount;

    //@SerializedName("comments_count")
    //private String commentsCount;

    //@SerializedName("comments_in_day_count")
    //private String commentsInDayCount;

    //@SerializedName("users_in_anonymous_count")
    //private String usersInAnonymousCount;

    //@SerializedName("users_in_best_count")
    //private String usersInBestCount;

    //@SerializedName("users_in_live_count")
    //private String usersInLiveCount;

    //@SerializedName("total_anonymous_entries_count")
    //private String totalAnonymousEntriesCount;

    //@SerializedName("votes_in_day_count")
    //private String votesInDayCount;

    //@SerializedName("users_votes_in_day_count")
    //private String usersVotesInDayCount;


    private Integer parseIntSilent(String longVal) {
        try {
            return Integer.parseInt(longVal);
        } catch (Throwable e) {
            return null;
        }
    }

    public Integer getPublicEntriesInDayCount() {
        return parseIntSilent(publicEntriesInDayCount());
    }


    public Integer getBestEntriesInDayCount() {
        return parseIntSilent(bestEntriesInDayCount());
    }

    public Integer getAnonymousEntriesInDayCount() {
        return parseIntSilent(anonymousEntriesInDayCount());
    }

    public Integer getFlowsTotal() {
        return parseIntSilent(flowsCount());
    }
}
