package ru.taaasty.ui.post;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.res.Resources;
import android.support.v13.app.FragmentPagerAdapter;
import android.view.ViewGroup;

/**
 * Created by alexey on 05.09.14.
 */
public class SectionsPagerAdapter extends FragmentPagerAdapter {

    private final Resources mResources;

    private CreatePostFragmentBase mCurrentPrimaryItem;

    public SectionsPagerAdapter(Context context, FragmentManager fm) {
        super(fm);
        mResources = context.getResources();
    }

    @Override
    public Fragment getItem(int position) {
        switch (Page.values()[position]) {
            case TEXT_POST:
                return CreateTextPostFragment.newInstance();
            case IMAGE_POST:
                return CreateImagePostFragment.newInstance();
            case QUOTE_POST:
                return CreateQuotePostFragment.newInstance();
            default:
                throw  new IllegalStateException();
        }
    }

    @Override
    public int getCount() {
        return Page.values().length;
    }

    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
        super.setPrimaryItem(container, position, object);
        if (mCurrentPrimaryItem != object) {
            mCurrentPrimaryItem = (CreatePostFragmentBase) object;
        }
    }

    public CreatePostFragmentBase getCurrentPrimaryItem() {
        return mCurrentPrimaryItem;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mResources.getString(Page.values()[position].titleViewId);
    }
}
