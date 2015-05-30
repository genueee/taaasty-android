package ru.taaasty.rest.model.iframely;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

/**
 * Created by alexey on 19.09.14.
 */
public class IFramely implements Parcelable {

    /**
     *  short ID of a link
     *  Example: "ACcM3Y"
     */
    public String id;

    /**
     * "http://coub.com/view/2pc24rpb"
     */
    public String url;

    public Meta meta = Meta.DUMMY;

    public Links links;

    public String html = "";

    /**
     * @return Первая попавшаяся ссылка - изображение
     */
    @Nullable
    public Link getImageLink() {
        for (Link l: links) if (l.isImage()) return l;
        return null;
    }

    /**
     * @return Первая попавшаяся ссылка - text/html
     */
    @Nullable
    public Link getHtmlLink() {
        for (Link l: links) if (l.isTextHtml()) return l;
        return null;
    }

    /**
     * Ссылка - изображение, по ширине ближе всего к dstWidth
     * @param dstWidth Ширина, под которую искать изображение
     * @return Ссылка наа изображение, либо null
     */
    @Nullable
    public Link getImageLink(int dstWidth) {
        Link bestLink = null;
        for (Link l: links) {
            if (l.isImage()) {
                if (isBetterWidth(l, bestLink, dstWidth)) bestLink = l;
            }
        }
        return bestLink;
    }

    /**
     * @return основной контент - картинка. Поверх обычно не нужно показывать кнопку play
     */
    public boolean isContentLooksLikeImage() {
        return !links.image.isEmpty();
    }

    private boolean isBetterWidth(@Nullable Link link, @Nullable Link currentBestLink, int dstWidth) {
        if (currentBestLink == null) return true;
        if (link == null) return false;
        return Math.abs(link.media.width - dstWidth) < Math.abs(link.media.width - dstWidth);
    }

    @Override
    public String toString() {
        return "IFramely{" +
                "id='" + id + '\'' +
                ", url='" + url + '\'' +
                ", meta=" + meta +
                ", links=" + links +
                ", html='" + html + '\'' +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.id);
        dest.writeString(this.url);
        dest.writeParcelable(this.meta, flags);
        dest.writeParcelable(this.links, flags);
        dest.writeString(this.html);
    }

    public IFramely() {
    }

    private IFramely(Parcel in) {
        this.id = in.readString();
        this.url = in.readString();
        this.meta = in.readParcelable(Meta.class.getClassLoader());
        this.links = in.readParcelable(Links.class.getClassLoader());
        this.html = in.readString();
    }

    public static final Parcelable.Creator<IFramely> CREATOR = new Parcelable.Creator<IFramely>() {
        public IFramely createFromParcel(Parcel source) {
            return new IFramely(source);
        }

        public IFramely[] newArray(int size) {
            return new IFramely[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IFramely iFramely = (IFramely) o;

        if (id != null ? !id.equals(iFramely.id) : iFramely.id != null) return false;
        if (url != null ? !url.equals(iFramely.url) : iFramely.url != null) return false;
        if (meta != null ? !meta.equals(iFramely.meta) : iFramely.meta != null) return false;
        if (links != null ? !links.equals(iFramely.links) : iFramely.links != null) return false;
        return !(html != null ? !html.equals(iFramely.html) : iFramely.html != null);

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (url != null ? url.hashCode() : 0);
        result = 31 * result + (meta != null ? meta.hashCode() : 0);
        result = 31 * result + (links != null ? links.hashCode() : 0);
        result = 31 * result + (html != null ? html.hashCode() : 0);
        return result;
    }
}
