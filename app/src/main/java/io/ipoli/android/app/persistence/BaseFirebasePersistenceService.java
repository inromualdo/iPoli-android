package io.ipoli.android.app.persistence;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.otto.Bus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.ipoli.android.Constants;
import io.ipoli.android.app.App;
import io.ipoli.android.app.utils.StringUtils;
import io.ipoli.android.quest.persistence.OnDataChangedListener;
import rx.Observable;

/**
 * Created by Venelin Valkov <venelin@curiousily.com>
 * on 3/25/16.
 */
public abstract class BaseFirebasePersistenceService<T extends PersistedObject> implements PersistenceService<T> {

    protected final FirebaseDatabase database;
    protected final Bus eventBus;
    private final Map<ValueEventListener, Query> valueListeners;
    protected final Map<ChildEventListener, Query> childListeners;
    private final Map<OnDataChangedListener<?>, ValueEventListener> listenerToValueListener;
    private DatabaseReference playerRef;

    public BaseFirebasePersistenceService(Bus eventBus) {
        this.eventBus = eventBus;
        this.database = FirebaseDatabase.getInstance();
        this.valueListeners = new HashMap<>();
        this.childListeners = new HashMap<>();
        this.listenerToValueListener = new HashMap<>();
        this.playerRef = null;
    }

    @Override
    public void save(T obj) {
        DatabaseReference collectionRef = getCollectionReference();
        boolean isNew = StringUtils.isEmpty(obj.getId());
        if (!isNew) {
            obj.markUpdated();
        }
        DatabaseReference objRef = isNew ?
                collectionRef.push() :
                collectionRef.child(obj.getId());
        obj.setId(objRef.getKey());
        objRef.setValue(obj);
    }

    @Override
    public void findById(String id, OnDataChangedListener<T> listener) {
        if (StringUtils.isEmpty(id)) {
            listener.onDataChanged(null);
            return;
        }
        DatabaseReference dbRef = getPlayerReference().child(getCollectionName()).child(id);
        listenForSingleModelChange(dbRef, listener);
    }

    @Override
    public void listenById(String id, OnDataChangedListener<T> listener) {
        if (StringUtils.isEmpty(id)) {
            listener.onDataChanged(null);
            return;
        }
        DatabaseReference dbRef = getPlayerReference().child(getCollectionName()).child(id);
        listenForModelChange(dbRef, listener);
    }

    @Override
    public void delete(T object) {
        getCollectionReference().child(object.getId()).removeValue();
    }

    @Override
    public void removeAllListeners() {
        for (ValueEventListener valueEventListener : valueListeners.keySet()) {
            Query query = valueListeners.get(valueEventListener);
            query.removeEventListener(valueEventListener);
        }
        valueListeners.clear();

        for (ChildEventListener childEventListener : childListeners.keySet()) {
            Query query = childListeners.get(childEventListener);
            query.removeEventListener(childEventListener);
        }
        childListeners.clear();
    }

    @Override
    public void removeDataChangedListener(OnDataChangedListener<?> listener) {
        if (!listenerToValueListener.containsKey(listener)) {
            return;
        }
        ValueEventListener valueEventListener = listenerToValueListener.get(listener);
        Query query = valueListeners.get(valueEventListener);
        query.removeEventListener(valueEventListener);

        valueListeners.remove(valueEventListener);
        listenerToValueListener.remove(listener);
    }

    protected abstract Class<T> getModelClass();

    protected abstract String getCollectionName();

    protected abstract DatabaseReference getCollectionReference();

    protected DatabaseReference getPlayerReference() {
        if (playerRef == null) {
            playerRef = database.getReference(Constants.API_VERSION).child("players").child(App.getPlayerId());
            playerRef.keepSynced(true);
        }
        return playerRef;
    }

    protected void listenForListChange(Query query, OnDataChangedListener<List<T>> listener) {
        listenForQuery(query, createListListener(listener), listener);
    }

    protected void listenForListChange(Query query, OnDataChangedListener<List<T>> listener, QueryFilter<T> queryFilter) {
        listenForQuery(query, createListListener(listener, queryFilter), listener);
    }

    protected void listenForListChange(Query query, OnDataChangedListener<List<T>> listener, QueryFilter<T> queryFilter, QuerySort<T> querySort) {
        listenForQuery(query, createSortedListListener(listener, queryFilter, querySort), listener);
    }

    protected void listenForModelChange(Query query, OnDataChangedListener<T> listener) {
        listenForQuery(query, createModelListener(listener), listener);
    }

    protected void listenForQuery(Query query, ValueEventListener valueListener, OnDataChangedListener<?> listener) {
        listenerToValueListener.put(listener, valueListener);
        valueListeners.put(valueListener, query);
        query.addValueEventListener(valueListener);
    }

    protected void listenForSingleChange(Query query, ValueEventListener valueListener) {
        query.addListenerForSingleValueEvent(valueListener);
    }

    protected void listenForSingleListChange(Query query, OnDataChangedListener<List<T>> listener, QueryFilter<T> queryFilter) {
        query.addListenerForSingleValueEvent(createListListener(listener, queryFilter));
    }

    protected void listenForSingleListChange(Query query, OnDataChangedListener<List<T>> listener, QueryFilter<T> queryFilter, QuerySort<T> querySort) {
        query.addListenerForSingleValueEvent(createSortedListListener(listener, queryFilter, querySort));
    }

    protected void listenForSingleListChange(Query query, OnDataChangedListener<List<T>> listener) {
        listenForSingleListChange(query, listener, null);
    }

    protected void listenForSingleModelChange(Query query, OnDataChangedListener<T> listener) {
        query.addListenerForSingleValueEvent(createModelListener(listener));
    }

    protected void listenForCountChange(Query query, OnDataChangedListener<Long> listener) {
        listenForQuery(query, createCountListener(listener), listener);
    }

    protected void listenForCountChange(Query query, OnDataChangedListener<Long> listener, QueryFilter<T> queryFilter) {
        listenForQuery(query, createCountListener(listener, queryFilter), listener);
    }

    protected void listenForSingleCountChange(Query query, OnDataChangedListener<Long> listener) {
        query.addListenerForSingleValueEvent(createCountListener(listener));
    }

    protected void listenForSingleCountChange(Query query, OnDataChangedListener<Long> listener, QueryFilter<T> queryFilter) {
        query.addListenerForSingleValueEvent(createCountListener(listener, queryFilter));
    }

    protected List<T> getListFromMapSnapshot(DataSnapshot dataSnapshot) {
        if (dataSnapshot.getChildrenCount() == 0) {
            return new ArrayList<>();
        }

        return new ArrayList<>(dataSnapshot.getValue(getGenericMapIndicator()).values());
    }


    protected abstract GenericTypeIndicator<Map<String, T>> getGenericMapIndicator();

    protected abstract GenericTypeIndicator<List<T>> getGenericListIndicator();

    protected ValueEventListener createListListener(OnDataChangedListener<List<T>> listener) {
        return createListListener(listener, null);
    }

    protected ValueEventListener createListListener(OnDataChangedListener<List<T>> listener, QueryFilter<T> queryFilter) {
        return new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<T> data = getListFromMapSnapshot(dataSnapshot);
                if (queryFilter == null) {
                    listener.onDataChanged(data);
                    return;
                }
                List<T> filteredData = queryFilter.filter(Observable.from(data)).toList().toBlocking().single();
                listener.onDataChanged(filteredData);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
    }

    protected ValueEventListener createSortedListListener(OnDataChangedListener<List<T>> listener, QueryFilter<T> queryFilter, QuerySort<T> querySort) {
        return new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<T> data = getListFromMapSnapshot(dataSnapshot);
                Observable<T> observableData = Observable.from(data);
                if (queryFilter != null) {
                    observableData = queryFilter.filter(observableData);
                }

                List<T> filteredData;
                if (querySort != null) {
                    filteredData = observableData.toSortedList(querySort::sort).toBlocking().single();
                } else {
                    filteredData = observableData.toSortedList().toBlocking().single();
                }
                listener.onDataChanged(filteredData);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
    }

    protected ValueEventListener createCountListener(OnDataChangedListener<Long> listener) {
        return createCountListener(listener, null);
    }

    protected ValueEventListener createCountListener(OnDataChangedListener<Long> listener, QueryFilter<T> queryFilter) {
        return new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (queryFilter == null) {

                    listener.onDataChanged(dataSnapshot.getChildrenCount());
                    return;
                }
                List<T> data = getListFromMapSnapshot(dataSnapshot);
                List<T> filteredData = queryFilter.filter(Observable.from(data)).toList().toBlocking().single();
                listener.onDataChanged((long) filteredData.size());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
    }

    protected ValueEventListener createModelListener(OnDataChangedListener<T> listener) {
        return new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                listener.onDataChanged(dataSnapshot.getValue(getModelClass()));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
    }

    public interface QueryFilter<T> {
        Observable<T> filter(Observable<T> data);
    }

    public interface QuerySort<T> {
        int sort(T obj1, T obj2);
    }
}
