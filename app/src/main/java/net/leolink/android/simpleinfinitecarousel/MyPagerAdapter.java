package net.leolink.android.simpleinfinitecarousel;

import android.app.Fragment;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;

import ru.taaasty.R;
import ru.taaasty.ui.login.FirstRunFragment;


public class MyPagerAdapter extends FragmentPagerAdapter implements
		ViewPager.OnPageChangeListener {

	private MyLinearLayout cur = null;
	private MyLinearLayout next = null;
    private FirstRunFragment mParent;
    private ViewPager.OnPageChangeListener mListener;


	public MyPagerAdapter( FirstRunFragment parent, ViewPager.OnPageChangeListener attachedListener ) {
		super(parent.getFragmentManager());
        mParent = parent;
        mListener = attachedListener;
	}

	@Override
	public Fragment getItem(int position)
	{
        float scale = 0;

        // make the first pager bigger than others
        if (position == mParent.FIRST_PAGE)
        	scale = mParent.BIG_SCALE;
        else
        	scale = mParent.SMALL_SCALE;
        
        position = position % mParent.PAGES;

        return MyFragment.newInstance(mParent, position, scale);
	}

	@Override
	public int getCount()
	{		
		return mParent.PAGES * mParent.LOOPS;
	}

	@Override
	public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels)
	{	
		if (positionOffset >= 0f && positionOffset <= 1f)
		{
			cur = getRootView(position);
			next = getRootView(position +1);

			cur.setScaleBoth(mParent.BIG_SCALE
					- mParent.DIFF_SCALE * positionOffset);
			next.setScaleBoth(FirstRunFragment.SMALL_SCALE
					+ mParent.DIFF_SCALE * positionOffset);
		}

        mListener.onPageScrolled(position, positionOffset, positionOffsetPixels);
	}

	@Override
	public void onPageSelected(int position) {
        mListener.onPageSelected(position);
    }
	
	@Override
	public void onPageScrollStateChanged(int state) {
        mListener.onPageScrollStateChanged(state);
    }
	
	private MyLinearLayout getRootView(int position)
	{
		return (MyLinearLayout)
                mParent.getFragmentManager().findFragmentByTag(this.getFragmentTag(position))
				.getView().findViewById(R.id.screenshot_page_root);
	}
	
	private String getFragmentTag(int position)
	{
	    return "android:switcher:" + mParent.mPager.getId() + ":" + position;
	}
}
