package com.bloodpressure.app.ui.history;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.bloodpressure.app.data.db.SessionEntity;
import com.bloodpressure.app.data.repository.SignalRepository;

import java.util.List;

public class HistoryViewModel extends AndroidViewModel {

    private final SignalRepository repository;
    private final LiveData<List<SessionEntity>> allSessions;

    public HistoryViewModel(@NonNull Application application) {
        super(application);
        repository = new SignalRepository(application);
        allSessions = repository.getAllSessions();
    }

    public LiveData<List<SessionEntity>> getAllSessions() {
        return allSessions;
    }

    public void deleteSession(long sessionId) {
        repository.deleteSession(sessionId);
    }
}
