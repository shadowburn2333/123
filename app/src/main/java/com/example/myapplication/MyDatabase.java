package com.example.myapplication;
import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {LocationEntity.class, PowerEntity.class}, version = 1)
public abstract class MyDatabase extends RoomDatabase {
    public abstract LocationDao locationDao();
    public abstract PowerDao powerDao();
}
