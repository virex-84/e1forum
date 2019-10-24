package com.virex.e1forum.db.entity;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class User {
    //autoGenerate = true обязательно иначе пользователь не сохранится в базе
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int idForum;

    @ColumnInfo(collate = ColumnInfo.UNICODE)
    public String nick;

    public String link;

    public String actionLK;
    public String actionMail;

    public String avatarURL;

    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    public byte[] avatarIMG;

    //сравнение для адаптера PagedList (recucleview)
    public static final DiffUtil.ItemCallback<User> DIFF_CALLBACK = new DiffUtil.ItemCallback<User>(){

        @Override
        public boolean areItemsTheSame(@NonNull User oldItem, @NonNull User newItem) {
            return oldItem.id == newItem.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull User oldItem, @NonNull User newItem) {
            return oldItem.link.equals(newItem.link);
        }
    };
}
