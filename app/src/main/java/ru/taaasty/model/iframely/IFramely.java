package ru.taaasty.model.iframely;

import android.support.annotation.Nullable;

/**
 * Created by alexey on 19.09.14.
 */
public class IFramely {

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
}
