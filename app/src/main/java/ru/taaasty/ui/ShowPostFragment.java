package ru.taaasty.ui;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.nirhart.parallaxscroll.views.ParallaxListView;
import com.squareup.pollexor.ThumborUrlBuilder;

import java.util.Locale;
import java.util.NoSuchElementException;

import pl.droidsonroids.gif.GifImageView;
import ru.taaasty.BuildConfig;
import ru.taaasty.Constants;
import ru.taaasty.R;
import ru.taaasty.adapters.CommentsAdapter;
import ru.taaasty.model.Comments;
import ru.taaasty.model.Entry;
import ru.taaasty.model.TlogDesign;
import ru.taaasty.model.User;
import ru.taaasty.service.ApiComments;
import ru.taaasty.service.ApiEntries;
import ru.taaasty.utils.ImageUtils;
import ru.taaasty.utils.NetworkUtils;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.observables.AndroidObservable;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.Subscriptions;

/**
 * Пост с комментариями
 */
public class ShowPostFragment extends Fragment {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "LiveFeedFragment";
    private static final String ARG_POST_ID = "post_id";

    private OnFragmentInteractionListener mListener;

    private Subscription mPostSubscribtion = Subscriptions.empty();
    private Subscription mCommentsSubscribtion = Subscriptions.empty();
    private ApiEntries mEntriesService;
    private ApiComments mCommentsService;

    private ParallaxListView mListView;
    private CommentsAdapter mCommentsAdapter;

    private ViewGroup mUserTitleView;
    private ViewGroup mPostContentView;

    private long mPostId;

    private Entry mCurrentEntry;
    private TlogDesign mTlogDesign;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment LiveFeedFragment.
     */
    public static ShowPostFragment newInstance(long postId) {
        ShowPostFragment f = new  ShowPostFragment();
        Bundle b = new Bundle();
        b.putLong(ARG_POST_ID, postId);
        f.setArguments(b);
        return f;
    }

    public ShowPostFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        mPostId = args.getLong(ARG_POST_ID);
        mCommentsService = NetworkUtils.getInstance().createRestAdapter().create(ApiComments.class);
        mEntriesService = NetworkUtils.getInstance().createRestAdapter().create(ApiEntries.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_show_post, container, false);

        mListView = (ParallaxListView) v.findViewById(R.id.list_view);
        mUserTitleView = (ViewGroup) inflater.inflate(R.layout.header_show_post, mListView, false);
        mPostContentView = (ViewGroup) inflater.inflate(R.layout.post_item, mListView, false);

        mUserTitleView.findViewById(R.id.avatar).setOnClickListener(mOnClickListener);
        return v;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mCommentsAdapter = new CommentsAdapter(getActivity());

        mListView.addParallaxedHeaderView(mUserTitleView);
        mListView.addHeaderView(mPostContentView);
        mListView.setAdapter(mCommentsAdapter);

        refreshEntry();
    }

    private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.avatar:
                    onAvatarClicked(v);
                    break;
            }
        }
    };


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mPostSubscribtion.unsubscribe();
        mCommentsSubscribtion.unsubscribe();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    void onAvatarClicked(View v) {
        Toast.makeText(getActivity(), R.string.not_ready_yet, Toast.LENGTH_SHORT).show();
    }

    void setupAuthor() {
        if (mCurrentEntry == null || mCurrentEntry.getAuthor() == null) {
            // XXX
        } else {
            User author = mCurrentEntry.getAuthor();
            String name = author.getName();
            if (name == null) name = "";
            name = name.substring(0,1).toUpperCase(Locale.getDefault()) + name.substring(1);
            ((TextView)mUserTitleView.findViewById(R.id.user_name)).setText(name);
            setupAvatar(author);
        }
    }

    void setupFeedDesign() {
    }

    void setupPost() {
        if (mCurrentEntry == null) {
            mPostContentView.setVisibility(View.GONE);
            // XXX
            return;
        }
        mPostContentView.setVisibility(View.VISIBLE);
        setupPostImage();
        setupPostTitle();
        setupPostText();

    }

    // XXX
    private void setupPostImage() {
        GifImageView imageView = (GifImageView)mPostContentView.findViewById(R.id.gif_image);

        if (mCurrentEntry.getImages().isEmpty()) {
            imageView.setVisibility(View.GONE);
            return;
        }

        Entry.Image image = mCurrentEntry.getImages().get(0);
        ThumborUrlBuilder b = NetworkUtils.createThumborUrlFromPath(image.image.path);

        float dstWidth, dstHeight;
        float imgWidth, imgHeight;

        // XXX: check for 0
        float parentWidth = mListView.getMeasuredWidth();
        if (parentWidth < image.image.geometry.width) {
            imgWidth = parentWidth;
            imgHeight = (float)image.image.geometry.height * parentWidth / (float)image.image.geometry.width;
            b.resize((int)Math.ceil(imgWidth), 0);
        } else {
            imgWidth = image.image.geometry.width;
            imgHeight = image.image.geometry.height;
        }
        dstWidth = parentWidth;
        dstHeight = imgHeight * (dstWidth / imgWidth);

        if (DBG) Log.v(TAG, "setimagesize " + dstWidth + " " + dstHeight);
        ViewGroup.LayoutParams lp = imageView.getLayoutParams();
        lp.height = (int)Math.ceil(dstHeight);
        imageView.setLayoutParams(lp);
        imageView.setVisibility(View.VISIBLE);

        NetworkUtils.getInstance().getPicasso(getActivity())
                .load(b.toUrl())
                .placeholder(R.drawable.image_loading_drawable)
                .error(R.drawable.image_loading_drawable)
                .noFade()
                .into(imageView);
    }

    private void setupPostTitle() {
        TextView titleView = (TextView)mPostContentView.findViewById(R.id.title);
        String title = mCurrentEntry.getTitle();
        if (TextUtils.isEmpty(title)) {
            titleView.setVisibility(View.GONE);
        } else {
            titleView.setText(Html.fromHtml(title));
            titleView.setVisibility(View.VISIBLE);
        }
    }

    private void setupPostText() {
        TextView textView = (TextView)mPostContentView.findViewById(R.id.text);
        TextView sourceView = (TextView)mPostContentView.findViewById(R.id.source);
        CharSequence text = mCurrentEntry.getTextSpanned();
        CharSequence source = mCurrentEntry.getSourceSpanned();

        // XXX: другой шрифт если есть source
        if (text == null) {
            textView.setVisibility(View.GONE);
        } else {
            textView.setText(text);
            textView.setVisibility(View.VISIBLE);
        }

        if (source == null) {
            sourceView.setVisibility(View.GONE);
        } else {
            sourceView.setText(source);
            sourceView.setVisibility(View.VISIBLE);
        }
    }

    private void setupAvatar(User author) {
        ImageUtils.getInstance().loadAvatar(author,
                (ImageView)getView().findViewById(R.id.avatar),
                R.dimen.avatar_normal_diameter);
    }

    public void refreshEntry() {
        if (!mPostSubscribtion.isUnsubscribed()) {
            mPostSubscribtion.unsubscribe();
        }

        Observable<Entry> observablePost = AndroidObservable.bindFragment(this,
                mEntriesService.getEntry(mPostId, false));

        mPostSubscribtion = observablePost
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mCurrentEntryObserver);
    }

    private void loadComments() {
        mCommentsSubscribtion.unsubscribe();

        Observable<Comments> observableComments = AndroidObservable.bindFragment(this,
                mCommentsService.getComments(mPostId, mCommentsAdapter.getTopCommentId(),
                        Constants.SHOW_POST_COMMENTS_COUNT));

        mCommentsSubscribtion = observableComments
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mCommentsObserver);

    }

    private final Observer<Entry> mCurrentEntryObserver = new Observer<Entry>() {

        @Override
        public void onCompleted() {

        }

        @Override
        public void onError(Throwable e) {
            // XXX
            if (e instanceof NoSuchElementException) {
                setupAuthor();
            }
            mListener.notifyError(getString(R.string.error_loading_user), e);
        }

        @Override
        public void onNext(Entry entry) {
            //setupFeedDesign(currentUser.getDesign());
            mCurrentEntry = entry;
            setupAuthor();
            setupPost();
            loadComments();
        }
    };

    private final Observer<Comments> mCommentsObserver = new Observer<Comments>() {

        @Override
        public void onCompleted() {

        }

        @Override
        public void onError(Throwable e) {
            mListener.notifyError(getString(R.string.error_loading_comments), e);
        }

        @Override
        public void onNext(Comments comments) {
            mCommentsAdapter.appendComments(comments.comments);
        }
    };

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener extends CustomErrorView {
    }
}
