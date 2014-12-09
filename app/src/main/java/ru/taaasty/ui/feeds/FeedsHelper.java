package ru.taaasty.ui.feeds;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.google.android.youtube.player.YouTubeIntents;

import java.util.Date;
import java.util.List;

import ru.taaasty.adapters.FeedItemAdapter;
import ru.taaasty.adapters.FeedItemAdapterLite;
import ru.taaasty.adapters.list.ListEmbeddEntry;
import ru.taaasty.adapters.list.ListEntryBase;
import ru.taaasty.adapters.list.ListImageEntry;
import ru.taaasty.model.Entry;
import ru.taaasty.model.iframely.Link;
import ru.taaasty.ui.photo.ShowPhotoActivity;
import ru.taaasty.utils.UiUtils;
import ru.taaasty.widgets.DateIndicatorWidget;

/**
 * Created by alexey on 04.11.14.
 */
public class FeedsHelper {

    private FeedsHelper() {

    }

    @Nullable
    public static int getAdapterPositionAtWidgetHeight(RecyclerView listView, View widget) {
        // Здесь считаем, что top listview и top parent'а dateindicator находятся на одной высоте
        int triggerTop = widget.getTop() + widget.getHeight() / 2;
        // int triggerTop = mListView.getTop() + mListView.getHeight() / 2;
        View child = null;
        // Ищем ближайшую запись
        RecyclerView.LayoutManager lm = listView.getLayoutManager();
        final int count = lm.getChildCount();
        for (int i = count - 1; i >= 0; i--) {
            final View checked = lm.getChildAt(i);
            if (triggerTop >= checked.getTop()) {
                child = checked;
                break;
            }
        }

        return child == null ? RecyclerView.NO_POSITION : listView.getChildPosition(child);
    }

    public static void updateDateIndicator(RecyclerView listView,
                                           DateIndicatorWidget dateIndicator,
                                           FeedItemAdapter adapter,
                                           boolean animScrollUp) {
        Date newDate = null;
        if (listView == null || dateIndicator == null) return;

        int position = getAdapterPositionAtWidgetHeight(listView, dateIndicator);
        if (position != RecyclerView.NO_POSITION) {
            Entry entry = adapter.getAnyEntryAtPosition(position);
            if (entry != null) newDate = entry.getCreatedAt();
        }
        dateIndicator.setDate(newDate, animScrollUp);
    }

    public static void updateDateIndicator(RecyclerView listView,
                                           DateIndicatorWidget dateIndicator,
                                           FeedItemAdapterLite adapter,
                                           boolean animScrollUp) {
        Date newDate = null;
        if (listView == null || dateIndicator == null) return;

        int position = getAdapterPositionAtWidgetHeight(listView, dateIndicator);
        Entry entry = adapter.getEntry(position);
        if (entry != null) newDate = entry.getCreatedAt();
        dateIndicator.setDate(newDate, animScrollUp);
    }

    public static void setupListEntryClickListener(FeedItemAdapterLite adapter, final ListEntryBase pHolder) {
        if (pHolder instanceof ListImageEntry) {
            ((ListImageEntry) pHolder).getImageView().setOnClickListener(
                    new FeedsHelper.OnImageEntryClickListener(pHolder, adapter));
        } else if (pHolder instanceof  ListEmbeddEntry) {
            ((ListEmbeddEntry) pHolder).getImageView().setOnClickListener(new FeedsHelper.OnImageEntryClickListener(pHolder, adapter));
        }
    }

    public static class OnImageEntryClickListener implements View.OnClickListener {

        private final ListEntryBase mHolder;
        private final FeedItemAdapterLite mAdapter;

        public OnImageEntryClickListener(ListEntryBase holder, FeedItemAdapterLite adapter) {
            mHolder = holder;
            mAdapter = adapter;
        }

        @Override
        public void onClick(View view) {
            Entry entry = mAdapter.getAnyEntryAtHolderPosition(mHolder);
            if (entry == null) return;
            if (mHolder instanceof  ListImageEntry) {
                onImageEntryClick(entry, view);
            } else if (mHolder instanceof ListEmbeddEntry) {
                onEmbeddEntryClick(entry, view);
            }
        }

        private void onImageEntryClick(Entry entry, View view) {
            if (ShowPhotoActivity.canShowEntry(entry)) {
                ShowPhotoActivity.startShowPhotoActivity(mHolder.itemView.getContext(), entry,
                        ((ListImageEntry)mHolder).getImageViewUrl(), view);
            }
        }

        private void onEmbeddEntryClick(Entry entry, View view) {
            Context context = view.getContext();
            if (entry.isYoutubeVideo() && YouTubeIntents.canResolvePlayVideoIntent(context)) {
                final String youtubeId = UiUtils.parseYoutubeVideoId(entry.getIframely().url);
                Intent intent = YouTubeIntents.createPlayVideoIntentWithOptions(context, youtubeId,
                        true, false);
                context.startActivity(intent);
                return;
            }

            if (entry.isEmbedd()) {
                Link link = entry.getIframely().getHtmlLink();
                if (link == null) {
                    List<Link> links = entry.getIframely().links.getMergedList();
                    if (links.isEmpty()) {
                        return;
                    } else {
                        link = links.get(0);
                    }
                }
                Uri uri = Uri.parse(link.getHref());
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                PackageManager manager = context.getPackageManager();
                List<ResolveInfo> infos = manager.queryIntentActivities(intent, 0);
                if (infos.size() > 0) {
                    context.startActivity(intent);
                }
            }
        }
    }
}
