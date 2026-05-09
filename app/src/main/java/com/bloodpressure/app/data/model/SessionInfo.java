package com.bloodpressure.app.data.model;

public class SessionInfo {
    private final long id;
    private final long startTime;
    private final long endTime;
    private final int sampleCount;
    private final boolean completed;

    public SessionInfo(long id, long startTime, long endTime, int sampleCount, boolean completed) {
        this.id = id;
        this.startTime = startTime;
        this.endTime = endTime;
        this.sampleCount = sampleCount;
        this.completed = completed;
    }

    public long getId() { return id; }
    public long getStartTime() { return startTime; }
    public long getEndTime() { return endTime; }
    public int getSampleCount() { return sampleCount; }
    public boolean isCompleted() { return completed; }
}
