package ru.taaasty.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import ru.taaasty.R;
import ru.taaasty.model.Relationship;
import ru.taaasty.model.User;
import ru.taaasty.utils.ImageUtils;

public class RelationshipsAdapter extends BaseAdapter {
    private final LayoutInflater mInfater;
    private final ImageUtils mImageUtils;

    private final ArrayList<Relationship> mRelationships;

    public RelationshipsAdapter(Context context) {
        super();
        mInfater = LayoutInflater.from(context);
        mImageUtils = ImageUtils.getInstance();
        mRelationships = new ArrayList<Relationship>();
    }

    public void setRelationships(List<Relationship> relationships) {
        mRelationships.clear();
        appendRelationships(relationships);
    }

    public ArrayList<Relationship> getRelationships() {
        return mRelationships;
    }

    public void appendRelationships(List<Relationship> relationships) {
        // XXX: sort, remove duplicates
        mRelationships.addAll(relationships);
        notifyDataSetChanged();
    }

    public Long getTopCommentId() {
        if (mRelationships.isEmpty()) return null;
        return mRelationships.get(0).getId();
    }

    @Override
    public int getCount() {
        return mRelationships.size();
    }

    @Override
    public Relationship getItem(int position) {
        return mRelationships.get(position);
    }

    @Override
    public long getItemId(int position) {
        return mRelationships.get(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder vh;
        View res;

        if (convertView == null) {
            res = mInfater.inflate(R.layout.relationships_item, parent, false);
            vh = new ViewHolder(res);
            res.setTag(R.id.relationships_view_holder, vh);
        } else {
            res = convertView;
            vh = (ViewHolder) res.getTag(R.id.relationships_view_holder);
        }

        Relationship rel = getItem(position);
        User author = rel.getReader();
        mImageUtils.loadAvatar(author.getUserpic(), author.getName(), vh.avatar, R.dimen.avatar_small_diameter);

        vh.userName.setText(author.getName());

        int posts = (int)(author.getTotalEntriesCount() % 1000000);
        vh.entriesCount.setText(vh.entriesCount.getResources().getQuantityString(
                        R.plurals.records_title, posts, posts));

        // XXX: add button

        return res;
    }

    public class ViewHolder {
        public final ImageView avatar;
        public final TextView userName;
        public final TextView entriesCount;
        public final View addedButton;
        public final View addButton;

        public ViewHolder(View v) {
            avatar = (ImageView) v.findViewById(R.id.avatar);
            userName = (TextView) v.findViewById(R.id.user_name);
            entriesCount = (TextView) v.findViewById(R.id.posts_count);
            addButton = v.findViewById(R.id.add_relationship);
            addedButton = v.findViewById(R.id.relationship_added);
        }
    }
}
