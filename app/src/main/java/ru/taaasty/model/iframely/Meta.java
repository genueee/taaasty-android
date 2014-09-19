package ru.taaasty.model.iframely;


import android.os.Parcel;
import android.os.Parcelable;

/**
 * <a href="https://iframely.com/docs/meta">https://iframely.com/docs/meta</a>
 */
public class Meta implements Parcelable {

    public static final Meta DUMMY = new Meta();

    public String title = "";

    public String description = "";

    /**
     * canonical URL of the resource
     */
    public String canonical = "";

    /**
     * URL shortened through publisher
     */
    public String shortlink = "";


    public String category = "";

    public String keywords = "";


    /**
     * Attribution
     */

    public String author = "";

    public String author_url = "";

    public String site = "";


    /**
     * Stats
     */

    public Long duration;


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.title);
        dest.writeString(this.description);
        dest.writeString(this.canonical);
        dest.writeString(this.shortlink);
        dest.writeString(this.category);
        dest.writeString(this.keywords);
        dest.writeString(this.author);
        dest.writeString(this.author_url);
        dest.writeString(this.site);
        dest.writeValue(this.duration);
    }

    public Meta() {
    }

    private Meta(Parcel in) {
        this.title = in.readString();
        this.description = in.readString();
        this.canonical = in.readString();
        this.shortlink = in.readString();
        this.category = in.readString();
        this.keywords = in.readString();
        this.author = in.readString();
        this.author_url = in.readString();
        this.site = in.readString();
        this.duration = (Long) in.readValue(Long.class.getClassLoader());
    }

    public static final Parcelable.Creator<Meta> CREATOR = new Parcelable.Creator<Meta>() {
        public Meta createFromParcel(Parcel source) {
            return new Meta(source);
        }

        public Meta[] newArray(int size) {
            return new Meta[size];
        }
    };

    @Override
    public String toString() {
        return "Meta{" +
                "title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", canonical='" + canonical + '\'' +
                ", shortlink='" + shortlink + '\'' +
                ", category='" + category + '\'' +
                ", keywords='" + keywords + '\'' +
                ", author='" + author + '\'' +
                ", author_url='" + author_url + '\'' +
                ", site='" + site + '\'' +
                ", duration=" + duration +
                '}';
    }
}
