package ru.taaasty.ui.post;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import ru.taaasty.R;

public class CreatePostActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new CreateTextPostFragment())
                    .commit();
        }
        findViewById(R.id.text_post).setActivated(true);
        findViewById(R.id.private_post_indicator).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.setActivated(!v.isActivated());
            }
        });
    }
}
