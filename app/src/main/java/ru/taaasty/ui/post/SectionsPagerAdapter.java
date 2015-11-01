package ru.taaasty.ui.post;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

/**
 * Created by alexey on 05.09.14.
 */
public class SectionsPagerAdapter extends FragmentStatePagerAdapter {

    private final Long mTlogId;

    public SectionsPagerAdapter(FragmentManager fm, Long tlogId) {
        super(fm);
        mTlogId = tlogId;
    }

    @Override
    public Fragment getItem(int position) {
        switch (Page.values()[position]) {
            case TEXT_POST:
                return CreateTextPostFragment.newCreatePostInstance(mTlogId);
            case IMAGE_POST:
                return CreateImagePostFragment.newInstance(mTlogId, null);
            case QUOTE_POST:
                return CreateQuotePostFragment.newInstance(mTlogId);
            case EMBEDD_POST:
                return CreateEmbeddPostFragment.newInstance(mTlogId, null);
            default:
                throw  new IllegalStateException();
        }
    }

    @Override
    public int getCount() {
        return Page.values().length;
    }

}
