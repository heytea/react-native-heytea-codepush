package com.heyteago.codepush.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.heyteago.codepush.data.dao.IndexUpdateDao;
import com.heyteago.codepush.data.dao.IndexVersionDao;
import com.heyteago.codepush.data.entity.IndexUpdateEntity;
import com.heyteago.codepush.data.entity.IndexVersionEntity;

@Database(entities = {IndexVersionEntity.class, IndexUpdateEntity.class}, version = 1, exportSchema = false)
public abstract class HtCodePushDb extends RoomDatabase {
    private static HtCodePushDb instance;

    public static void init(Context context) {
        if (instance == null) {
            synchronized (HtCodePushDb.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(), HtCodePushDb.class, "HtCodePushDb")
                            .allowMainThreadQueries()
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
    }

    public static HtCodePushDb getDb(Context context) {
        init(context.getApplicationContext());
        return instance;
    }

    public abstract IndexVersionDao indexVersionDao();

    public abstract IndexUpdateDao indexUpdateDao();
}
