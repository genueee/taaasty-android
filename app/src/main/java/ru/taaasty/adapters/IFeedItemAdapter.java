package ru.taaasty.adapters;

import java.util.List;

import ru.taaasty.model.Entry;
import ru.taaasty.model.TlogDesign;

/**
 * Created by alexey on 12.08.14.
 */
public interface  IFeedItemAdapter {

    public void setFeed(List<Entry> feed);

    public void setFeedDesign(TlogDesign design);

    public void onUpdateRatingStart(long entryId);

    public void onUpdateRatingEnd(long entryId);

    public boolean isRatingInUpdate(long entryId);

    public void updateEntry(Entry entry);

}
