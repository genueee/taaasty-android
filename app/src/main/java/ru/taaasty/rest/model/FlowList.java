package ru.taaasty.rest.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

/**
 * Created by alexey on 29.08.15.
 */
public class FlowList {

    public FlowSubList[] items;

    public long totalCount;

    public int currentPage = 1;

    public int totalPages = 1;

    public int nextPage;

    public boolean hasMore;

    public int limit;

    public static class FlowSubList implements Parcelable {

        public Flow flow;

        /**
         * Отнощение. Может не быть, если, например, пользователь не залогинен
         */
        @Nullable
        public Relationship relationship;

        public boolean isMeSubscribed() {
            return relationship != null &&
                    Relationship.isMeSubscribed(relationship.getState());
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeParcelable(this.flow, 0);
            dest.writeParcelable(this.relationship, 0);
        }

        public FlowSubList() {
        }

        protected FlowSubList(Parcel in) {
            this.flow = in.readParcelable(Flow.class.getClassLoader());
            this.relationship = in.readParcelable(Relationship.class.getClassLoader());
        }

        public static final Parcelable.Creator<FlowSubList> CREATOR = new Parcelable.Creator<FlowSubList>() {
            public FlowSubList createFromParcel(Parcel source) {
                return new FlowSubList(source);
            }

            public FlowSubList[] newArray(int size) {
                return new FlowSubList[size];
            }
        };
    }
}
