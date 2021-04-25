package com.heyteago.codepush.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.heyteago.codepush.data.entity.IndexUpdateEntity;

@Dao
public interface IndexUpdateDao {

    @Query("SELECT * FROM index_update WHERE is_fail = :isFail AND version_name = :versionName ORDER BY version_code DESC")
    IndexUpdateEntity[] findByIsFailAndVersionName(boolean isFail, String versionName);

    @Query("SELECT * FROM index_update ORDER BY version_code DESC")
    IndexUpdateEntity[] findAll();

    @Query("SELECT * FROM index_update WHERE id = :id")
    IndexUpdateEntity[] findById(long id);

    @Query("SELECT * FROM index_update WHERE version_name = :versionName ORDER BY version_code DESC")
    IndexUpdateEntity[] findByVersionName(String versionName);

    @Update
    void updateEntities(IndexUpdateEntity ...indexUpdateEntities);

    @Delete
    void deleteEntities(IndexUpdateEntity ...indexUpdateEntities);

    @Insert
    void insertEntities(IndexUpdateEntity... indexUpdateEntities);

    @Query("DELETE FROM `index_update` WHERE version_code = :versionCode")
    void deleteByVersionCode(int versionCode);
}
