package ru.taaasty.ui.post;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SearchView.OnCloseListener;
import android.support.v7.widget.SearchView.OnQueryTextListener;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.squareup.pollexor.ThumborUrlBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import ru.taaasty.ActivityBase;
import ru.taaasty.R;
import ru.taaasty.Session;
import ru.taaasty.rest.RestClient;
import ru.taaasty.rest.model.Entry;
import ru.taaasty.rest.model.Flow;
import ru.taaasty.rest.model.Flow.FlowPic;
import ru.taaasty.rest.model.FlowList;
import ru.taaasty.rest.model.FlowList.FlowSubList;
import ru.taaasty.rest.model.User;
import ru.taaasty.rest.service.ApiFlows;
import ru.taaasty.rest.service.ApiReposts;
import ru.taaasty.ui.DividerItemDecoration;
import ru.taaasty.utils.CircleTransformation;
import ru.taaasty.utils.ImageUtils;
import ru.taaasty.utils.MessageHelper;
import ru.taaasty.utils.NetworkUtils;
import ru.taaasty.widgets.ExtendedImageView;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.Subscriptions;

/**
 * Created by arkhipov on 16.02.2016.
 */
public class RepostActivity extends ActivityBase {

    private static final String ARG_ENTRY = "ru.taaasty.ui.post.RepostActivity.ARG_ENTRY";
    private static final String KEY_FLOWS = "ru.taaasty.ui.post.RepostActivity.KEY_FLOWS";
    private static final String KEY_FILTER = "ru.taaasty.ui.post.RepostActivity.KEY_FILTER";

    public static final int REQUEST_CODE_LOGIN = 1;
    public static final int FLOWS_PAGE_LIMIT = 50;

    private Entry mEntry;

    private ApiFlows mApiFlows;
    private ApiReposts mApiReposts;

    private SearchView mSearchView;
    private RecyclerView mRecyclerView;
    private ProgressBar mProgress;
    private RepostItemsAdapter mAdapter;
    private final CircleTransformation mCircleTransformation = new CircleTransformation();

    Subscription mFlowsSubscription = Subscriptions.unsubscribed();
    Subscription mRepostSubscription = Subscriptions.unsubscribed();

    public static void startActivity(Activity activity, Entry entry, int requestCode) {
        Intent intent = new Intent(activity, RepostActivity.class);
        intent.putExtra(ARG_ENTRY, entry);
        activity.startActivityForResult(intent, requestCode);
        activity.overridePendingTransition(R.anim.scroll_up_in, 0);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_share_repost);
        mProgress = (ProgressBar) findViewById(R.id.progress);
        mRecyclerView = (RecyclerView) findViewById(R.id.scroll_container);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        mRecyclerView.addItemDecoration(new DividerItemDecoration(this, R.drawable.repost_list_divider_line));
        mAdapter = new RepostItemsAdapter(this);
        mRecyclerView.setAdapter(mAdapter);
        mSearchView = (SearchView) findViewById(R.id.search_view);
        mSearchView.setOnQueryTextListener(onQueryTextListener);
        mSearchView.setOnCloseListener(onCloseListener);
        mSearchView.setIconified(false);
        mSearchView.clearFocus();
        mSearchView.setOnCloseListener(() -> {
            finish();
            return false;
        });

        findViewById(R.id.touch_outside).setOnClickListener(v -> finish());

        View bottomSheet = findViewById(R.id.bottom_sheet);
        BottomSheetBehavior behavior = BottomSheetBehavior.from(bottomSheet);
        //behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        behavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                //if (DBG) Log.d(TAG, "nStateChanged() newState: " + newState);
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    finish();
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                //if (DBG) Log.d(TAG, "onSlide() called slideOffset: " + slideOffset);
            }
        });

        mEntry = getIntent().getParcelableExtra(ARG_ENTRY);
        if (mEntry == null) throw new IllegalArgumentException("ARG_ENTRY not defined");

        mApiFlows = RestClient.getAPiFlows();
        mApiReposts = RestClient.getApiReposts();

        if (savedInstanceState != null) {
            mAdapter.restoreInstanceState(savedInstanceState);
            if (!TextUtils.isEmpty(mAdapter.filter)) {
                mSearchView.setQuery(mAdapter.filter, false);
            }
        } else {
            loadAvailableFlows(1, FLOWS_PAGE_LIMIT, false);
        }
    }

    public void setProgressVisible(boolean isVisible) {
        mProgress.setVisibility(isVisible ? View.VISIBLE : View.GONE);
    }

    public void loadAvailableFlows(int page, int limit, boolean staffs) {
        mFlowsSubscription.unsubscribe();
        setProgressVisible(true);

        Observable<FlowList> observableComments = mApiFlows.getAvailableFlows(page, limit, staffs);

        mFlowsSubscription = observableComments
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mFlowsObserver);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        mAdapter.saveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.scroll_down_out);
    }

    @Override
    protected void onDestroy() {
        mFlowsSubscription.unsubscribe();
        mRepostSubscription.unsubscribe();
        super.onDestroy();
    }

    private OnCloseListener onCloseListener = new OnCloseListener() {
        @Override
        public boolean onClose() {
            mAdapter.setFilter(null);
            return false;
        }
    };

    private OnQueryTextListener onQueryTextListener = new OnQueryTextListener() {
        @Override
        public boolean onQueryTextSubmit(String query) {
            return false;
        }

        @Override
        public boolean onQueryTextChange(String newText) {
            mAdapter.setFilter(newText);
            return false;
        }
    };

    private Observer<FlowList> mFlowsObserver = new Observer<FlowList>() {
        @Override
        public void onCompleted() {
            setProgressVisible(false);
        }

        @Override
        public void onError(Throwable e) {
            MessageHelper.showError(RepostActivity.this, getString(R.string.error_loading_flows), e);
            finish();
        }

        @Override
        public void onNext(FlowList flowList) {
            ArrayList<Flow> flows = new ArrayList<>();
            for (FlowSubList subList : flowList.items) {
                flows.add(subList.flow);
            }

            mAdapter.setFlows(flows);
        }
    };

    private Observer<Entry> mRepostObserver = new Observer<Entry>() {
        @Override
        public void onCompleted() {

        }

        @Override
        public void onError(Throwable e) {
            setProgressVisible(false);
            MessageHelper.showError(RepostActivity.this, getString(R.string.error_loading_flows), e);
        }

        @Override
        public void onNext(Entry entry) {
            setProgressVisible(false);
            Toast.makeText(RepostActivity.this, R.string.repost_successful, Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        }
    };

    public void repostToFlow(long flowId) {
        mRepostSubscription.unsubscribe();
        setProgressVisible(true);

        Observable<Entry> observable = mApiReposts.repost(flowId, mEntry.getId());

        mRepostSubscription = observable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mRepostObserver);
    }

    private class RepostItemsAdapter extends RecyclerView.Adapter {

        private ArrayList<Flow> flows = new ArrayList<>();
        private ArrayList<RepostTarget> repostTargets = new ArrayList<>();
        private ArrayList<RepostTarget> filtered = new ArrayList<>();

        private String filter;
        private Context context;

        public RepostItemsAdapter(Context context) {
            this.context = context;
        }

        public void saveInstanceState(Bundle bundle) {
            bundle.putParcelableArrayList(KEY_FLOWS, flows);
            bundle.putString(KEY_FILTER, filter);
        }

        public void restoreInstanceState(Bundle bundle) {
            ArrayList<Flow> flows = bundle.getParcelableArrayList(KEY_FLOWS);
            filter = bundle.getString(KEY_FILTER);
            setFlows(flows);
        }

        public void setFlows(List<Flow> flows) {
            this.flows.clear();
            this.flows.addAll(flows);
            repostTargets.clear();
            repostTargets.add(new RepostTarget(Session.getInstance().getCachedCurrentUser().getName(), null));

            for (Flow flow : flows) {
                repostTargets.add(new RepostTarget(flow.getName(), flow));
            }
            applyFilter();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            FlowHolder holder = new FlowHolder(LayoutInflater.from(context).inflate(R.layout.list_item_flow, parent, false));
            holder.root.setOnClickListener(onClickListener);
            return holder;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            final FlowHolder flowHolder = (FlowHolder) holder;
            if (filtered.get(position).flow == null) {
                User currentUser = Session.getInstance().getCachedCurrentUser();
                ImageUtils.getInstance()
                        .loadAvatarToImageView(currentUser, R.dimen.avatar_small_diameter, flowHolder.imageView);
                flowHolder.title.setText(currentUser.getName());
                flowHolder.root.setTag(currentUser.getId());
            } else {
                Flow flow = filtered.get(position).flow;
                flowHolder.bindFlowImage(flow.getFlowPic());
                flowHolder.title.setText(flow.getName());
                flowHolder.root.setTag(flow.getId());
            }
        }

        OnClickListener onClickListener = v -> {
            long flowId = (Long) v.getTag();
            repostToFlow(flowId);
        };

        @Override
        public int getItemCount() {
            return filtered.size();
        }

        public void setFilter(String filter) {
            this.filter = filter;
            applyFilter();
        }

        private void applyFilter() {
            filtered.clear();
            for (RepostTarget target : repostTargets) {
                if (TextUtils.isEmpty(filter)
                        || target.title.length() >= filter.length()
                        && target.title.substring(0, filter.length())
                            .toLowerCase(Locale.getDefault()).contains(filter.toLowerCase(Locale.getDefault()))) {
                    filtered.add(target);
                }
            }
            notifyDataSetChanged();
        }

        public class FlowHolder extends RecyclerView.ViewHolder {

            ExtendedImageView imageView;
            TextView title;
            private View root;

            public FlowHolder(View itemView) {
                super(itemView);
                root = itemView;
                imageView = (ExtendedImageView) itemView.findViewById(R.id.flow_image);
                title = (TextView) itemView.findViewById(R.id.flow_title);
            }

            public void bindFlowImage(FlowPic pic) {
                final int imageSize = context.getResources().getDimensionPixelSize(R.dimen.avatar_small_diameter);
                ThumborUrlBuilder thumborUrl = NetworkUtils.createThumborUrl(pic.originalUrl);
                String userpicUrl = thumborUrl.resize(imageSize, imageSize)
                        .toUrlUnsafe();

                Picasso.with(context)
                        .load(userpicUrl)
                        .error(R.drawable.image_load_error)
                        .config(Bitmap.Config.RGB_565)
                        .resize(imageSize, imageSize)
                        .onlyScaleDown()
                        .centerCrop()
                        .noFade()
                        .transform(mCircleTransformation)
                        .into(imageView);
            }
        }

        private class RepostTarget {
            Flow flow;
            String title;

            public RepostTarget(String title, Flow flow) {
                this.title = title;
                this.flow = flow;
            }
        }
    }
}
