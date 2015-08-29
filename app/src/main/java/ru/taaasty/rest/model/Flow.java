package ru.taaasty.rest.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.Arrays;
import java.util.Comparator;

import ru.taaasty.utils.Objects;

/**
 * Created by alexey on 29.08.15.
 */
public class Flow implements Parcelable {

    public static transient Comparator<Entry> ORDER_BY_CREATE_DATE_DESC_ID_COMARATOR = new Comparator<Entry>() {
        @Override
        public int compare(Entry lhs, Entry rhs) {
            if (lhs == null && rhs == null) {
                return 0;
            } else if (lhs == null) {
                return -1;
            } else if (rhs == null) {
                return 1;
            } else {
                int compareDates = rhs.getCreatedAt().compareTo(lhs.getCreatedAt());
                return compareDates != 0 ? compareDates : Objects.compare(rhs.getId(), lhs.getId());
            }
        }
    };


    @SerializedName("id")
    private long mId;

    @SerializedName("name")
    private String mName;

    @SerializedName("title")
    private String mTitle;

    @SerializedName("slug")
    private String mSlug;

    @SerializedName("tlog_url")
    private String mTlogUrl;

    @SerializedName("flowpic")
    private FlowPic mFlowPic;

    @SerializedName("is_privacy")
    private boolean mIsPrivacy;

    @SerializedName("is_premoderate")
    private boolean mIsPremoderate;

    @SerializedName("tag")
    private String mTag;

    @SerializedName("followers_count")
    private int mFollowersCount;

    @SerializedName("public_tlog_entries_count")
    private int mPublicTlogEntriesCount;

    @Nullable
    @SerializedName("staffs")
    private FlowStaff[] mFlowStaffs;

    public FlowPic getFlowPic() {
        return mFlowPic;
    }

    public int getFollowersCount() {
        return mFollowersCount;
    }

    public long getId() {
        return mId;
    }

    public boolean isPremoderate() {
        return mIsPremoderate;
    }

    public boolean isPrivacy() {
        return mIsPrivacy;
    }

    public String getName() {
        return mName;
    }

    public int getPublicTlogEntriesCount() {
        return mPublicTlogEntriesCount;
    }

    public String getSlug() {
        return mSlug;
    }

    public String getTag() {
        return mTag;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getTlogUrl() {
        return mTlogUrl;
    }

    @Nullable
    public FlowStaff[] getFlowStaffs() {
        return mFlowStaffs;
    }

    @Override
    public String toString() {
        return "Flow{" +
                "mFlowPic=" + mFlowPic +
                ", mId=" + mId +
                ", mName='" + mName + '\'' +
                ", mTitle='" + mTitle + '\'' +
                ", mSlug='" + mSlug + '\'' +
                ", mTlogUrl='" + mTlogUrl + '\'' +
                ", mIsPrivacy=" + mIsPrivacy +
                ", mIsPremoderate=" + mIsPremoderate +
                ", mTag='" + mTag + '\'' +
                ", mFollowersCount=" + mFollowersCount +
                ", mPublicTlogEntriesCount=" + mPublicTlogEntriesCount +
                ", mFlowStaffs=" + Arrays.toString(mFlowStaffs) +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Flow flow = (Flow) o;

        if (mId != flow.mId) return false;
        if (mIsPrivacy != flow.mIsPrivacy) return false;
        if (mIsPremoderate != flow.mIsPremoderate) return false;
        if (mFollowersCount != flow.mFollowersCount) return false;
        if (mPublicTlogEntriesCount != flow.mPublicTlogEntriesCount) return false;
        if (mName != null ? !mName.equals(flow.mName) : flow.mName != null) return false;
        if (mTitle != null ? !mTitle.equals(flow.mTitle) : flow.mTitle != null) return false;
        if (mSlug != null ? !mSlug.equals(flow.mSlug) : flow.mSlug != null) return false;
        if (mTlogUrl != null ? !mTlogUrl.equals(flow.mTlogUrl) : flow.mTlogUrl != null)
            return false;
        if (mFlowPic != null ? !mFlowPic.equals(flow.mFlowPic) : flow.mFlowPic != null)
            return false;
        if (mTag != null ? !mTag.equals(flow.mTag) : flow.mTag != null) return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        return Arrays.equals(mFlowStaffs, flow.mFlowStaffs);

    }

    @Override
    public int hashCode() {
        int result = (int) (mId ^ (mId >>> 32));
        result = 31 * result + (mName != null ? mName.hashCode() : 0);
        result = 31 * result + (mTitle != null ? mTitle.hashCode() : 0);
        result = 31 * result + (mSlug != null ? mSlug.hashCode() : 0);
        result = 31 * result + (mTlogUrl != null ? mTlogUrl.hashCode() : 0);
        result = 31 * result + (mFlowPic != null ? mFlowPic.hashCode() : 0);
        result = 31 * result + (mIsPrivacy ? 1 : 0);
        result = 31 * result + (mIsPremoderate ? 1 : 0);
        result = 31 * result + (mTag != null ? mTag.hashCode() : 0);
        result = 31 * result + mFollowersCount;
        result = 31 * result + mPublicTlogEntriesCount;
        result = 31 * result + (mFlowStaffs != null ? Arrays.hashCode(mFlowStaffs) : 0);
        return result;
    }

    public static class FlowPic implements Parcelable {
        public String originalUrl;

        public String thumborPath;


        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(this.originalUrl);
            dest.writeString(this.thumborPath);
        }

        public FlowPic() {
        }

        protected FlowPic(Parcel in) {
            this.originalUrl = in.readString();
            this.thumborPath = in.readString();
        }

        public static final Parcelable.Creator<FlowPic> CREATOR = new Parcelable.Creator<FlowPic>() {
            public FlowPic createFromParcel(Parcel source) {
                return new FlowPic(source);
            }

            public FlowPic[] newArray(int size) {
                return new FlowPic[size];
            }
        };

        @Override
        public String toString() {
            return "FlowPic{" +
                    "originalUrl='" + originalUrl + '\'' +
                    ", thumborPath='" + thumborPath + '\'' +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            FlowPic flowPic = (FlowPic) o;

            if (originalUrl != null ? !originalUrl.equals(flowPic.originalUrl) : flowPic.originalUrl != null)
                return false;
            return !(thumborPath != null ? !thumborPath.equals(flowPic.thumborPath) : flowPic.thumborPath != null);

        }

        @Override
        public int hashCode() {
            int result = originalUrl != null ? originalUrl.hashCode() : 0;
            result = 31 * result + (thumborPath != null ? thumborPath.hashCode() : 0);
            return result;
        }
    }

    public Flow() {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.mId);
        dest.writeString(this.mName);
        dest.writeString(this.mTitle);
        dest.writeString(this.mSlug);
        dest.writeString(this.mTlogUrl);
        dest.writeParcelable(this.mFlowPic, 0);
        dest.writeByte(mIsPrivacy ? (byte) 1 : (byte) 0);
        dest.writeByte(mIsPremoderate ? (byte) 1 : (byte) 0);
        dest.writeString(this.mTag);
        dest.writeInt(this.mFollowersCount);
        dest.writeInt(this.mPublicTlogEntriesCount);
        dest.writeParcelableArray(this.mFlowStaffs, 0);
    }

    protected Flow(Parcel in) {
        this.mId = in.readLong();
        this.mName = in.readString();
        this.mTitle = in.readString();
        this.mSlug = in.readString();
        this.mTlogUrl = in.readString();
        this.mFlowPic = in.readParcelable(FlowPic.class.getClassLoader());
        this.mIsPrivacy = in.readByte() != 0;
        this.mIsPremoderate = in.readByte() != 0;
        this.mTag = in.readString();
        this.mFollowersCount = in.readInt();
        this.mPublicTlogEntriesCount = in.readInt();
        this.mFlowStaffs = (FlowStaff[]) in.readParcelableArray(FlowStaff.class.getClassLoader());
    }

    public static final Creator<Flow> CREATOR = new Creator<Flow>() {
        public Flow createFromParcel(Parcel source) {
            return new Flow(source);
        }

        public Flow[] newArray(int size) {
            return new Flow[size];
        }
    };
}
