package ru.taaasty.ui.messages;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import ru.taaasty.R;

public class CreateMessageFragment extends Fragment {

    private ImageView imageView;
    private ProgressBar progressBar;
    private EditText edImageDescription;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_create_message, container, false);
        edImageDescription = (EditText) rootView.findViewById(R.id.ed_image_description);
        imageView = (ImageView) rootView.findViewById(R.id.image);
        progressBar = (ProgressBar) rootView.findViewById(R.id.progress);
        progressBar.setVisibility(View.VISIBLE);
        imageView.setVisibility(View.INVISIBLE);
        return rootView;
    }

    public void showProgressBar() {
        progressBar.setVisibility(View.VISIBLE);
    }

    public void hideProgressBar() {
        progressBar.setVisibility(View.INVISIBLE);
    }

    public String getImageDescription() {
        return edImageDescription.getText().toString();
    }

    public void setImageUri(Uri imageUri) {
        Picasso
                .with(getActivity())
                .load(imageUri)
                .into(imageView, new Callback() {
                    @Override
                    public void onSuccess() {
                        progressBar.setVisibility(View.INVISIBLE);
                        imageView.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onError() {

                    }
                });
    }


}
