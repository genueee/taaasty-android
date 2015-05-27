package ru.taaasty.rest.model.iframely;


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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Meta meta = (Meta) o;

        if (title != null ? !title.equals(meta.title) : meta.title != null) return false;
        if (description != null ? !description.equals(meta.description) : meta.description != null)
            return false;
        if (canonical != null ? !canonical.equals(meta.canonical) : meta.canonical != null)
            return false;
        if (shortlink != null ? !shortlink.equals(meta.shortlink) : meta.shortlink != null)
            return false;
        if (category != null ? !category.equals(meta.category) : meta.category != null)
            return false;
        if (keywords != null ? !keywords.equals(meta.keywords) : meta.keywords != null)
            return false;
        if (author != null ? !author.equals(meta.author) : meta.author != null) return false;
        if (author_url != null ? !author_url.equals(meta.author_url) : meta.author_url != null)
            return false;
        if (site != null ? !site.equals(meta.site) : meta.site != null) return false;
        return !(duration != null ? !duration.equals(meta.duration) : meta.duration != null);

    }

    @Override
    public int hashCode() {
        int result = title != null ? title.hashCode() : 0;
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (canonical != null ? canonical.hashCode() : 0);
        result = 31 * result + (shortlink != null ? shortlink.hashCode() : 0);
        result = 31 * result + (category != null ? category.hashCode() : 0);
        result = 31 * result + (keywords != null ? keywords.hashCode() : 0);
        result = 31 * result + (author != null ? author.hashCode() : 0);
        result = 31 * result + (author_url != null ? author_url.hashCode() : 0);
        result = 31 * result + (site != null ? site.hashCode() : 0);
        result = 31 * result + (duration != null ? duration.hashCode() : 0);
        return result;
    }
}
