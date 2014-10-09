package ru.taaasty.ui.post;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.util.Log;
import android.util.SparseArray;
import android.view.ViewGroup;

import ru.taaasty.BuildConfig;

/**
 * Created by alexey on 05.09.14.
 */
public class SectionsPagerAdapter extends FragmentStatePagerAdapter {

    private final Resources mResources;

    private SparseArray<Fragment> registeredFragments = new SparseArray<>();

    private final FragmentManager mFragmentManager;

    public SectionsPagerAdapter(Context context, FragmentManager fm) {
        super(fm);
        mResources = context.getResources();
        mFragmentManager = fm;
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

    public Fragment getRegisteredFragment(int position) {
        return registeredFragments.get(position);
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        Fragment fragment = (Fragment) super.instantiateItem(container, position);
        registeredFragments.put(position, fragment);
        return fragment;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        registeredFragments.remove(position);
        super.destroyItem(container, position, object);
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mResources.getString(Page.values()[position].titleViewId);
    }

    @Override
    public void restoreState(Parcelable state, ClassLoader loader) {
        registeredFragments.clear();
        if (state != null) {
            Bundle bundle = (Bundle) state;
            Iterable<String> keys = bundle.keySet();
            for (String key : keys) {
                if (key.startsWith("f")) {
                    int index = Integer.parseInt(key.substring(1));
                    Fragment f = mFragmentManager.getFragment(bundle, key);
                    if (f != null) {
                        registeredFragments.put(index, f);
                    } else {
                        Log.w("SectionsPagerAdamter", "Bad fragment at key " + key);
                        if (BuildConfig.DEBUG) throw new IllegalStateException();
                    }
                }
            }
        }
    }

}
