package com.virex.e1forum.db.dao;

import androidx.lifecycle.LiveData;
import androidx.paging.DataSource;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.virex.e1forum.db.entity.Post;

import java.util.ArrayList;
import java.util.List;

/**
 * Интерфейс работы с "таблицей" Форумы
 */
@Dao
public interface PostDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Post... post);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ArrayList<Post> posts);

    @Update(onConflict = OnConflictStrategy.IGNORE)
    void update(Post... post);

    @Query("SELECT * FROM post WHERE forum_id=:forum_id AND topic_id=:topic_id ORDER BY lastmod asc")
    LiveData<List<Post>> dataSource(int forum_id, int topic_id);

    //@Query("SELECT * FROM post WHERE forum_id=:forum_id AND topic_id=:topic_id ORDER BY lastmod asc")
    //id поста актуальнее т.к. lastmod можно не всегда адекватно вычислить по дате
    @Query("SELECT * FROM post WHERE forum_id=:forum_id AND topic_id=:topic_id ORDER BY lastmod asc")
    DataSource.Factory<Integer, Post> dataSourcePagedList(int forum_id, int topic_id);

    @Query("SELECT COUNT(id) FROM post WHERE forum_id=:forum_id AND topic_id=:topic_id")
    int getCount(int forum_id, int topic_id);

}