package com.bloodpressure.app.data.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface QuestionnaireDao {

    @Insert
    void insert(QuestionnaireEntity questionnaire);

    @Query("SELECT * FROM questionnaires ORDER BY createdAt DESC")
    LiveData<List<QuestionnaireEntity>> getAll();

    @Query("SELECT * FROM questionnaires WHERE id = :id")
    QuestionnaireEntity getById(long id);

    @Query("SELECT * FROM questionnaires ORDER BY createdAt DESC LIMIT 1")
    QuestionnaireEntity getLatest();

    @Query("DELETE FROM questionnaires WHERE id = :id")
    void delete(long id);
}
