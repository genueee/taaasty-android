package ru.taaasty.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import ru.taaasty.R;
import ru.taaasty.SortedList;
import ru.taaasty.rest.model.Relationship;
import ru.taaasty.rest.model.User;
import ru.taaasty.ui.relationships.RelationshipListFragmentBase;
import ru.taaasty.utils.ImageUtils;
import ru.taaasty.utils.Objects;

public class RelationshipsAdapter extends BaseAdapter implements RelationshipListFragmentBase.IRelationshipAdapter {
    private final LayoutInflater mInfater;
    private final ImageUtils mImageUtils;

    private final SortedList<Relationship> mRelationships;
    private final boolean mUseReader;

    public RelationshipsAdapter(Context context) {
        this(context, true);
    }

    public RelationshipsAdapter(Context context, boolean useReader) {
        super();
        mInfater = LayoutInflater.from(context);
        mImageUtils = ImageUtils.getInstance();
        mRelationships = new SortedList<>(Relationship.class, new android.support.v7.util.SortedList.Callback<Relationship>() {
            @Override
            public int compare(Relationship o1, Relationship o2) {
                return Relationship.ORDER_BY_ID_DESC_COMARATOR.compare(o1, o2);
            }

            @Override
            public void onInserted(int position, int count) {
                notifyDataSetChanged();
            }

            @Override
            public void onRemoved(int position, int count) {
                notifyDataSetChanged();
            }

            @Override
            public void onMoved(int fromPosition, int toPosition) {
                notifyDataSetChanged();
            }

            @Override
            public void onChanged(int position, int count) {
                notifyDataSetChanged();
            }

            @Override
            public boolean areContentsTheSame(Relationship oldItem, Relationship newItem) {
                return oldItem.equals(newItem);
            }

            @Override
            public boolean areItemsTheSame(Relationship item1, Relationship item2) {
                return Objects.equals(item1.getId(), item2.getId());
            }
        });
        mUseReader = useReader;
    }

    public void setRelationships(List<Relationship> relationships) {
        mRelationships.resetItems(relationships);
    }

    public List<Relationship> getRelationships() {
        return mRelationships.getItems();
    }

    public void setRelationship(Relationship relationship) {
        mRelationships.addOrUpdate(relationship);
    }

    public void deleteRelationship(long id) {
        for (int i = mRelationships.size() - 1; i >= 0; --i) {
            Long l = mRelationships.get(i).getId();
            if (l != null && l == id) {
                mRelationships.removeItemAt(i);
                break;
            }
        }
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
    @SuppressWarnings("ConstantConditions")
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
        User author = mUseReader ? rel.getReader() : rel.getUser();
        mImageUtils.loadAvatar(author.getUserpic(), author.getName(), vh.avatar, R.dimen.avatar_small_diameter);

        vh.userName.setText(author.getName());

        int posts = (int)(author.getTotalEntriesCount() % 1000000);
        vh.entriesCount.setText(vh.entriesCount.getResources().getQuantityString(
                        R.plurals.records_title, posts, posts));

        // XXX: add button

        return res;
    }

    @Override
    public boolean hasStableIds() {
        return true;
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
