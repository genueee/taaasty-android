package ru.taaasty.ui.login;


import android.app.Activity;
import android.app.Fragment;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.leolink.android.simpleinfinitecarousel.MyPagerAdapter;

import java.util.ArrayList;
import java.util.List;

import ru.taaasty.R;
import ru.taaasty.utils.ImageUtils;

public class FirstRunFragment extends Fragment {

    public final static int PAGES = 4;
    // You can choose a bigger number for LOOPS, but you know, nobody will fling
    // more than 1000 times just in order to test your "infinite" ViewPager :D
    public final static int LOOPS = 1000;
    public final static int FIRST_PAGE = PAGES * LOOPS / 2;
    public final static float BIG_SCALE = 1.0f;
    public final static float SMALL_SCALE = 0.7f;
    public final static float DIFF_SCALE = BIG_SCALE - SMALL_SCALE;

    public MyPagerAdapter mAdapter;

    @Nullable
    public ViewPager mPager;

    private int mNextItem = FIRST_PAGE;
    private boolean mChangePages = true;
    private Handler mHandler;

    private List<Bitmap> mBitmaps = new ArrayList<Bitmap>();


    public static FirstRunFragment newInstance() {
        return new FirstRunFragment();
    }

    public FirstRunFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_first_run, container, false);


        Point displaySize = new Point();
        getActivity().getWindowManager().getDefaultDisplay().getSize(displaySize);
        displaySize.x -= convertDpToPixel(50);
        displaySize.y -= convertDpToPixel(50);

        mBitmaps.add(ImageUtils.decodeBackgroundBitmap(getActivity(), R.drawable.screenshot_1, displaySize, 0));
        mBitmaps.add(ImageUtils.decodeBackgroundBitmap(getActivity(), R.drawable.screenshot_2, displaySize, 0));
        mBitmaps.add(ImageUtils.decodeBackgroundBitmap(getActivity(), R.drawable.screenshot_3, displaySize, 0));
        mBitmaps.add(ImageUtils.decodeBackgroundBitmap(getActivity(), R.drawable.screenshot_4, displaySize, 0));

        mPager = (ViewPager) root.findViewById(R.id.myviewpager);

        mAdapter = new MyPagerAdapter(this, mListener);
        mPager.setAdapter(mAdapter);
        mPager.setOnPageChangeListener(mAdapter);


        // Set current item to the middle page so we can fling to both
        // directions left and right
        mPager.setCurrentItem(FIRST_PAGE);

        // Necessary or the pager will only have one extra page to show
        // make this at least however many pages you can see
        mPager.setOffscreenPageLimit(3);

        // Set margin for pages as a negative number, so a part of next and
        // previous pages will be showed
        mPager.setPageMargin(-200);

        mHandler = new Handler();

        return root;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();
        mHandler.postDelayed(mChangePageRunnable, 1500);
    }

    @Override
    public void onPause() {
        super.onPause();
        mHandler.removeCallbacks(mChangePageRunnable);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mPager = null;
        mHandler.removeCallbacks(mChangePageRunnable);
        mHandler = null;
    }

    private final Runnable mChangePageRunnable = new Runnable() {
        @Override
        public void run() {
            if (mPager == null) return;
            if(mChangePages) {
                mPager.setCurrentItem(mNextItem, true);
                mNextItem++;
            }
            mHandler.postDelayed(this, 2000);
        }
    };

    private ViewPager.OnPageChangeListener mListener = new ViewPager.OnPageChangeListener() {

        @Override
        public void onPageScrolled(int i, float v, int i2) {
        }

        @Override
        public void onPageSelected(int index) {
            mNextItem = index;
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            if (state == ViewPager.SCROLL_STATE_IDLE) { //this is triggered when the switch to a new page is complete
                mChangePages = true;
            }
            else if( state == ViewPager.SCROLL_STATE_DRAGGING ) {
                mChangePages = false;
            }
        }
    };

    private float convertDpToPixel(float dp){
        Resources resources = getActivity().getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float px = dp * (metrics.densityDpi / 160f);
        return px;
    }

    public Bitmap getBitmapForPos(int pos) {
        return mBitmaps.get(pos);
    }
}
