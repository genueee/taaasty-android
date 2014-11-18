package ru.taaasty.ui.post;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.res.Resources;

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
    public CharSequence getPageTitle(int position) {
        return mResources.getString(Page.values()[position].titleViewId);
    }

}
