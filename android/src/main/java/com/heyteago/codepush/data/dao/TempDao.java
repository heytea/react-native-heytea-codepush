package com.heyteago.codepush.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.heyteago.codepush.data.entity.TempEntity;

@Dao
public interface TempDao {
    @Query("SELECT * FROM `temp` WHERE `key` = :key")
    TempEntity[] findByKey(String key);

    @Update
    void updateTempEntities(TempEntity... tempEntities);

    @Insert
    void insertTempEntities(TempEntity... tempEntities);
}
