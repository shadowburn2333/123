package com.example.myapplication;

import androidx.room.Dao;
import androidx.room.Insert;

@Dao
public interface PowerDao {
    @Insert
    void insertPower(PowerEntity powerEntity);
}
