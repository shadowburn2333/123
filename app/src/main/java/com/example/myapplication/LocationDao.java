package com.example.myapplication;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface LocationDao {

    @Insert
    void insertLocation(LocationEntity location);

    @Query("SELECT * FROM location_table")
    List<LocationEntity> getAllLocations();
}
