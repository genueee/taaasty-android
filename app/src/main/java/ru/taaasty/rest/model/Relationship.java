package ru.taaasty.rest.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.Comparator;

import ru.taaasty.utils.Objects;

public class Relationship implements Parcelable {

    public static final String RELATIONSHIP_NONE = "none";
    public static final String RELATIONSHIP_FRIEND = "friend";
    public static final String RELATIONSHIP_GUESSED = "guessed";
    public static final String RELATIONSHIP_IGNORED = "ignored";
    public static final String RELATIONSHIP_REQUESTED = "requested";

    @SerializedName("id")
    private Long mId;

    @SerializedName("user_id")
    private long mUserId = -1;

    @SerializedName("reader_id")
    private long mReaderId = -1;

    @SerializedName("state")
    private String mState = "";

    @SerializedName("reader")
    private User mReader = User.DUMMY;

    @SerializedName("user")
    private User mUser = User.DUMMY;


    /**
     * Сортировка по убыванию id (более новые - в начале списка)
     */
    public static transient Comparator<Relationship> ORDER_BY_ID_DESC_COMARATOR = new Comparator<Relationship>() {
        @Override
        public int compare(Relationship lhs, Relationship rhs) {
            if (lhs == null && rhs == null) {
                return 0;
            } else if (lhs == null || lhs.getId() == null) {
                return -1;
            } else if (rhs == null || rhs.getId() == null) {
                return 1;
            } else {
                return Objects.compare(rhs.getId(), lhs.getId());
            }
        }
    };

    public static transient Comparator<Relationship> ORDER_BY_TO_USER_ID_COMARATOR = new Comparator<Relationship>() {
        @Override
        public int compare(Relationship lhs, Relationship rhs) {
            if (lhs == null && rhs == null) {
                return 0;
            } else if (lhs == null || lhs.getId() == null) {
                return -1;
            } else if (rhs == null || rhs.getId() == null) {
                return 1;
            } else {
                return Objects.compare(rhs.getId(), lhs.getId());
            }
        }
    };


    public Relationship() {
    }

    public static boolean isMeSubscribed(String myRelationship) {
        return myRelationship != null && (RELATIONSHIP_FRIEND.equals(myRelationship)
                || RELATIONSHIP_REQUESTED.equals(myRelationship));
    }

    /**
     * ID отношения.
     * Может быть null, если отношения между пользователями нет.
     * @return
     */
    @Nullable
    public Long getId() {
        return mId;
    }

    public long getUserId() {
        return mUserId;
    }

    public long getReaderId() {
        return mReaderId;
    }

    public String getState() {
        return mState;
    }

    @Nullable
    @Deprecated
    // TODO постараться избавиться и не исопользовать, на многих точкх он не приходит
    public User getReader() {
        return mReader;
    }

    @Nullable
    @Deprecated
    // TODO постараться избавиться и не исопользовать, на многих точкх он не приходит
    public User getUser() {
        return mUser;
    }

    public long getFromId() {
        return mReaderId;
    }

    public long getToId() {
        return mUserId;
    }

    public boolean isMyRelation(long myUserId) {
        return isMyRelationToHim(myUserId) || isHisRelationToMe(myUserId);
    }

    public boolean isMyRelationToHim(long myUserId) {
        return (myUserId != CurrentUser.USER_UNAUTHORIZED_ID) && (myUserId == getFromId());
    }

    public boolean isHisRelationToMe(long myUserId) {
        return (myUserId != CurrentUser.USER_UNAUTHORIZED_ID) && (myUserId == getToId());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(this.mId);
        dest.writeLong(this.mUserId);
        dest.writeLong(this.mReaderId);
        dest.writeString(this.mState);
        dest.writeParcelable(this.mReader, 0);
        dest.writeParcelable(this.mUser, 0);
    }

    protected Relationship(Parcel in) {
        this.mId = (Long) in.readValue(Long.class.getClassLoader());
        this.mUserId = in.readLong();
        this.mReaderId = in.readLong();
        this.mState = in.readString();
        this.mReader = in.readParcelable(User.class.getClassLoader());
        this.mUser = in.readParcelable(User.class.getClassLoader());
    }

    public static final Creator<Relationship> CREATOR = new Creator<Relationship>() {
        public Relationship createFromParcel(Parcel source) {
            return new Relationship(source);
        }

        public Relationship[] newArray(int size) {
            return new Relationship[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Relationship that = (Relationship) o;

        if (mUserId != that.mUserId) return false;
        if (mReaderId != that.mReaderId) return false;
        if (mId != null ? !mId.equals(that.mId) : that.mId != null) return false;
        if (mState != null ? !mState.equals(that.mState) : that.mState != null) return false;
        if (mReader != null ? !mReader.equals(that.mReader) : that.mReader != null) return false;
        return !(mUser != null ? !mUser.equals(that.mUser) : that.mUser != null);

    }

    @Override
    public int hashCode() {
        int result = mId != null ? mId.hashCode() : 0;
        result = 31 * result + (int) (mUserId ^ (mUserId >>> 32));
        result = 31 * result + (int) (mReaderId ^ (mReaderId >>> 32));
        result = 31 * result + (mState != null ? mState.hashCode() : 0);
        result = 31 * result + (mReader != null ? mReader.hashCode() : 0);
        result = 31 * result + (mUser != null ? mUser.hashCode() : 0);
        return result;
    }
}
