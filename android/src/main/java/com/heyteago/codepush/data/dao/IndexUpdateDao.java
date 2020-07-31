package com.heyteago.codepush.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.heyteago.codepush.data.entity.IndexUpdateEntity;

@Dao
public interface IndexUpdateDao {

    @Query("SELECT * FROM index_update WHERE is_fail = :isFail OR is_temp = :isTemp ORDER BY version_code DESC")
    IndexUpdateEntity[] findByIsFailOrIsTemp(boolean isFail, boolean isTemp);

    @Query("SELECT * FROM index_update WHERE is_temp = :isTemp ORDER BY version_code DESC")
    IndexUpdateEntity[] findByIsTemp(boolean isTemp);

    @Query("SELECT * FROM `index_update` ORDER BY version_code DESC")
    IndexUpdateEntity[] findAll();

    @Query("SELECT * FROM index_update WHERE id = :id")
    IndexUpdateEntity[] findById(long id);

    @Query("SELECT * FROM index_update WHERE version_name = :versionName")
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
