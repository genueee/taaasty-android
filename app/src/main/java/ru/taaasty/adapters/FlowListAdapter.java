package ru.taaasty.adapters;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;
import com.squareup.pollexor.ThumborUrlBuilder;

import java.util.Locale;

import pl.droidsonroids.gif.GifDrawable;
import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.rest.model.Flow;
import ru.taaasty.rest.model.Relationship;
import ru.taaasty.utils.ImageUtils;
import ru.taaasty.utils.NetworkUtils;
import ru.taaasty.utils.UiUtils;
import ru.taaasty.widgets.MyRecyclerView;

public class FlowListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "FlowListAdapter";

    public static final int VIEW_TYPE_HEADER = R.id.feed_view_type_header;

    public static final int VIEW_TYPE_ITEM = R.id.view_type_item;

    public static final int VIEW_TYPE_PENDING_INDICATOR = R.id.view_type_pending_indicator;

    private static final int HEADERS_COUNT = 1;

    private final Picasso mPicasso;
    private final InteractionListener mListener;

    private final FlowListManaged mFlowList;

    private boolean mPendingIndicatorShown;

    private final Drawable mEntriesCountDrawableNotSubscribed;
    private final Drawable mEntriesCountDrawableSubscribed;

    private final Drawable mSubscribersCountDrawableNotSubscribed;
    private final Drawable mSubscribersCountDrawableSubscribed;

    private final int mTextColorSecondaryNotSubscribed;

    private final int mTextColorSecondarySubscribed;

    private final int mTextColorPrimaryNotSubscribed;

    private final int mTextColorPrimarySubscribed;

    private final OkHttpClient mOkHttpClient = NetworkUtils.getInstance().getOkHttpClient();

    public FlowListAdapter(Context context, InteractionListener listener, FlowListManaged flowList) {
        mPendingIndicatorShown = false;
        mListener = listener;
        mPicasso = Picasso.with(context);
        mFlowList = flowList;

        Resources resources = context.getResources();

        mTextColorPrimarySubscribed = resources.getColor(R.color.flow_brick_text_color_primary_subscribed);
        mTextColorPrimaryNotSubscribed = resources.getColor(R.color.flow_brick_text_color_primary_not_subscribed);

        mTextColorSecondaryNotSubscribed = resources.getColor(R.color.flow_brick_text_color_secondary_not_subscribed);
        mTextColorSecondarySubscribed = resources.getColor(R.color.flow_brick_text_color_secondary_subscribed);

        mEntriesCountDrawableNotSubscribed = DrawableCompat.wrap(resources.getDrawable(R.drawable.ic_posts_count_20dp).mutate());
        DrawableCompat.setTint(mEntriesCountDrawableNotSubscribed, mTextColorSecondaryNotSubscribed);

        mEntriesCountDrawableSubscribed = DrawableCompat.wrap(resources.getDrawable(R.drawable.ic_posts_count_20dp).mutate());
        DrawableCompat.setTint(mEntriesCountDrawableSubscribed, mTextColorSecondarySubscribed);

        mSubscribersCountDrawableNotSubscribed = DrawableCompat.wrap(resources.getDrawable(R.drawable.ic_subscribers_count_20dp).mutate());
        DrawableCompat.setTint(mSubscribersCountDrawableNotSubscribed, mTextColorSecondaryNotSubscribed);

        mSubscribersCountDrawableSubscribed = DrawableCompat.wrap(resources.getDrawable(R.drawable.ic_subscribers_count_20dp).mutate());
        DrawableCompat.setTint(mSubscribersCountDrawableSubscribed, mTextColorSecondarySubscribed);

        setHasStableIds(true);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View root;
        final RecyclerView.ViewHolder holder;

        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case VIEW_TYPE_HEADER:
                holder =  mListener.onCreateHeaderViewHolder(parent);
                break;
            case VIEW_TYPE_PENDING_INDICATOR:
                root = inflater.inflate(R.layout.endless_loading_indicator, parent, false);
                holder = new ViewHolderPendingIndicator(root);
                break;
            case VIEW_TYPE_ITEM:
                root = inflater.inflate(R.layout.flow_list_item, parent, false);
                holder = new ViewHolderItem(root);
                break;
            default:
                throw new IllegalStateException();
        }

        mListener.initClickListeners(holder, viewType);

        return holder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (isHeaderPosition(position)) {
            mListener.onBindHeaderViewHolder(holder);
            return;
        } else if (isPendingIndicatorPosition(position)) {
            return;
        }

        int feedLocation = getFeedLocation(position);
        Flow flow = mFlowList.getFlow(feedLocation);
        Relationship relationship = mFlowList.getRelationship(feedLocation);

        bindFlow((ViewHolderItem) holder, flow, relationship);

        mListener.onBindFlow((ViewHolderItem) holder, flow, relationship, feedLocation);
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);
        onViewDetachedFromWindow(holder);
        if (holder instanceof ViewHolderItem) {
            stopImageLoading((ViewHolderItem) holder);
        }
    }

    @Override
    public long getItemId(int position) {
        if (position == 0 || position > mFlowList.size()) {
            return RecyclerView.NO_ID;
        } else {
            return mFlowList.getFlow(getFeedLocation(position)).getId();
        }
    }

    @Override
    public int getItemCount() {
        int size = mFlowList.size() + HEADERS_COUNT;
        if (mPendingIndicatorShown) size += 1;
        return size;
    }

    @Override
    public int getItemViewType(int position) {
        if (isHeaderPosition(position)) {
            return VIEW_TYPE_HEADER;
        } else if (isPendingIndicatorPosition(position)) {
            return VIEW_TYPE_PENDING_INDICATOR;
        }

        return VIEW_TYPE_ITEM;
    }

    @Override
    public void onViewAttachedToWindow(RecyclerView.ViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        if (holder instanceof IParallaxedHeaderHolder) {
            holder.itemView.getViewTreeObserver().addOnScrollChangedListener((IParallaxedHeaderHolder) holder);
        }
    }

    @Override
    public void onViewDetachedFromWindow(RecyclerView.ViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        if (holder instanceof IParallaxedHeaderHolder && holder.itemView.getViewTreeObserver().isAlive()) {
            holder.itemView.getViewTreeObserver().removeOnScrollChangedListener((IParallaxedHeaderHolder) holder);
        }
    }

    public void onDestroy(RecyclerView parent) {
        // Особенности FragmentStatePagerAdapter.
        // onViewDetachedFromWindow может не вызываться при удалении активности и адаптер и фрагмент висят в памяти
        if (parent != null) {
            for (int i = 0; i < parent.getChildCount(); ++i) {
                RecyclerView.ViewHolder vh = parent.getChildViewHolder(parent.getChildAt(i));
                onViewDetachedFromWindow(vh);
            }
        }
    }

    @Nullable
    public Flow getFlow(int position) {
        if (!isPositionInFeed(position)) return null;
        return mFlowList.getFlow(getFeedLocation(position));
    }

    @Nullable
    public Relationship getRelationship(int position) {
        if (!isPositionInFeed(position)) return null;
        return mFlowList.getRelationship(getFeedLocation(position));
    }

    public void setShowPendingIndicator(boolean newValue) {
        if (newValue == mPendingIndicatorShown) return;
        mPendingIndicatorShown = newValue;
        if (newValue) {
            notifyItemInserted(getPendingIndicatorPosition());
        } else {
            notifyItemRemoved(getPendingIndicatorPosition());
        }
    }

    public static int getAdapterPosition(int listLocation) {
        return listLocation + HEADERS_COUNT;
    }

    public static int getFeedLocation(int adapterPosition) {
        return adapterPosition - HEADERS_COUNT;
    }

    public boolean isHeaderPosition(int position) {
        return position == 0;
    }

    public boolean isPositionInFeed(int position) {
        return position != RecyclerView.NO_POSITION
                && !isHeaderPosition(position)
                && !isPendingIndicatorPosition(position);
    }

    private int getPendingIndicatorPosition() {
        return mFlowList.size() + HEADERS_COUNT;
    }

    private boolean isPendingIndicatorPosition(int position) {
        return mPendingIndicatorShown && position == getPendingIndicatorPosition();
    }

    private void bindFlow(ViewHolderItem holder, Flow flow, @Nullable Relationship relationship) {
        holder.name.setText(UiUtils.capitalize(flow.getName()));
        holder.description.setText(flow.getTitle());
        holder.entriesCount.setText(String.valueOf(flow.getPublicTlogEntriesCount()));
        holder.subscribersCount.setText(String.valueOf(flow.getFollowersCount()));
        bindImage(holder, flow, relationship);
        bindStyle(holder, relationship);
    }

    private void bindStyle(ViewHolderItem holder, @Nullable Relationship relationship) {
        boolean meSubscribed = Relationship.isMeSubscribed(relationship == null ? null: relationship.getState());
        int textColorPrimary = meSubscribed ? mTextColorPrimarySubscribed : mTextColorPrimaryNotSubscribed;
        int textColorSecondary = meSubscribed ? mTextColorSecondarySubscribed : mTextColorSecondaryNotSubscribed;

        holder.descriptionContainer.setBackgroundResource(meSubscribed ?
                        R.color.flow_brick_description_background_subscribed :R.color.flow_brick_description_background_not_subscribed);

        holder.name.setTextColor(textColorPrimary);
        holder.description.setTextColor(textColorSecondary);
        holder.entriesCount.setTextColor(textColorSecondary);
        holder.subscribersCount.setTextColor(textColorSecondary);

        holder.entriesCount.setCompoundDrawablesWithIntrinsicBounds(
                meSubscribed ? mEntriesCountDrawableSubscribed : mEntriesCountDrawableNotSubscribed,
                null, null, null);

        holder.subscribersCount.setCompoundDrawablesWithIntrinsicBounds(
                meSubscribed ? mSubscribersCountDrawableSubscribed : mSubscribersCountDrawableNotSubscribed,
                null, null, null);
    }

    private void bindImage(final ViewHolderItem holder, final Flow flow, @Nullable final Relationship relationship) {
        if (holder.image.getWidth() > 0) {
            bindImageAfterSizeKnown(holder, flow, relationship);
        } else {
            holder.image.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    if (holder.image.getViewTreeObserver().isAlive()) {
                        holder.image.getViewTreeObserver().removeOnPreDrawListener(this);
                        bindImageAfterSizeKnown(holder, flow, relationship);
                    }
                    return false;
                }
            });
        }
    }

    private void bindImageAfterSizeKnown(ViewHolderItem holder, Flow flow, Relationship relationship) {
        stopImageLoading(holder);
        Flow.FlowPic flowPic = flow.getFlowPic();
        if ((flowPic != null) && !TextUtils.isEmpty(flowPic.thumborPath)) {
            bindImageByThumborPath(holder, relationship, flowPic.thumborPath);
        } else if ((flowPic != null) && !TextUtils.isEmpty(flowPic.originalUrl)) {
            bindImageByUrl(holder, relationship, flowPic.originalUrl, true);
        } else {
            holder.imageViewUrl = null;
            holder.image.setImageDrawable(null);
        }
    }

    private void bindImageByThumborPath(ViewHolderItem holder, Relationship relationship, String thumborPath) {
        String imageUrl = NetworkUtils.createThumborUrlFromPath(thumborPath)
                .resize(Math.min(holder.image.getWidth(), ImageUtils.getMaxTextureSize()),
                        Math.min(holder.image.getHeight(), ImageUtils.getMaxTextureSize()))
                .filter(ThumborUrlBuilder.noUpscale())
                .toUrl();
        bindImageByUrl(holder, relationship, imageUrl, false);
    }

    private void bindImageByUrl(ViewHolderItem holder, Relationship relationship, String imageUrl, boolean resizeOnPicasso) {
        if (TextUtils.equals(imageUrl, holder.imageViewUrl)) return;
        holder.imageLoading = true;
        holder.imageViewUrl = imageUrl;
        if (imageUrl.toLowerCase(Locale.US).endsWith(".gif")) {
            ImageUtils.loadGifWithProgress(holder.image, imageUrl, holder.gifLoadTag,
                    holder.image.getWidth(), holder.image.getHeight(), holder);
        } else {
            holder.image.setImageResource(getFlowPlaceholderResId(relationship));
            RequestCreator requestCreator =  mPicasso
                    .load(imageUrl)
                    .placeholder(getFlowPlaceholderResId(relationship))
                    .error(R.drawable.image_load_error)
                    .config(Bitmap.Config.RGB_565)
                    .noFade();
            if (resizeOnPicasso) {
                requestCreator
                        .resize(holder.image.getWidth(), holder.image.getHeight())
                        .onlyScaleDown()
                        .centerCrop();
            }

            requestCreator.into(holder.image, holder);
        }
    }

    private void stopImageLoading(ViewHolderItem holder) {
        if (holder.imageLoading) {
            if (DBG) Log.v(TAG, "stopImageLoading() url: " + holder.imageViewUrl);
            mPicasso.cancelRequest(holder.image);
            mOkHttpClient.cancel(holder);
            holder.imageLoading = false;
            holder.imageViewUrl = null;
        }
    }

    @DrawableRes
    private static int getFlowPlaceholderResId(@Nullable Relationship relationship) {
        if (relationship != null && Relationship.isMeSubscribed(relationship.getState())) {
            return R.color.flow_brick_description_image_loading_subscribed;
        } else {
            return R.color.flow_brick_description_image_loading_not_susbscribed;
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View itemView) {
            super(itemView);
        }
    }

    public final class ViewHolderPendingIndicator extends ViewHolder {
        public ViewHolderPendingIndicator(View itemView) {
            super(itemView);
        }
    }

    public final class ViewHolderItem extends ViewHolder implements com.squareup.picasso.Callback, MyRecyclerView.ScrollEventConsumerVh {

        public final ImageView image;

        public final TextView name;

        public final TextView description;

        public final TextView subscribersCount;

        public final TextView entriesCount;

        public final View descriptionContainer;

        public String imageViewUrl = null;

        public final Object gifLoadTag = this;

        public boolean imageLoading;

        public ViewHolderItem(View root) {
            super(root);
            image = (ImageView)root.findViewById(R.id.flow_image);
            name = (TextView)root.findViewById(R.id.flow_name);
            descriptionContainer = root.findViewById(R.id.description_container);
            description = (TextView)root.findViewById(R.id.flow_description);
            subscribersCount = (TextView)root.findViewById(R.id.subscribers_count);
            entriesCount = (TextView)root.findViewById(R.id.entries_count);
        }

        @Override
        public void onSuccess() {
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageLoading = false;
        }

        @Override
        public void onError() {
            image.setScaleType(ImageView.ScaleType.FIT_XY);
            imageLoading = false;
        }

        @Override
        public void onStartScroll() {
            stopGifDrawable();
        }

        @Override
        public void onStopScroll() {
            startGifDrawable();
        }

        private void startGifDrawable() {
            Drawable drawable = image.getDrawable();
            if (drawable != null && drawable instanceof GifDrawable){
                ((GifDrawable) drawable).start();
            }
        }

        private void stopGifDrawable() {
            Drawable drawable = image.getDrawable();
            if (drawable != null && drawable instanceof GifDrawable){
                ((GifDrawable) drawable).stop();
            }
        }

    }

    public interface InteractionListener {

        /**
         * Выполняется после onBindViewHolder(), по position - это индекс не в адаптере, а в списке
         * @param position
         */
        void onBindFlow(ViewHolderItem holder, Flow flow, Relationship relationship, int position);

        RecyclerView.ViewHolder onCreateHeaderViewHolder(ViewGroup parent);

        void onBindHeaderViewHolder(RecyclerView.ViewHolder viewHolder);

        void initClickListeners(RecyclerView.ViewHolder holder, int type);
    }
}
