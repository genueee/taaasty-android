package ru.taaasty.ui.feeds;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import com.google.android.youtube.player.YouTubeIntents;

import java.util.Date;
import java.util.List;

import ru.taaasty.BuildConfig;
import ru.taaasty.adapters.FeedItemAdapterLite;
import ru.taaasty.adapters.list.ListEmbeddEntry;
import ru.taaasty.adapters.list.ListEntryBase;
import ru.taaasty.adapters.list.ListImageEntry;
import ru.taaasty.rest.model.Entry;
import ru.taaasty.ui.photo.ShowPhotoActivity;
import ru.taaasty.utils.UiUtils;
import ru.taaasty.widgets.DateIndicatorWidget;

/**
 * Created by alexey on 04.11.14.
 */
public class FeedsHelper {

    private FeedsHelper() {

    }

    public interface IFeedsHelper {
        public Entry getAnyEntryAtHolderPosition(RecyclerView.ViewHolder holder);
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
                                           FeedItemAdapterLite adapter,
                                           boolean animScrollUp) {
        Date newDate = null;
        if (listView == null || dateIndicator == null) return;

        int position = getAdapterPositionAtWidgetHeight(listView, dateIndicator);
        if (!adapter.isPositionInFeed(position) && position != RecyclerView.NO_POSITION) {
            // Если на хидере или на футере - не показываем индикатор
            dateIndicator.setVisibility(View.INVISIBLE);
        } else {
            Entry entry = adapter.getEntry(position);
            if (entry != null) {
                newDate = entry.getCreatedAt();
                dateIndicator.setDate(newDate, animScrollUp);
            } else {
                if (BuildConfig.DEBUG) Log.v("FeedsHelper", "no entry at position" + position);

            }
        }
    }

    public static class DateIndicatorUpdateHelper {

        private final Handler mHandler;
        private final RecyclerView mListView;
        private final DateIndicatorWidget mWidget;
        private final FeedItemAdapterLite mAdapter;

        boolean mQueued = false;

        boolean mLastAnimScrollUp = true;

        public DateIndicatorUpdateHelper(RecyclerView listView,
                                         DateIndicatorWidget dateIndicator,
                                         FeedItemAdapterLite adapter) {
            mHandler = new Handler();
            mListView = listView;
            mWidget = dateIndicator;
            mAdapter = adapter;
        }

        public void onResume() {
            updateDateIndicatorDelayed(true);
        }

        public void onDestroy() {
            mHandler.removeCallbacksAndMessages(null);
            mQueued = false;
        }

        void updateDateIndicatorDelayed(boolean animScrollIUp) {
            if (mQueued) return;
            mQueued = true;
            mLastAnimScrollUp = animScrollIUp;
            mHandler.removeCallbacks(mUpdateRunnable);
            mHandler.postDelayed(mUpdateRunnable, 16 * 5);
        }

        public final RecyclerView.AdapterDataObserver adapterDataObserver = new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                updateDateIndicatorDelayed(true);
            }

            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                updateDateIndicatorDelayed(true);
            }
        };

        private final Runnable mUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                FeedsHelper.updateDateIndicator(mListView, mWidget, mAdapter, mLastAnimScrollUp);
                mQueued = false;
            }
        };

        public final RecyclerView.OnScrollListener onScrollListener = new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                updateDateIndicatorDelayed(dy > 0);
            }
        };
    }

    public static void setupListEntryClickListener(IFeedsHelper adapter, final ListEntryBase pHolder) {
        if (pHolder instanceof ListImageEntry) {
            FeedsHelper.OnImageEntryClickListener listener = new FeedsHelper.OnImageEntryClickListener(pHolder, adapter);
            ((ListImageEntry) pHolder).getImageView().setOnClickListener(listener);
            ((ListImageEntry) pHolder).getMoreImagesWidget().setOnClickListener(listener);
        } else if (pHolder instanceof  ListEmbeddEntry) {
            ((ListEmbeddEntry) pHolder).getImageView().setOnClickListener(new FeedsHelper.OnImageEntryClickListener(pHolder, adapter));
        }
    }

    public static class OnImageEntryClickListener implements View.OnClickListener {

        private final ListEntryBase mHolder;
        private final IFeedsHelper mAdapter;

        public OnImageEntryClickListener(ListEntryBase holder, IFeedsHelper adapter) {
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
                Uri uri = Uri.parse(entry.getIframely().url);
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
