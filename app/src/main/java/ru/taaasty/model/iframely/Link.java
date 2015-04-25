package ru.taaasty.model.iframely;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Collections;
import java.util.List;

/**
 * https://iframely.com/docs/links
 */
public class Link implements Parcelable {

    /**
     * supplementary rels: autoplay, html5, inline,
     */
    public List<String> rel = Collections.emptyList();

    /**
     * SRC of embed. The main attribute
     *
     */
    String href = "";

    /**
     * MIME type:
     * "text/html",
     * "application/javascript",
     * "application/x-shockwave-flash",
     * "video/mp4",
     * "image", "image/*"
     */
    public String type = "text/html";

    /**
     * Media query. Mostly responsive
     */
    public Media media = Media.DUMMY;

    /**
     * @return true если линк - изображение, с проставленными width и height
     */
    public boolean isImage() {
        return media.width > 0 && media.height > 0 && type.startsWith("image");
    }

    public boolean isTextHtml() {
        return "text/html".equals(type);
    }

    public String getHref() {
        if (href == null) return "";
        if (href.startsWith("//")) {
            // Прикол soundcloud'а. Ссылка валидная, но у нас, вроде, не поддерживается.
            return "http:" + href;
        }
        return href;
    }


    public Link() {
    }

    private Link(Parcel in) {
        this.media = in.readParcelable(Media.class.getClassLoader());
        this.href = in.readString();
        this.rel = in.createStringArrayList();
        this.type = in.readString();
    }

    public static final Parcelable.Creator<Link> CREATOR = new Parcelable.Creator<Link>() {
        public Link createFromParcel(Parcel source) {
            return new Link(source);
        }

        public Link[] newArray(int size) {
            return new Link[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.media, 0);
        dest.writeString(this.href);
        dest.writeStringList(this.rel);
        dest.writeString(this.type);
    }

    @Override
    public String toString() {
        return "Link{" +
                "media=" + media +
                ", href='" + href + '\'' +
                ", rel=" + rel +
                ", type='" + type + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Link link = (Link) o;

        if (rel != null ? !rel.equals(link.rel) : link.rel != null) return false;
        if (href != null ? !href.equals(link.href) : link.href != null) return false;
        if (type != null ? !type.equals(link.type) : link.type != null) return false;
        return !(media != null ? !media.equals(link.media) : link.media != null);

    }

    @Override
    public int hashCode() {
        int result = rel != null ? rel.hashCode() : 0;
        result = 31 * result + (href != null ? href.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (media != null ? media.hashCode() : 0);
        return result;
    }
}
