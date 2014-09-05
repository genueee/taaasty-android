package ru.taaasty.ui.post;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.res.Resources;
import android.support.v13.app.FragmentPagerAdapter;
import android.view.ViewGroup;

import ru.taaasty.R;

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
        switch (position) {
            case 0:
                return CreateTextPostFragment.newInstance();
            case 1:
                return CreateImagePostFragment.newInstance();
            case 2:
                return CreateQuotePostFragment.newInstance();
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    public int getCount() {
        return 3;
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
        switch (position) {
            case 0:
                return mResources.getString(R.string.title_text_post);
            case 1:
                return mResources.getString(R.string.title_image_post);
            case 2:
                return mResources.getString(R.string.title_quote_post);
        }
        return null;
    }
}
