package ru.taaasty.adapters;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v4.util.LongSparseArray;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.greenrobot.event.EventBus;
import ru.taaasty.BuildConfig;
import ru.taaasty.Session;
import ru.taaasty.events.RelationshipChanged;
import ru.taaasty.events.RelationshipRemoved;
import ru.taaasty.rest.model.Flow;
import ru.taaasty.rest.model.FlowList;
import ru.taaasty.rest.model.Relationship;
import ru.taaasty.utils.Objects;

// TODO relationship с id = null - бесполезные relationship, не хранить в map?
public class FlowListManaged implements Parcelable {

    private final List<Flow> mFlowList;

    /**
     * Flow id => relationship
     */
    private final LongSparseArray<Relationship> mRelationshipMap;

    /**
     * FLow id
     */
    private final Set<Long> mFlowMap;

    private FlowChangedCallback mCallback = DUMMY_CALLBACK;

    public FlowListManaged() {
        mFlowList = new ArrayList<>();
        mRelationshipMap = new LongSparseArray<>();
        mFlowMap = new HashSet<>();
    }

    public void onCreate() {
        EventBus.getDefault().register(this);
    }

    public void onDestroy() {
        EventBus.getDefault().unregister(this);
    }

    public void setListener(FlowChangedCallback callback) {
        mCallback = callback != null ? callback : DUMMY_CALLBACK;
    }

    public Flow getFlow(int location) {
        return mFlowList.get(location);
    }

    @Nullable
    public Relationship getRelationship(int flowLocation) {
        return mRelationshipMap.get(getFlow(flowLocation).getId());
    }

    @Nullable
    public Relationship getRelationshipByFlowId(long flowId) {
        return mRelationshipMap.get(flowId);
    }

    public void grepMyFlows() {
        for (int i = mFlowList.size() - 1; i >= 0; --i) {
            Relationship relationship = getRelationship(i);
            if (relationship == null || !Relationship.isMeSubscribed(relationship.getState())) {
                remove(i, true);
            }
        }
    }

    public int getFlowLocation(long flowId) {
        if (!mFlowMap.contains(flowId)) return -1;
        for (int i = mFlowList.size() - 1; i >= 0; --i) {
            if (mFlowList.get(i).getId() == flowId) {
                return i;
            }
        }
        return -1;
    }

    public int size() {
        return mFlowList.size();
    }

    public boolean isEmpty() {
        return mFlowList.isEmpty();
    }

    public void refreshOrAddItems(FlowList.FlowSubList[] items) {
        resetInternal(items, false);
    }

    public void reset(FlowList.FlowSubList[] items) {
        resetInternal(items, true);
    }

    public void onEventMainThread(RelationshipChanged event) {
        onNewRelationshipReceived(event.relationship);
    }

    public void onEventMainThread(RelationshipRemoved event) {
        onNewRelationshipReceived(event.relationship);
    }

    private void onNewRelationshipReceived(Relationship relationship) {
        if (Session.getInstance().isMe(relationship.getFromId())) {
            long tlogId = relationship.getToId();
            if (mFlowMap.contains(tlogId)
                    && !Objects.equals(mRelationshipMap.get(tlogId), relationship)) {
                mRelationshipMap.put(tlogId, relationship);
                mCallback.onChanged(getFlowLocation(tlogId), 1);
            }
        }
    }

    private void resetInternal(FlowList.FlowSubList[] items, boolean forceReplace) {
        if (items.length > 0 && hasSameIds(0, items)) {
            // Мягкий reset. ID новых элементов совпадают с уже имеющимися
            updateSublistItems(0, items);

            //  Удаляем всё, что дальше по списку
            if (forceReplace) {
                int predSize = mFlowList.size();
                for (int i = mFlowList.size() - 1; i >= items.length; --i) remove(i, false);
                if (predSize != items.length)
                    mCallback.onRemoved(items.length, predSize - items.length);
            }
        } else {
            // Либо это ноый список, либо список изменился. Удаляем всё.
            clear();
            appendInternal(items);
        }
    }

    public void remove(int location, boolean notify) {
        Flow oldFlow = mFlowList.remove(location);
        mRelationshipMap.remove(oldFlow.getId());
        mFlowMap.remove(oldFlow.getId());
        if (notify) mCallback.onRemoved(location, 1);
    }

    public void appendEntries(FlowList.FlowSubList[] items) {
        if (items.length == 0) return;

        int firstItemLocation = getFlowLocation(items[0].flow.getId());
        if (firstItemLocation >= 0 && hasSameIds(firstItemLocation, items)) {
            // В списке уже имеется список с такими же id, как в items. Просто обновляем элементы
            updateSublistItems(firstItemLocation, items);
        } else {
            // Удаление элементов, которые уже есть
            Set<Long> toRemove = new HashSet<>();
            for (FlowList.FlowSubList item: items) {
                if (mFlowMap.contains(item.flow.getId())) toRemove.add(item.flow.getId());
            }
            if (!toRemove.isEmpty()) {
                for (int i = mFlowList.size() - 1; i >= 0; --i) {
                    long flowId = mFlowList.get(i).getId();
                    if (toRemove.contains(flowId)) remove(i, true);
                }
            }

            //добавление
            appendInternal(items);
        }
    }

    public void clear() {
        if (mFlowList.isEmpty()) return;
        final int prevSize = mFlowList.size();
        mFlowList.clear();
        mFlowMap.clear();
        mRelationshipMap.clear();
        mCallback.onRemoved(0, prevSize);
    }

    private boolean hasSameIds(int fromId, FlowList.FlowSubList[] items) {
        if (mFlowList.size() - fromId < items.length) return false;
        for (int i = 0; i < items.length; ++i) {
            if (mFlowList.get(i + fromId).getId() != items[i].flow.getId()) return false;
        }
        return true;
    }

    private boolean itemsEquals(int location, FlowList.FlowSubList item) {
        return getFlow(location).equals(item.flow)
                && (Objects.equals(getRelationship(location), item.relationship));

    }

    /**
     * Простое добавление элементов, без доп. проверок
     * @param items
     */
    private void appendInternal(FlowList.FlowSubList[] items) {
        if (items.length == 0) return;
        int predSize = mFlowList.size();
        for (FlowList.FlowSubList item: items) {
            // На всякий случай, кто ж знает, что там в API придумают
            if (mFlowMap.contains(item.flow.getId())) continue;
            mFlowList.add(item.flow);
            mFlowMap.add(item.flow.getId());
            if (item.relationship != null) mRelationshipMap.put(item.flow.getId(), item.relationship);
        }
        mCallback.onInserted(predSize, mFlowList.size() - predSize);
    }

    /**
     * Замена всех элементов списка, начиная с fromId, на элементы из items.
     * ID основного списка должны совпадать с ID в items.
     */
    private void updateSublistItems(int fromId, FlowList.FlowSubList[] items) {
        if (BuildConfig.DEBUG) Assert.assertTrue(hasSameIds(fromId, items));
        for (int i = 0; i < items.length; ++i) {
            if (!itemsEquals(i + fromId, items[i])) {
                Flow newFlow = items[i].flow;
                if (BuildConfig.DEBUG) Assert.assertEquals(newFlow.getId(), mFlowList.get(i + fromId).getId());
                mFlowList.set(i + fromId, newFlow);
                mFlowMap.add(newFlow.getId());
                mRelationshipMap.put(newFlow.getId(), items[i].relationship);
                mCallback.onChanged(i + fromId, 1);
            }
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(mFlowList);
        int size =  mRelationshipMap.size();
        Relationship relationships[] = new Relationship[size];
        for (int i = 0; i < size; ++i) {
            relationships[i] = mRelationshipMap.valueAt(i);
        }
        dest.writeArray(relationships);
    }

    protected FlowListManaged(Parcel in) {
        this.mFlowList = in.createTypedArrayList(Flow.CREATOR);
        mFlowMap = new HashSet<>(mFlowList.size());
        for (Flow flow: mFlowList) mFlowMap.add(flow.getId());

        Relationship relationships[] = in.createTypedArray(Relationship.CREATOR);
        mRelationshipMap = new LongSparseArray<>(relationships.length);
        for (Relationship relationship: relationships) {
            mRelationshipMap.put(relationship.getToId(), relationship);
        }
    }

    public static final Parcelable.Creator<FlowListManaged> CREATOR = new Parcelable.Creator<FlowListManaged>() {
        public FlowListManaged createFromParcel(Parcel source) {
            return new FlowListManaged(source);
        }

        public FlowListManaged[] newArray(int size) {
            return new FlowListManaged[size];
        }
    };

    public interface  FlowChangedCallback {
        void onDataSetChanged();
        void onChanged(int position, int count);
        void onInserted(int position, int count);
        void onMoved(int fromPosition, int toPosition);
        void onRemoved(int position, int count);
    }

    private static final FlowChangedCallback DUMMY_CALLBACK = new FlowChangedCallback() {

        @Override
        public void onDataSetChanged() {
        }

        @Override
        public void onChanged(int position, int count) {
        }

        @Override
        public void onInserted(int position, int count) {
        }

        @Override
        public void onMoved(int fromPosition, int toPosition) {
        }

        @Override
        public void onRemoved(int position, int count) {
        }
    };
}
