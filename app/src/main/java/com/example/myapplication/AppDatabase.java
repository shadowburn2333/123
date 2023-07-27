package com.example.myapplication;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.example.myapplication.LocationDao;
import com.example.myapplication.LocationEntity;

@Database(entities = {LocationEntity.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract LocationDao locationDao();
}
