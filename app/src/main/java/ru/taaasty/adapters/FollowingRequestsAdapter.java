package ru.taaasty.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.SortedList;
import ru.taaasty.model.Relationship;
import ru.taaasty.model.User;
import ru.taaasty.ui.relationships.RelationshipListFragmentBase;
import ru.taaasty.utils.ImageUtils;
import ru.taaasty.utils.Objects;

public class FollowingRequestsAdapter extends BaseAdapter implements RelationshipListFragmentBase.IRelationshipAdapter {
    private final LayoutInflater mInfater;
    private final ImageUtils mImageUtils;

    private final SortedList<Relationship> mRelationships;

    public FollowingRequestsAdapter(Context context) {
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
                if (BuildConfig.DEBUG) Log.v("FollowingsRqAdapt", "onInserted pos: " + position + " count: " + count);
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
    }

    public void setRelationships(List<Relationship> relationships) {
        mRelationships.resetItems(relationships);
    }

    public List<Relationship> getRelationships() {
        return mRelationships.getItems();
    }

    public void setRelationship(Relationship relationship) {
        mRelationships.add(relationship);
    }

    public void deleteRelationship(long id) {
        for (int i=mRelationships.size() - 1; i >= 0; --i) {
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
