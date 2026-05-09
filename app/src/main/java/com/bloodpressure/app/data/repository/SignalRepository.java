package com.bloodpressure.app.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.bloodpressure.app.data.db.AppDatabase;
import com.bloodpressure.app.data.db.SessionEntity;
import com.bloodpressure.app.data.db.SignalDao;
import com.bloodpressure.app.data.db.SignalEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SignalRepository {

    private final SignalDao signalDao;
    private final ExecutorService executor;

    public SignalRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        signalDao = db.signalDao();
        executor = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<SessionEntity>> getAllSessions() {
        return signalDao.getAllSessions();
    }

    public LiveData<List<SignalEntity>> getSignalsBySession(long sessionId) {
        return signalDao.getSignalsBySession(sessionId);
    }

    public void createSession(SessionEntity session) {
        executor.execute(() -> signalDao.insertSession(session));
    }

    public void insertSignals(long sessionId, List<int[]> samples) {
        executor.execute(() -> {
            List<SignalEntity> entities = new ArrayList<>();
            long now = System.currentTimeMillis();
            for (int i = 0; i < samples.size(); i++) {
                SignalEntity entity = new SignalEntity();
                entity.sessionId = sessionId;
                entity.timestamp = now + i;
                entity.ppg = samples.get(i)[0];
                entity.ecg = samples.get(i)[1];
                entities.add(entity);
            }
            signalDao.insertSignals(entities);
        });
    }

    public void updateSession(long sessionId, long endTime, int sampleCount, boolean completed) {
        executor.execute(() -> signalDao.updateSession(sessionId, endTime, sampleCount, completed));
    }

    public void deleteSession(long sessionId) {
        executor.execute(() -> {
            signalDao.deleteSignalsBySession(sessionId);
            signalDao.deleteSession(sessionId);
        });
    }
}
