package com.virex.e1forum.db.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.virex.e1forum.db.dao.ForumDao;
import com.virex.e1forum.db.dao.PostDao;
import com.virex.e1forum.db.dao.TopicDao;
import com.virex.e1forum.db.dao.UserDao;
import com.virex.e1forum.db.entity.Forum;
import com.virex.e1forum.db.entity.Post;
import com.virex.e1forum.db.entity.Topic;
import com.virex.e1forum.db.entity.User;

/**
 * База данных Room
 */
@Database(entities = {Forum.class, Topic.class, Post.class, User.class}, version = 1, exportSchema = false)
public abstract class AppDataBase extends RoomDatabase {
    private static AppDataBase instance;

    private static final String E1FORUM_DB = "E1FORUM.db";

    public abstract ForumDao forumDao();
    public abstract TopicDao topicDao();
    public abstract PostDao postDao();
    public abstract UserDao userDao();

    public static AppDataBase getAppDatabase(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                    AppDataBase.class,
                    E1FORUM_DB)

                    //разрешить работать с БД в главном потоке - не нужно (у нас работает через WorkManager)
                    //.allowMainThreadQueries()

                    //миграция БД - понадобится позже
                    //.addMigrations(MIGRATION_1_2)
                    //.addMigrations(MIGRATION_2_3)

                    //убиваем всё если схема данных не совпадает
                    .fallbackToDestructiveMigration()

                    .build();
        }
        return instance;
    }

    /*
    //пример миграции
    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(final SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE topic ADD COLUMN isBookMark INTEGER DEFAULT 0 NOT NULL");
        }
    };

    //пример миграции
    private static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(final SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE topic ADD COLUMN countBookmarkedPages INTEGER DEFAULT 0 NOT NULL");
        }
    };
    */
}
