package com.bloodpressure.app.data.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

@Dao
public interface SignalDao {

    @Insert
    void insertSession(SessionEntity session);

    @Insert
    @Transaction
    void insertSignals(List<SignalEntity> signals);

    @Query("SELECT * FROM sessions ORDER BY startTime DESC")
    LiveData<List<SessionEntity>> getAllSessions();

    @Query("SELECT * FROM sessions WHERE id = :sessionId")
    SessionEntity getSessionById(long sessionId);

    @Query("SELECT * FROM signals WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    LiveData<List<SignalEntity>> getSignalsBySession(long sessionId);

    @Query("SELECT * FROM signals WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    List<SignalEntity> getSignalsBySessionSync(long sessionId);

    @Query("UPDATE sessions SET endTime = :endTime, sampleCount = :sampleCount, completed = :completed WHERE id = :sessionId")
    void updateSession(long sessionId, long endTime, int sampleCount, boolean completed);

    @Query("DELETE FROM sessions WHERE id = :sessionId")
    void deleteSession(long sessionId);

    @Query("DELETE FROM signals WHERE sessionId = :sessionId")
    void deleteSignalsBySession(long sessionId);
}
