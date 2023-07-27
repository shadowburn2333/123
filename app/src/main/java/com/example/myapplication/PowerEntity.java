package com.example.myapplication;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "power_table")
public class PowerEntity {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private long timestamp;
    private String action;

    public PowerEntity(long timestamp, String action) {
        this.timestamp = timestamp;
        this.action = action;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}
