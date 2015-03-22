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
import ru.taaasty.model.Relationship;
import ru.taaasty.model.User;
import ru.taaasty.ui.relationships.RelationshipListFragmentBase;
import ru.taaasty.utils.ImageUtils;

public class FollowingRequestsAdapter extends BaseAdapter implements RelationshipListFragmentBase.IRelationshipAdapter {
    private final LayoutInflater mInfater;
    private final ImageUtils mImageUtils;

    private final SortedList<Relationship> mRelationships;

    public FollowingRequestsAdapter(Context context) {
        super();
        mInfater = LayoutInflater.from(context);
        mImageUtils = ImageUtils.getInstance();
        mRelationships = new SortedList<Relationship>(Relationship.ORDER_BY_CREATE_DATE_DESC_ID_COMARATOR) {
            @Override
            public long getItemId(Relationship item) {
                return item.getId();
            }
        };
        mRelationships.setListener(new SortedList.OnListChangedListener() {
            @Override
            public void onDataSetChanged() {
                notifyDataSetChanged();
            }

            @Override
            public void onItemChanged(int location) {
                notifyDataSetChanged();
            }

            @Override
            public void onItemInserted(int location) {
                notifyDataSetChanged();
            }

            @Override
            public void onItemRemoved(int location) {
                notifyDataSetChanged();
            }

            @Override
            public void onItemMoved(int fromLocation, int toLocation) {
                notifyDataSetChanged();
            }

            @Override
            public void onItemRangeChanged(int locationStart, int itemCount) {
                notifyDataSetChanged();
            }

            @Override
            public void onItemRangeInserted(int locationStart, int itemCount) {
                notifyDataSetChanged();
            }

            @Override
            public void onItemRangeRemoved(int locationStart, int itemCount) {
                notifyDataSetChanged();
            }
        });
    }

    public void setRelationships(List<Relationship> relationships) {
        mRelationships.resetItems(relationships);
    }

    public List<Relationship> getRelationships() {
        return mRelationships.getItems();
    }

    public void setRelationship(Relationship relationship) {
        mRelationships.insertItem(relationship);
    }

    public void deleteRelationship(long id) {
        mRelationships.deleteItem(id);
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
            res = mInfater.inflate(R.layout.following_requests_item, parent, false);
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
        public final View approveButton;
        public final View disapproveButton;

        public ViewHolder(View v) {
            avatar = (ImageView) v.findViewById(R.id.avatar);
            userName = (TextView) v.findViewById(R.id.user_name);
            entriesCount = (TextView) v.findViewById(R.id.posts_count);
            approveButton = v.findViewById(R.id.approve);
            disapproveButton = v.findViewById(R.id.disapprove);
        }
    }
}
