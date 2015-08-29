package ru.taaasty.ui.post;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import ru.taaasty.adapters.FragmentStatePagerAdapterBase;

/**
 * Created by alexey on 05.09.14.
 */
public class SectionsPagerAdapter extends FragmentStatePagerAdapterBase {

    private final Long mTlogId;

    public SectionsPagerAdapter(Context context, FragmentManager fm, Long tlogId) {
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
