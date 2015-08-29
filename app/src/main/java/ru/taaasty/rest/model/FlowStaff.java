package ru.taaasty.rest.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by alexey on 29.08.15.
 */
public class FlowStaff implements Parcelable {

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({
            ROLE_ADMIN,
            ROLE_MODERATOR
    })
    public @interface Role {}

    public static final String ROLE_ADMIN = "admin";

    public static final String ROLE_MODERATOR = "moderator";

    public long id;

    public long flowId;

    public long userId;

    public User user = User.DUMMY;

    public @Role String role = ROLE_ADMIN;


    public FlowStaff() {
    }

    public boolean isAdmin() {
        return ROLE_ADMIN.equals(role);
    }

    public boolean isModerator() {
        return ROLE_MODERATOR.equals(role);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FlowStaff flowStaff = (FlowStaff) o;

        if (id != flowStaff.id) return false;
        if (flowId != flowStaff.flowId) return false;
        if (userId != flowStaff.userId) return false;
        if (user != null ? !user.equals(flowStaff.user) : flowStaff.user != null) return false;
        return role.equals(flowStaff.role);

    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + (int) (flowId ^ (flowId >>> 32));
        result = 31 * result + (int) (userId ^ (userId >>> 32));
        result = 31 * result + (user != null ? user.hashCode() : 0);
        result = 31 * result + role.hashCode();
        return result;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.id);
        dest.writeLong(this.flowId);
        dest.writeLong(this.userId);
        dest.writeParcelable(this.user, 0);
        dest.writeString(this.role);
    }

    protected FlowStaff(Parcel in) {
        this.id = in.readLong();
        this.flowId = in.readLong();
        this.userId = in.readLong();
        this.user = in.readParcelable(User.class.getClassLoader());
        //noinspection ResourceType
        this.role = in.readString();
    }

    public static final Parcelable.Creator<FlowStaff> CREATOR = new Parcelable.Creator<FlowStaff>() {
        public FlowStaff createFromParcel(Parcel source) {
            return new FlowStaff(source);
        }

        public FlowStaff[] newArray(int size) {
            return new FlowStaff[size];
        }
    };
}
