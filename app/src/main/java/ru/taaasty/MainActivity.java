package ru.taaasty;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;

import com.squareup.picasso.Picasso;


public class MainActivity extends Activity implements LiveFeedFragment.OnFragmentInteractionListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, LiveFeedFragment.newInstance())
                    .commit();
        }
    }


    @Override
    public void onFeedButtonClicked(Uri uri) {

    }


}
