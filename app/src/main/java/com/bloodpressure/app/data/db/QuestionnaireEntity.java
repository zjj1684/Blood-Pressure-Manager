package com.bloodpressure.app.data.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "questionnaires")
public class QuestionnaireEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String nickname;
    public String gender;
    public float height;
    public float weight;
    public int scorePinghe;
    public int scoreYinxu;
    public int scoreTanshi;
    public int scoreQiyu;
    public int scoreFujia;
    public long createdAt;
}
