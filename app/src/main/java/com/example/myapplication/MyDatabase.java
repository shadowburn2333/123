package com.example.myapplication;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.example.myapplication.LocationDao;
import com.example.myapplication.LocationEntity;
import com.example.myapplication.PowerDao;
import com.example.myapplication.PowerEntity;

@Database(entities = {LocationEntity.class, PowerEntity.class}, version = 1)
public abstract class MyDatabase extends RoomDatabase {
    public abstract LocationDao locationDao();
    public abstract PowerDao powerDao();
}
