package ru.taaasty.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import ru.taaasty.ActivityBase;
import ru.taaasty.R;

public class AdditionalMenuActivity extends ActivityBase {

    public static final String RESULT_REQUESTED_VIEW_ID = "ru.taaasty.ui.feeds.RESULT_REQUESTED_VIEW_ID";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_additional_menu);

        for (int vid: new int[] {
                R.id.profile,
                R.id.favorites,
                R.id.hidden,
                R.id.settings,
                R.id.friends,
                R.id.back_button

        }) {
            findViewById(vid).setOnClickListener(mOnClickListener);
        }
    }

    View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.back_button:
                    setResult(RESULT_CANCELED);
                    finish();
                    break;
                default:
                    finishWithResultViewId(v.getId());
                    break;
            }
        }
    };

    void finishWithResultViewId(int viewId) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(RESULT_REQUESTED_VIEW_ID, viewId);
        setResult(RESULT_OK, resultIntent);
        finish();
    }


}
