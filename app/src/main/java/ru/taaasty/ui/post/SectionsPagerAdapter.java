package ru.taaasty.ui.post;

import android.content.Context;
import android.content.res.Resources;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import ru.taaasty.adapters.FragmentStatePagerAdapterBase;

/**
 * Created by alexey on 05.09.14.
 */
public class SectionsPagerAdapter extends FragmentStatePagerAdapterBase {

    private final Resources mResources;

    public SectionsPagerAdapter(Context context, FragmentManager fm) {
        super(fm);
        mResources = context.getResources();
    }

    @Override
    public Fragment getItem(int position) {
        switch (Page.values()[position]) {
            case TEXT_POST:
                return CreateTextPostFragment.newCreatePostInstance();
            case IMAGE_POST:
                return CreateImagePostFragment.newInstance(null);
            case QUOTE_POST:
                return CreateQuotePostFragment.newInstance();
            case EMBEDD_POST:
                return CreateEmbeddPostFragment.newInstance(null);
            default:
                throw  new IllegalStateException();
        }
    }

    @Override
    public int getCount() {
        return Page.values().length;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mResources.getString(Page.values()[position].titleViewId);
    }

}
