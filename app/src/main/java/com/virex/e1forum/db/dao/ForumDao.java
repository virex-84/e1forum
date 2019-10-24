package com.virex.e1forum.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.virex.e1forum.db.entity.Forum;

import java.util.ArrayList;
import java.util.List;

/**
 * Интерфейс работы с "таблицей" Форумы
 */
@Dao
public interface ForumDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Forum... forum);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ArrayList<Forum> topics);

    @Update(onConflict = OnConflictStrategy.IGNORE)
    void update(Forum... forum);

    @Query("SELECT * FROM forum ORDER BY  isBookMark desc, id ASC")
    LiveData<List<Forum>> dataSource();

    @Query("SELECT * FROM forum WHERE id=:forum_id")
    Forum getForum(int forum_id);

    @Query("UPDATE forum SET isBookMark = CASE WHEN isBookMark=0 THEN 1 ELSE 0 END WHERE id ==:forum_id")
    int changeBookMark(int forum_id);
}