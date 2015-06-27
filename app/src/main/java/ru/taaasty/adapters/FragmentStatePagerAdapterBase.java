package ru.taaasty.adapters;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.util.Log;
import android.util.SparseArray;
import android.view.ViewGroup;

import ru.taaasty.BuildConfig;

/**
 * {@link android.support.v13.app.FragmentStatePagerAdapter} с {@linkplain #getRegisteredFragment(int)}
 * TODO избавиться
 */
public abstract class FragmentStatePagerAdapterBase extends FragmentStatePagerAdapter {

    private SparseArray<Fragment> registeredFragments = new SparseArray<>();

    private final FragmentManager mFragmentManager;

    /**
     * {@inheritDoc}
     */
    public FragmentStatePagerAdapterBase(FragmentManager fm) {
        super(fm);
        mFragmentManager = fm;
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
    public void restoreState(Parcelable state, ClassLoader loader) {
        super.restoreState(state, loader);
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
                        Log.w("SectionsPagerAdapter", "Bad fragment at key " + key);
                        if (BuildConfig.DEBUG) throw new IllegalStateException();
                    }
                }
            }
        }
    }

}
