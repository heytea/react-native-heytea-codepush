package com.heyteago.codepush.data.dao;

import androidx.annotation.NonNull;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.heyteago.codepush.data.entity.IndexVersionEntity;


@Dao
public interface IndexVersionDao {
    @Query("SELECT * FROM `index_version`")
    IndexVersionEntity[] findAll();

    @Query("SELECT * FROM `index_version` WHERE enable = :enable ORDER BY version_code DESC")
    IndexVersionEntity[] findByEnable(boolean enable);

    @Query("SELECT * FROM `index_version` WHERE version_code = :versionCode")
    IndexVersionEntity[] findByVersionCode(int versionCode);

    @Update
    void updateIndexVersions(@NonNull IndexVersionEntity ...indexVersionEntities);

    @Insert
    void insertIndexVersions(@NonNull IndexVersionEntity ...indexVersionEntities);

}
