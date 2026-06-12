package com.bloodpressure.app.data.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ReportDao {

    @Insert
    void insert(ReportEntity report);

    @Query("SELECT * FROM reports ORDER BY createdAt DESC")
    LiveData<List<ReportEntity>> getAll();

    @Query("DELETE FROM reports WHERE id = :id")
    void delete(long id);
}
