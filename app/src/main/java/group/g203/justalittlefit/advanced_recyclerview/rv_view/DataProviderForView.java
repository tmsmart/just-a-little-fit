package group.g203.justalittlefit.advanced_recyclerview.rv_view;

import android.support.v4.util.Pair;

import com.h6ah4i.android.widget.advrecyclerview.expandable.RecyclerViewExpandableItemManager;
import com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import group.g203.justalittlefit.model.Exercise;
import group.g203.justalittlefit.model.Set;
import group.g203.justalittlefit.util.Constants;
import group.g203.justalittlefit.util.Utils;

public class DataProviderForView extends AbstractExpandableDataProvider {
    private List<Pair<GroupData, List<ChildData>>> mData;

    // for undo group item
    private Pair<GroupData, List<ChildData>> mLastRemovedGroup;
    private int mLastRemovedGroupPosition = -1;

    // for undo child item
    private ChildData mLastRemovedChild;
    private long mLastRemovedChildParentGroupId = -1;
    private int mLastRemovedChildPosition = -1;

    public DataProviderForView(List<Exercise> exercises) {
        mData = new LinkedList<>();

        for (Exercise exercise : exercises) {
            //noinspection UnnecessaryLocalVariable
            final long groupId = mData.size();
            final int groupSwipeReaction = RecyclerViewSwipeManager.REACTION_CAN_SWIPE_LEFT | RecyclerViewSwipeManager.REACTION_CAN_SWIPE_RIGHT;
            final ConcreteGroupData group = new ConcreteGroupData(groupId, exercise.getName(), groupSwipeReaction, exercise);
            final List<ChildData> children = new ArrayList<>();

            List<Set> childSets = new ArrayList<>();
            childSets.addAll(exercise.getSets());

            for (Set set : childSets) {
                final long childId = group.generateNewChildId();
                final int childSwipeReaction = RecyclerViewSwipeManager.REACTION_CAN_SWIPE_LEFT | RecyclerViewSwipeManager.REACTION_CAN_SWIPE_RIGHT;

                children.add(new ConcreteChildData(childId, set.toString(), childSwipeReaction, set));
            }

            mData.add(new Pair<GroupData, List<ChildData>>(group, children));
        }
    }

    @Override
    public int getGroupCount() {
        return mData.size();
    }

    @Override
    public int getChildCount(int groupPosition) {
        return mData.get(groupPosition).second.size();
    }

    @Override
    public GroupData getGroupItem(int groupPosition) {
        if (groupPosition < 0 || groupPosition >= getGroupCount()) {
            throw new IndexOutOfBoundsException("groupPosition = " + groupPosition);
        }

        return mData.get(groupPosition).first;
    }

    @Override
    public ChildData getChildItem(int groupPosition, int childPosition) {
        if (groupPosition < 0 || groupPosition >= getGroupCount()) {
            throw new IndexOutOfBoundsException("groupPosition = " + groupPosition);
        }

        final List<ChildData> children = mData.get(groupPosition).second;

        if (childPosition < 0 || childPosition >= children.size()) {
            throw new IndexOutOfBoundsException("childPosition = " + childPosition);
        }

        return children.get(childPosition);
    }

    public void setChildItem(int groupPosition, int childPosition, Set set) {
        if (groupPosition < 0 || groupPosition >= getGroupCount()) {
            throw new IndexOutOfBoundsException("groupPosition = " + groupPosition);
        }

        final List<ChildData> children = mData.get(groupPosition).second;

        if (childPosition < 0 || childPosition >= children.size()) {
            throw new IndexOutOfBoundsException("childPosition = " + childPosition);
        }

        ChildData childData = children.get(childPosition);
        childData.setExerciseSet(set);
        childData.setText(set.toString());

        children.set(childPosition, childData);
        GroupData groupData = mData.get(groupPosition).first;
        mData.set(groupPosition, new Pair<GroupData, List<ChildData>>(groupData, children));
    }

    @Override
    public void moveGroupItem(int fromGroupPosition, int toGroupPosition) {
        if (fromGroupPosition == toGroupPosition) {
            return;
        }

        final Pair<GroupData, List<ChildData>> item = mData.remove(fromGroupPosition);
        mData.add(toGroupPosition, item);
    }

    @Override
    public void moveChildItem(int fromGroupPosition, int fromChildPosition, int toGroupPosition, int toChildPosition) {
        if ((fromGroupPosition == toGroupPosition) && (fromChildPosition == toChildPosition)) {
            return;
        }

        final Pair<GroupData, List<ChildData>> fromGroup = mData.get(fromGroupPosition);
        final Pair<GroupData, List<ChildData>> toGroup = mData.get(toGroupPosition);

        final ConcreteChildData item = (ConcreteChildData) fromGroup.second.remove(fromChildPosition);

        if (toGroupPosition != fromGroupPosition) {
            // assign a new ID
            final long newId = ((ConcreteGroupData) toGroup.first).generateNewChildId();
            item.setChildId(newId);
        }

        toGroup.second.add(toChildPosition, item);
    }

    @Override
    public void removeGroupItem(int groupPosition) {
        mLastRemovedGroup = mData.remove(groupPosition);
        mLastRemovedGroupPosition = groupPosition;

        mLastRemovedChild = null;
        mLastRemovedChildParentGroupId = -1;
        mLastRemovedChildPosition = -1;
    }

    @Override
    public void removeChildItem(int groupPosition, int childPosition) {
        mLastRemovedChild = mData.get(groupPosition).second.remove(childPosition);
        mLastRemovedChildParentGroupId = mData.get(groupPosition).first.getGroupId();
        mLastRemovedChildPosition = childPosition;

        mLastRemovedGroup = null;
        mLastRemovedGroupPosition = -1;
    }


    @Override
    public long undoLastRemoval() {
        if (mLastRemovedGroup != null) {
            return undoGroupRemoval();
        } else if (mLastRemovedChild != null) {
            return undoChildRemoval();
        } else {
            return RecyclerViewExpandableItemManager.NO_EXPANDABLE_POSITION;
        }
    }

    private long undoGroupRemoval() {
        int insertedPosition;
        if (mLastRemovedGroupPosition >= 0 && mLastRemovedGroupPosition < mData.size()) {
            insertedPosition = mLastRemovedGroupPosition;
        } else {
            insertedPosition = mData.size();
        }

        mData.add(insertedPosition, mLastRemovedGroup);

        mLastRemovedGroup = null;
        mLastRemovedGroupPosition = -1;

        return RecyclerViewExpandableItemManager.getPackedPositionForGroup(insertedPosition);
    }

    private long undoChildRemoval() {
        Pair<GroupData, List<ChildData>> group = null;
        int groupPosition = -1;

        // find the group
        for (int i = 0; i < mData.size(); i++) {
            if (mData.get(i).first.getGroupId() == mLastRemovedChildParentGroupId) {
                group = mData.get(i);
                groupPosition = i;
                break;
            }
        }

        if (group == null) {
            return RecyclerViewExpandableItemManager.NO_EXPANDABLE_POSITION;
        }

        int insertedPosition;
        if (mLastRemovedChildPosition >= 0 && mLastRemovedChildPosition < group.second.size()) {
            insertedPosition = mLastRemovedChildPosition;
        } else {
            insertedPosition = group.second.size();
        }

        group.second.add(insertedPosition, mLastRemovedChild);

        mLastRemovedChildParentGroupId = -1;
        mLastRemovedChildPosition = -1;
        mLastRemovedChild = null;

        return RecyclerViewExpandableItemManager.getPackedPositionForChild(groupPosition, insertedPosition);
    }

    public List<Object> getDataObjects() {
        List<Object> dataObjs = new ArrayList<>();
        for (Pair<GroupData, List<ChildData>> data : mData) {
            Exercise exercise = data.first.getExercise();
            dataObjs.add(exercise);
            List<ChildData> childDataList = data.second;
            for (ChildData childData : childDataList) {
                Set set = childData.getSet();
                dataObjs.add(set);
            }
        }
        return dataObjs;
    }

    public int getExerciseDataCount() {
        int count = 0;
        for (Pair<GroupData, List<ChildData>> data : mData) {
            count++;
        }
        return count;
    }

    public int getSetDataCount(Exercise ex) {
        int count = 0;
        if (ex != null) {
            for (Pair<GroupData, List<ChildData>> data : mData) {
                Exercise exercise = data.first.getExercise();
                if (Utils.ensureValidString(ex.getName()).equals(
                        Utils.ensureValidString(exercise.getName())
                )) {
                    List<ChildData> childDataList = data.second;
                    for (ChildData childData : childDataList) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    public List<String> getExerciseDisplayNames() {
        List<String> exerciseNames = new ArrayList<>();
        for (Pair<GroupData, List<ChildData>> data : mData) {
            Exercise exercise = data.first.getExercise();
            exerciseNames.add(exercise.getName().trim());
        }
        return exerciseNames;
    }

    public int getCount() {
        return getDataObjects().size();
    }

    public int getExerciseCount() {
        return getExerciseDataCount() + Constants.INT_ONE;
    }

    public int getSetCount(Exercise exercise) {
        return getSetDataCount(exercise) + Constants.INT_ONE;
    }

    public List<Object> getOrderedDataObjects() {
        List<Object> dataObjs = new ArrayList<>();
        int groupPosition = 0;
        int childPosition = 0;
        for (Pair<GroupData, List<ChildData>> data : mData) {
            childPosition = 0;
            Exercise exercise = data.first.getExercise();
            exercise.setOrderNumber(groupPosition);
            groupPosition++;
            dataObjs.add(exercise);
            List<ChildData> childDataList = data.second;
            for (ChildData childData : childDataList) {
                Set set = childData.getSet();
                set.setOrderNumber(childPosition);
                childPosition++;
                dataObjs.add(set);
            }
        }
        return dataObjs;
    }

    public static final class ConcreteGroupData extends GroupData {

        private final long mId;
        private final String mText;
        private final int mSwipeReaction;
        private boolean mPinnedToSwipeLeft;
        private long mNextChildId;
        private Exercise mExerciseObj;

        ConcreteGroupData(long id, String text, int swipeReaction, Exercise exerciseObj) {
            mId = id;
            mText = text;
            mSwipeReaction = swipeReaction;
            mNextChildId = 0;
            mExerciseObj = exerciseObj;
        }

        @Override
        public long getGroupId() {
            return mId;
        }

        @Override
        public Exercise getExercise() {
            return mExerciseObj;
        }

        @Override
        public boolean isSectionHeader() {
            return false;
        }

        @Override
        public int getSwipeReactionType() {
            return mSwipeReaction;
        }

        @Override
        public String getText() {
            return mText;
        }

        @Override
        public void setPinnedToSwipeLeft(boolean pinnedToSwipeLeft) {
            mPinnedToSwipeLeft = pinnedToSwipeLeft;
        }

        @Override
        public boolean isPinnedToSwipeLeft() {
            return mPinnedToSwipeLeft;
        }

        public long generateNewChildId() {
            final long id = mNextChildId;
            mNextChildId += 1;
            return id;
        }
    }

    public static final class ConcreteChildData extends ChildData {

        private long mId;
        private String mText;
        private final int mSwipeReaction;
        private boolean mPinnedToSwipeLeft;
        private Set mSetObj;

        ConcreteChildData(long id, String text, int swipeReaction, Set setObj) {
            mId = id;
            mText = text;
            mSwipeReaction = swipeReaction;
            mSetObj = setObj;
        }

        @Override
        public long getChildId() {
            return mId;
        }

        @Override
        public Set getSet() {
            return mSetObj;
        }

        @Override
        public void setExerciseSet(Set set) {
            this.mSetObj = set;
        }

        @Override
        public void setText(String text) {
            mText = text;
        }

        @Override
        public int getSwipeReactionType() {
            return mSwipeReaction;
        }

        @Override
        public String getText() {
            return mText;
        }

        @Override
        public void setPinnedToSwipeLeft(boolean pinnedToSwipeLeft) {
            mPinnedToSwipeLeft = pinnedToSwipeLeft;
        }

        @Override
        public boolean isPinnedToSwipeLeft() {
            return mPinnedToSwipeLeft;
        }

        public void setChildId(long id) {
            this.mId = id;
        }
    }
}
