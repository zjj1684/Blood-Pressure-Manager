package com.bloodpressure.app.data.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "sessions")
public class SessionEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public long startTime;
    public long endTime;
    public int sampleCount;
    public boolean completed;
}
