package net.leolink.android.simpleinfinitecarousel;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import ru.taaasty.R;
import ru.taaasty.ui.login.FirstRunFragment;


public class MyFragment extends Fragment {

    private static FirstRunFragment mParent;

	public static Fragment newInstance(FirstRunFragment parent, int pos, float scale)
	{
		Bundle b = new Bundle();
		b.putInt("pos", pos);
		b.putFloat("scale", scale);
        mParent = parent;

		return Fragment.instantiate(parent.getActivity(), MyFragment.class.getName(), b);
	}


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if (container == null) {
			return null;
		}

        int pos = this.getArguments().getInt("pos");

        LinearLayout l = (LinearLayout)inflater.inflate(R.layout.fragment_first_run_page_1, container, false);

        ImageView iv = (ImageView)l.findViewById(R.id.screenshot);
        iv.setImageBitmap(mParent.getBitmapForPos(pos));

		MyLinearLayout root = (MyLinearLayout) l.findViewById(R.id.screenshot_page_root);
		float scale = this.getArguments().getFloat("scale");
		root.setScaleBoth(scale);

		return l;
	}
}
