package ru.taaasty.rest.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.text.Collator;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

import ru.taaasty.Constants;

/**
* Created by alexey on 01.08.14.
*/
public class User implements Parcelable {

    /**
     * Загрушка. Используется, когда инфа о юзере ещё не загружена чтобы что-то показать
     */
    public static User DUMMY = new User();

    public static User ANONYMOUS = new User();

    public static Comparator<User> SORT_BY_NAME_COMPARATOR = new SortByNameComparator();

    static {
        // TODO: переделать
        ANONYMOUS.mId = 4409;
        ANONYMOUS.mTitle = "";
        ANONYMOUS.mName = "anonymous";
        ANONYMOUS.mSlug = "anonymous";
        ANONYMOUS.mTlogUrl = "http://taaasty.com/~anonymous";
        ANONYMOUS.mCreateAt = new Date(1202843744);
        ANONYMOUS.mUserpic = new Userpic();
        ANONYMOUS.mUserpic.originalUrl = "http://taaasty.com/assets/userpic/72/29/4409_original.png";
        ANONYMOUS.mUserpic.defaultColors.background = "#00000000";
        ANONYMOUS.mUserpic.defaultColors.name = "#ffffff";
    }

    private static class SortByNameComparator implements Comparator<User> {

        private final Collator mCollator;

        public SortByNameComparator() {
            mCollator = Collator.getInstance(Locale.getDefault());
            mCollator.setStrength(Collator.TERTIARY);
        }

        @Override
        public int compare(User lhs, User rhs) {
            return mCollator.compare(lhs.mName == null ? ""  : lhs.mName, rhs.mName == null ? "" : rhs.mName);
        }
    }

    @SerializedName("id")
    long mId = CurrentUser.USER_UNAUTHORIZED_ID;

    @SerializedName("name")
    String mName;

    @SerializedName("slug")
    String mSlug;

    @SerializedName("title")
    String mTitle;

    @SerializedName("is_female")
    boolean mIsFemale;

    @SerializedName("is_daylog")
    boolean mIsDaylog;

    @SerializedName("tlog_url")
    String mTlogUrl;

    @SerializedName("created_at")
    Date mCreateAt;

    // @SerializedName("updated_at")
    // Date mUpdatedAt; // Постоянно меняется, пока не используем

    @SerializedName("email")
    String mEmail;

    @SerializedName("is_privacy")
    boolean mIsPrivacy;

    @SerializedName("is_flow")
    boolean mIsFlow;

    @SerializedName("total_entries_count")
    long mTotalEntriesCount;

    @SerializedName("private_entries_count")
    long privateEntriesCount;

    @SerializedName("public_entries_count")
    long publicEntriesCount;

    @SerializedName("userpic")
    Userpic mUserpic;

    @SerializedName("relationships_summary")
    RelationshipsSummary mRelationshipsSummary;

    @SerializedName("design")
    TlogDesign mDesign;

    public long getId() {
        return mId;
    }

    public String getName() {
        return mName;
    }

    /**
     * @return ~username
     */
    public String getNameWithPrefix() {
        return Constants.USER_PREFIX + mName;
    }

    /**
     * Вариант name'а, используемый в URL. В большинстве случаев, не отличается от name
     */
    public String getSlug() {
        return mSlug != null ? mSlug : (mName != null ? mName : "");
    }

    @Nullable
    public Userpic getUserpic() {
        return mUserpic;
    }

    public String getTlogUrl() {
        return mTlogUrl;
    }

    /**
     * @return Подпись под аватаркой
     */
    public String getTitle() {
        return mTitle;
    }

    public Date getCreatedAt() {
        return mCreateAt;
    }

    public long getTotalEntriesCount() {
        return mTotalEntriesCount;
    }

    public long getPublicEntriesCount() {
        return publicEntriesCount;
    }

    public long getPrivateEntriesCount() {
        return privateEntriesCount;
    }

    public boolean isFemale() {
        return mIsFemale;
    }

    public boolean isPrivacy() {
        return mIsPrivacy;
    }

    public boolean isFlow() {
        return mIsFlow;
    }

    public long getDaysOnTasty() {
        long diffMs = Math.abs(System.currentTimeMillis() - mCreateAt.getTime());
        return Math.round(diffMs / (24f * 60f * 60f * 1000f)); // XXX: wrong
    }

    @Nullable
    public RelationshipsSummary getRelationshipsSummary() {
        return mRelationshipsSummary;
    }

    @Nullable
    public TlogDesign getDesign() {
        return mDesign;
    }

    // TODO избавиться
    public void setUserpic(Userpic userpic) {
        mUserpic = userpic;
    }

    /**
     * @return Пользователь - авторизованный (не гостевая сессия)
     */
    public boolean isAuthorized() {
        return mId != CurrentUser.USER_UNAUTHORIZED_ID;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.mId);
        dest.writeString(this.mName);
        dest.writeString(this.mSlug);
        dest.writeString(this.mTitle);
        dest.writeByte(mIsFemale ? (byte) 1 : (byte) 0);
        dest.writeByte(mIsDaylog ? (byte) 1 : (byte) 0);
        dest.writeString(this.mTlogUrl);
        dest.writeLong(mCreateAt != null ? mCreateAt.getTime() : -1);
        dest.writeString(this.mEmail);
        dest.writeByte(mIsPrivacy ? (byte) 1 : (byte) 0);
        dest.writeByte(mIsFlow ? (byte) 1 : (byte) 0);
        dest.writeLong(this.mTotalEntriesCount);
        dest.writeLong(this.privateEntriesCount);
        dest.writeLong(this.publicEntriesCount);
        dest.writeParcelable(this.mUserpic, flags);
        dest.writeParcelable(this.mDesign, flags);
        dest.writeParcelable(this.mRelationshipsSummary, flags);
    }

    public User() {
    }

    protected User(Parcel in) {
        this.mId = in.readLong();
        this.mName = in.readString();
        this.mSlug = in.readString();
        this.mTitle = in.readString();
        this.mIsFemale = in.readByte() != 0;
        this.mIsDaylog = in.readByte() != 0;
        this.mTlogUrl = in.readString();
        long tmpMCreateAt = in.readLong();
        this.mCreateAt = tmpMCreateAt == -1 ? null : new Date(tmpMCreateAt);
        this.mEmail = in.readString();
        this.mIsPrivacy = in.readByte() != 0;
        this.mIsFlow = in.readByte() != 0;
        this.mTotalEntriesCount = in.readLong();
        this.privateEntriesCount = in.readLong();
        this.publicEntriesCount = in.readLong();
        this.mUserpic = in.readParcelable(Userpic.class.getClassLoader());
        this.mDesign = in.readParcelable(TlogDesign.class.getClassLoader());
        this.mRelationshipsSummary = in.readParcelable(RelationshipsSummary.class.getClassLoader());
    }

    public static final Parcelable.Creator<User> CREATOR = new Parcelable.Creator<User>() {
        public User createFromParcel(Parcel source) {
            return new User(source);
        }

        public User[] newArray(int size) {
            return new User[size];
        }
    };

    @Override
    public String toString() {
        return "User{" +
                "mId=" + mId +
                ", mName='" + mName + '\'' +
                ", mSlug='" + mSlug + '\'' +
                ", mTitle='" + mTitle + '\'' +
                ", mIsFemale=" + mIsFemale +
                ", mIsDaylog=" + mIsDaylog +
                ", mTlogUrl='" + mTlogUrl + '\'' +
                ", mCreateAt=" + mCreateAt +
                ", mEmail='" + mEmail + '\'' +
                ", mIsPrivacy=" + mIsPrivacy +
                ", mIsFlow=" + mIsFlow +
                ", mTotalEntriesCount=" + mTotalEntriesCount +
                ", privateEntriesCount=" + privateEntriesCount +
                ", publicEntriesCount=" + publicEntriesCount +
                ", mUserpic=" + mUserpic +
                ", mDesign=" + mDesign +
                ", mRelationshipsSummary=" + mRelationshipsSummary +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        User user = (User) o;

        if (mId != user.mId) return false;

        if (mIsFemale != user.mIsFemale) return false;
        if (mIsDaylog != user.mIsDaylog) return false;
        if (mIsPrivacy != user.mIsPrivacy) return false;
        if (mIsFlow != user.mIsFlow) return false;
        if (mTotalEntriesCount != user.mTotalEntriesCount) return false;
        if (privateEntriesCount != user.privateEntriesCount) return false;
        if (publicEntriesCount != user.publicEntriesCount) return false;
        if (mName != null ? !mName.equals(user.mName) : user.mName != null) return false;
        if (mSlug != null ? !mSlug.equals(user.mSlug) : user.mSlug != null) return false;
        if (mTitle != null ? !mTitle.equals(user.mTitle) : user.mTitle != null) return false;
        if (mTlogUrl != null ? !mTlogUrl.equals(user.mTlogUrl) : user.mTlogUrl != null)
            return false;
        if (mCreateAt != null ? !mCreateAt.equals(user.mCreateAt) : user.mCreateAt != null)
            return false;
        if (mEmail != null ? !mEmail.equals(user.mEmail) : user.mEmail != null) return false;
        if (mUserpic != null ? !mUserpic.equals(user.mUserpic) : user.mUserpic != null)
            return false;
        if (mRelationshipsSummary != null ? !mRelationshipsSummary.equals(user.mRelationshipsSummary) : user.mRelationshipsSummary != null)
            return false;
        return !(mDesign != null ? !mDesign.equals(user.mDesign) : user.mDesign != null);

    }

    @Override
    public int hashCode() {
        int result = (int) (mId ^ (mId >>> 32));
        result = 31 * result + (mName != null ? mName.hashCode() : 0);
        result = 31 * result + (mSlug != null ? mSlug.hashCode() : 0);
        result = 31 * result + (mTitle != null ? mTitle.hashCode() : 0);
        result = 31 * result + (mIsFemale ? 1 : 0);
        result = 31 * result + (mIsDaylog ? 1 : 0);
        result = 31 * result + (mTlogUrl != null ? mTlogUrl.hashCode() : 0);
        result = 31 * result + (mCreateAt != null ? mCreateAt.hashCode() : 0);
        result = 31 * result + (mEmail != null ? mEmail.hashCode() : 0);
        result = 31 * result + (mIsPrivacy ? 1 : 0);
        result = 31 * result + (mIsFlow ? 1 : 0);
        result = 31 * result + (int) (mTotalEntriesCount ^ (mTotalEntriesCount >>> 32));
        result = 31 * result + (int) (privateEntriesCount ^ (privateEntriesCount >>> 32));
        result = 31 * result + (int) (publicEntriesCount ^ (publicEntriesCount >>> 32));
        result = 31 * result + (mUserpic != null ? mUserpic.hashCode() : 0);
        result = 31 * result + (mRelationshipsSummary != null ? mRelationshipsSummary.hashCode() : 0);
        result = 31 * result + (mDesign != null ? mDesign.hashCode() : 0);
        return result;
    }
}
