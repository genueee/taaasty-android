package ru.taaasty.ui.post;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;

import java.util.ArrayList;
import java.util.List;

import ru.taaasty.R;

/**
 * Created by alexey on 15.03.15.
 */
public class EmbeddMenuDialogFragment extends DialogFragment {

    public final static String ARG_SHOW_REMOVE_MENU_ITEM = "ru.taaasty.ui.post.CreateEmbeddPostFragment.ARG_SHOW_REMOVE_MENU_ITEM";

    public final static String ARG_ENABLE_PASTE_MENU_ITEM = "ru.taaasty.ui.post.CreateEmbeddPostFragment.ARG_ENABLE_PASTE_MENU_ITEM";

    public interface OnDialogInteractionListener {
        void onEmbeddMenuDialogItemSelected(DialogInterface dialog, int resId);

        void onEmbeddMenuDialogDismissed(DialogInterface dialog);
    }

    private OnDialogInteractionListener mListener;

    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (OnDialogInteractionListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement OnDialogInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NO_TITLE, android.R.style.Theme_Holo_Light_Dialog_NoActionBar);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final List<EmbeddDialogMenuItem> menuItems = new ArrayList<>(3);

        final boolean enablePasteMenuItem = getArguments().getBoolean(ARG_ENABLE_PASTE_MENU_ITEM);

        menuItems.add(new EmbeddDialogMenuItem(R.string.embedd_post_menu_paste,
                enablePasteMenuItem, getResources()));

        if (getArguments().getBoolean(ARG_SHOW_REMOVE_MENU_ITEM, false)) {
            menuItems.add(new EmbeddDialogMenuItem(R.string.embedd_post_menu_remove, true, getResources()));
        }

        ListAdapter adapter = new ArrayAdapter<EmbeddDialogMenuItem>(getActivity(),
                android.R.layout.simple_list_item_1,
                android.R.id.text1,
                menuItems
        ) {
            @Override
            public boolean areAllItemsEnabled() {
                return enablePasteMenuItem;
            }

            @Override
            public boolean isEnabled(int position) {
                return enablePasteMenuItem || getItem(position).isEnabled;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                view.setEnabled(isEnabled(position));
                return view;
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setAdapter(adapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int id = menuItems.get(which).id;
                        if (mListener != null)
                            mListener.onEmbeddMenuDialogItemSelected(dialog, id);
                        dismissAllowingStateLoss();
                    }
                });

        return builder.create();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (mListener != null) mListener.onEmbeddMenuDialogDismissed(dialog);
    }

    public static class EmbeddDialogMenuItem {
        public final int id;
        public final CharSequence title;
        public final boolean isEnabled;

        public EmbeddDialogMenuItem(int id, boolean isEnabled, Resources resources) {
            this.id = id;
            this.title = resources.getText(id);
            this.isEnabled = isEnabled;
        }

        @Override
        public String toString() {
            return title.toString();
        }
    }

}
