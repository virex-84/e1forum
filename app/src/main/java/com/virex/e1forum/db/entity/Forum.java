package com.virex.e1forum.db.entity;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
/**
 * Описание таблицы Форум
 */
@Entity
public class Forum {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String title;

    public boolean isBookMark;      //признак "закладка"

    //сравнение для адаптера PagedList (recucleview)
    public static final DiffUtil.ItemCallback<Forum> DIFF_CALLBACK = new DiffUtil.ItemCallback<Forum>(){

        @Override
        public boolean areItemsTheSame(@NonNull Forum oldItem, @NonNull Forum newItem) {
            return oldItem.id == newItem.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull Forum oldItem, @NonNull Forum newItem) {
             return oldItem.title.equals(newItem.title);
        }
    };
}
