package com.heyteago.codepush.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.heyteago.codepush.data.entity.IndexUpdateEntity;

@Dao
public interface IndexUpdateDao {
    @Query("SELECT * FROM `index_update` WHERE is_delete = :isDelete ORDER BY version_code DESC")
    IndexUpdateEntity[] findByIsDelete(boolean isDelete);

    @Query("SELECT * FROM `index_update`")
    IndexUpdateEntity[] findAll();

    @Update
    void updateEntities(IndexUpdateEntity ...indexUpdateEntities);

    @Delete
    void deleteEntities(IndexUpdateEntity ...indexUpdateEntities);

    @Insert
    void insertEntities(IndexUpdateEntity... indexUpdateEntities);
}
