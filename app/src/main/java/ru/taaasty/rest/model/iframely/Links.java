package ru.taaasty.rest.model.iframely;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * <a href="https://iframely.com/docs/links">https://iframely.com/docs/links</a>
 */
public class Links implements Parcelable, Iterable<Link> {

    /**
     * widget with media playback. Like video or music or slideshow players
     */
    public List<Link> player = Collections.emptyList();

    /**
     * the preview image, usually smaller size
     */
    public List<Link> thumbnail = Collections.emptyList();

    /**
     * sizeable photo or picture, indicating that this is the main content on the web page. For use in e.g. photo albums "details" page
     */
    public List<Link> image = Collections.emptyList();

    /**
     * general extract for an app. For example, Twitter statuses, Facebook posts, Instagrams, etc;
     */
    public List<Link> app = Collections.emptyList();

    /**
     * text or graphical widget intended for reader functionality (e.g. article).
     */
    public List<Link> reader = Collections.emptyList();

    /**
     * the widget is a questionnaire;
     */
    public List<Link> survey = Collections.emptyList();

    /**
     * downloadable file. We return this rel for all direct links to files, including image files;
     */
    public List<Link> file = Collections.emptyList();

    /**
     * attribution favicon or glyph;
     */
    public List<Link> icon = Collections.emptyList();

    /**
     * logo the source site. Is returned mostly for pages with the news article (custom ones) for better attribution.
     */
    public List<Link> logo = Collections.emptyList();


    public List<Link> other = Collections.emptyList();


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(player);
        dest.writeTypedList(thumbnail);
        dest.writeTypedList(image);
        dest.writeTypedList(app);
        dest.writeTypedList(reader);
        dest.writeTypedList(survey);
        dest.writeTypedList(file);
        dest.writeTypedList(icon);
        dest.writeTypedList(logo);
        dest.writeTypedList(other);
    }

    public Links() {
    }

    private Links(Parcel in) {
        player = in.createTypedArrayList(Link.CREATOR);
        thumbnail = in.createTypedArrayList(Link.CREATOR);
        image = in.createTypedArrayList(Link.CREATOR);
        app = in.createTypedArrayList(Link.CREATOR);
        reader = in.createTypedArrayList( Link.CREATOR);
        survey = in.createTypedArrayList(Link.CREATOR);
        file = in.createTypedArrayList(Link.CREATOR);
        icon = in.createTypedArrayList(Link.CREATOR);
        logo = in.createTypedArrayList(Link.CREATOR);
        other = in.createTypedArrayList(Link.CREATOR);
    }

    public List<Link> getMergedList() {
        List<Link> items = new ArrayList<>();
        if (image != null ) items.addAll(image);
        if (thumbnail != null) items.addAll(thumbnail);
        if (logo != null) items.addAll(logo);
        if (icon != null) items.addAll(icon);
        if (app != null) items.addAll(app);
        if (player != null) items.addAll(player);
        if (reader != null) items.addAll(reader);
        if (survey != null) items.addAll(survey);
        if (file != null) items.addAll(file);
        if (other != null) items.addAll(other);
        return items;
    }

    @Override
    public Iterator<Link> iterator() {
        return getMergedList().iterator();
    }

    public static final Parcelable.Creator<Links> CREATOR = new Parcelable.Creator<Links>() {
        public Links createFromParcel(Parcel source) {
            return new Links(source);
        }

        public Links[] newArray(int size) {
            return new Links[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Links links = (Links) o;

        if (player != null ? !player.equals(links.player) : links.player != null) return false;
        if (thumbnail != null ? !thumbnail.equals(links.thumbnail) : links.thumbnail != null)
            return false;
        if (image != null ? !image.equals(links.image) : links.image != null) return false;
        if (app != null ? !app.equals(links.app) : links.app != null) return false;
        if (reader != null ? !reader.equals(links.reader) : links.reader != null) return false;
        if (survey != null ? !survey.equals(links.survey) : links.survey != null) return false;
        if (file != null ? !file.equals(links.file) : links.file != null) return false;
        if (icon != null ? !icon.equals(links.icon) : links.icon != null) return false;
        if (logo != null ? !logo.equals(links.logo) : links.logo != null) return false;
        return !(other != null ? !other.equals(links.other) : links.other != null);

    }

    @Override
    public int hashCode() {
        int result = player != null ? player.hashCode() : 0;
        result = 31 * result + (thumbnail != null ? thumbnail.hashCode() : 0);
        result = 31 * result + (image != null ? image.hashCode() : 0);
        result = 31 * result + (app != null ? app.hashCode() : 0);
        result = 31 * result + (reader != null ? reader.hashCode() : 0);
        result = 31 * result + (survey != null ? survey.hashCode() : 0);
        result = 31 * result + (file != null ? file.hashCode() : 0);
        result = 31 * result + (icon != null ? icon.hashCode() : 0);
        result = 31 * result + (logo != null ? logo.hashCode() : 0);
        result = 31 * result + (other != null ? other.hashCode() : 0);
        return result;
    }
}
