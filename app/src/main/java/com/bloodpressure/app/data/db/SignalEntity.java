package com.bloodpressure.app.data.db;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "signals", indices = {@Index("sessionId")})
public class SignalEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public long sessionId;
    public long timestamp;
    public int ppg;
    public int ecg;
}
