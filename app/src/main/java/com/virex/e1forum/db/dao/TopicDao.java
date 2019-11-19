package com.virex.e1forum.db.dao;

import androidx.lifecycle.LiveData;
import androidx.paging.DataSource;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.virex.e1forum.db.entity.Topic;

import java.util.ArrayList;
import java.util.List;

/**
 * Интерфейс работы с "таблицей" Форумы
 */
@Dao
public interface TopicDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Topic... topic);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ArrayList<Topic> topics);

    @Update(onConflict = OnConflictStrategy.IGNORE)
    void update(Topic... topic);

    @Query("SELECT * FROM topic WHERE forum_id=:forum_id ORDER BY isAttathed desc, lastmod desc")
    LiveData<List<Topic>> dataSource(int forum_id);

    //отображаем сначала прикрепленные, потом добавленные в избранное а потом по дате
    @Query("SELECT * FROM topic WHERE forum_id=:forum_id ORDER BY isAttathed desc, isBookMark desc, lastmod desc")
    DataSource.Factory<Integer, Topic> dataSourcePagedList(int forum_id);

    @Query("SELECT pagesCount FROM topic WHERE forum_id=:forum_id AND id=:topic_id")
    LiveData<Integer> getPagesCount(int forum_id, int topic_id);

    @Query("SELECT * FROM topic WHERE forum_id=:forum_id AND id=:topic_id")
    LiveData<Topic> getTopicLive(int forum_id, int topic_id);

    @Query("SELECT * FROM topic WHERE forum_id=:forum_id AND id=:topic_id")
    Topic getTopic(int forum_id, int topic_id);

    //Room это SQLite, и boolean хранится в виде integer поэтому нельзя применить "isBookMark = not isBookMark"
    @Query("UPDATE topic SET isBookMark = CASE WHEN isBookMark=0 THEN 1 ELSE 0 END WHERE id == :topic_id and forum_id==:forum_id")
    int changeBookMark(int forum_id, int topic_id);

    //Room это SQLite, и boolean хранится в виде integer поэтому нельзя применить "isBookMark = not isBookMark"
    @Query("select count(*) from topic where forum_id==:forum_id")
    LiveData<Integer> getCount(int forum_id);
}