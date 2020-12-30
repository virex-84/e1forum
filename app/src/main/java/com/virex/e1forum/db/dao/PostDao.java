package com.virex.e1forum.db.dao;

import androidx.lifecycle.LiveData;
import androidx.paging.DataSource;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
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
    //@Query("SELECT * FROM post WHERE forum_id=:forum_id AND topic_id=:topic_id ORDER BY lastmod asc")
    //DataSource.Factory<Integer, Post> dataSourcePagedList(int forum_id, int topic_id);

    @Transaction
    @Query("SELECT * FROM PostView WHERE forum_id=:forum_id AND topic_id=:topic_id ORDER BY lastmod asc")
    DataSource.Factory<Integer, PostView> dataSourcePagedList(int forum_id, int topic_id);

    //рабочее fts
    //@Query("SELECT * FROM post WHERE (forum_id=:forum_id AND topic_id=:topic_id) AND (post MATCH 'user:*'||:filter||'*' OR post MATCH 'text:*'||:filter||'*') ORDER BY lastmod asc")
    //DataSource.Factory<Integer, Post> dataSourcePagedList(int forum_id, int topic_id, String filter);

    //userSearch,textSearch и filter должны быть LOWER
    //@Query("SELECT * FROM post WHERE (forum_id=:forum_id AND topic_id=:topic_id) AND ((userSearch LIKE '%'||:filter||'%') OR (textSearch LIKE '%'||:filter||'%')) ORDER BY lastmod asc")
    //DataSource.Factory<Integer, Post> dataSourcePagedList(int forum_id, int topic_id, String filter);
    @Transaction
    @Query("SELECT * FROM PostView WHERE (forum_id=:forum_id AND topic_id=:topic_id) AND ((userSearch LIKE '%'||:filter||'%') OR (textSearch LIKE '%'||:filter||'%')) ORDER BY lastmod asc")
    DataSource.Factory<Integer, PostView> dataSourcePagedList(int forum_id, int topic_id, String filter);

    //@RawQuery(observedEntities = Post.class)
    //DataSource.Factory<Integer, Post>dataSourcePagedListRaw(SupportSQLiteQuery query);

    @Query("SELECT COUNT(*) FROM post WHERE forum_id=:forum_id AND topic_id=:topic_id")
    int getCount(int forum_id, int topic_id);

    @Query("SELECT * FROM post WHERE id=:post_id")
    Post get(int post_id);
}