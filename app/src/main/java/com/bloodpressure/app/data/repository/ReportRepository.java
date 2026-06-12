package com.bloodpressure.app.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.bloodpressure.app.data.db.AppDatabase;
import com.bloodpressure.app.data.db.ReportDao;
import com.bloodpressure.app.data.db.ReportEntity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReportRepository {

    private final ReportDao dao;
    private final ExecutorService executor;

    public ReportRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        dao = db.reportDao();
        executor = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<ReportEntity>> getAll() {
        return dao.getAll();
    }

    public void insert(ReportEntity entity) {
        executor.execute(() -> dao.insert(entity));
    }

    public void delete(long id) {
        executor.execute(() -> dao.delete(id));
    }
}
