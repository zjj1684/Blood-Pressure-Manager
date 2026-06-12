package com.bloodpressure.app.data.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "reports")
public class ReportEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String nickname;
    public long createdAt;
    public String constitutionType;
    public int confidence;
    public String constitutionType2;
    public int confidence2;
    public int systolic;
    public int diastolic;
    public int heartRate;
    public String dietaryAdvice;
    public String acupressureAdvice;
    public String earAcupressureAdvice;
}
