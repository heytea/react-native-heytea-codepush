package com.heyteago.codepush.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.heyteago.codepush.data.dao.IndexUpdateDao;
import com.heyteago.codepush.data.dao.TempDao;
import com.heyteago.codepush.data.entity.IndexUpdateEntity;
import com.heyteago.codepush.data.entity.TempEntity;

@Database(entities = {IndexUpdateEntity.class, TempEntity.class}, version = 2, exportSchema = false)
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

    public abstract IndexUpdateDao indexUpdateDao();

    public abstract TempDao tempDao();
}
