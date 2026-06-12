package com.bloodpressure.app.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.bloodpressure.app.data.db.AppDatabase;
import com.bloodpressure.app.data.db.QuestionnaireDao;
import com.bloodpressure.app.data.db.QuestionnaireEntity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QuestionnaireRepository {

    private final QuestionnaireDao dao;
    private final ExecutorService executor;

    public QuestionnaireRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        dao = db.questionnaireDao();
        executor = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<QuestionnaireEntity>> getAll() {
        return dao.getAll();
    }

    public void insert(QuestionnaireEntity entity) {
        executor.execute(() -> dao.insert(entity));
    }

    public void delete(long id) {
        executor.execute(() -> dao.delete(id));
    }

    public QuestionnaireEntity getLatest() {
        return dao.getLatest();
    }
}
