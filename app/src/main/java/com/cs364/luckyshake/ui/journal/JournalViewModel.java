package com.cs364.luckyshake.ui.journal;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.cs364.luckyshake.data.HistoryEntry;
import com.cs364.luckyshake.data.HistoryRepository;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ViewModel for managing the state of the Journal (history) screen.
 * Retains filter selections across configuration changes (e.g. screen rotation).
 */
public class JournalViewModel extends AndroidViewModel {

    public enum Filter {
        ALL,
        ACCEPTED,
        REDRAWN
    }

    private final HistoryRepository repository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final MutableLiveData<Filter> filter = new MutableLiveData<>(Filter.ALL);
    private final MutableLiveData<List<HistoryEntry>> entries = new MutableLiveData<>();
    private final MutableLiveData<Integer> totalCount = new MutableLiveData<>();

    private final AtomicLong refreshGeneration = new AtomicLong(0);

    public JournalViewModel(@NonNull Application application) {
        super(application);
        repository = new HistoryRepository(application);
        refresh();
    }

    public LiveData<Filter> getFilter() {
        return filter;
    }

    public LiveData<List<HistoryEntry>> getEntries() {
        return entries;
    }

    public LiveData<Integer> getTotalCount() {
        return totalCount;
    }

    public void setFilter(Filter f) {
        if (f != null && filter.getValue() != f) {
            filter.setValue(f);
            refresh();
        }
    }

    public void refresh() {
        final long requestGen = refreshGeneration.incrementAndGet();
        final Filter currentFilter = filter.getValue();
        executor.execute(() -> executeQueryAndPublish(requestGen, currentFilter));
    }

    public void delete(long id) {
        final long requestGen = refreshGeneration.incrementAndGet();
        executor.execute(() -> {
            repository.delete(id);
            executeQueryAndPublish(requestGen, filter.getValue());
        });
    }

    public void clearAll() {
        final long requestGen = refreshGeneration.incrementAndGet();
        executor.execute(() -> {
            repository.clearAll();
            executeQueryAndPublish(requestGen, filter.getValue());
        });
    }

    private void executeQueryAndPublish(long requestGen, Filter currentFilter) {
        String dbStatus = null;
        if (currentFilter == Filter.ACCEPTED) {
            dbStatus = HistoryEntry.STATUS_ACCEPTED;
        } else if (currentFilter == Filter.REDRAWN) {
            dbStatus = HistoryEntry.STATUS_REDRAWN;
        }

        List<HistoryEntry> list = repository.list(dbStatus, 0);
        int total = repository.count(null);

        // Discard stale query results if filter or refresh generation changed in the meantime
        if (requestGen == refreshGeneration.get()) {
            entries.postValue(list);
            totalCount.postValue(total);
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }
}
