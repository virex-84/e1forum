package com.virex.e1forum.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.virex.e1forum.db.entity.User;

import java.util.ArrayList;
import java.util.List;

@Dao
public interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(User... user);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ArrayList<User> users);

    @Update(onConflict = OnConflictStrategy.IGNORE)
    void update(User... user);

    @Query("SELECT * FROM user ORDER BY id ASC")
    LiveData<List<User>> dataSource();

    //@Query("SELECT * FROM user WHERE nick LIKE '%' || :nick || '%' COLLATE UNICODE")
    @Query("SELECT * FROM user WHERE nick LIKE :nick")
    //LiveData<User> getUser(String nick);
    User getUser(String nick);

    @Query("SELECT * FROM user WHERE nick LIKE :nick")
    LiveData<User> getUserLive(String nick);

    @Query("SELECT * FROM user")
    LiveData<User> getUsers();
}