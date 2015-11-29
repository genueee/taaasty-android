package ru.taaasty.rest.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

/**
 * Тлог, в который написана запись.
 * Сущность встречается под ключом 'tlog' в лентах. Схема отличается от всех существующих.
 */
public class EntryTlog implements Parcelable {

    /**
     * какой-то ID
     */
    public long id;

    public String tlogUrl;

    public String tag;

    public String slug;

    /**
     * Дизайн тлога. Вроде бы всегда совпадает с author.design
     */
    public TlogDesign design;

    /**
     * Инфа о тлоге в формате юзера.
     */
    public User author;


    public boolean isFlow() {
        return author.isFlow();
    }

    @Nullable
    public String getFlowTitle() {
        if (!isFlow()) return null;
        return author.getName();
    }

    public TlogDesign getDesign() {
        return design;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.id);
        dest.writeString(this.tlogUrl);
        dest.writeString(this.tag);
        dest.writeString(this.slug);
        dest.writeParcelable(this.design, 0);
        dest.writeParcelable(this.author, 0);
    }

    public EntryTlog() {
    }

    protected EntryTlog(Parcel in) {
        this.id = in.readLong();
        this.tlogUrl = in.readString();
        this.tag = in.readString();
        this.slug = in.readString();
        this.design = in.readParcelable(TlogDesign.class.getClassLoader());
        this.author = in.readParcelable(User.class.getClassLoader());
    }

    public static final Parcelable.Creator<EntryTlog> CREATOR = new Parcelable.Creator<EntryTlog>() {
        public EntryTlog createFromParcel(Parcel source) {
            return new EntryTlog(source);
        }

        public EntryTlog[] newArray(int size) {
            return new EntryTlog[size];
        }
    };
}
