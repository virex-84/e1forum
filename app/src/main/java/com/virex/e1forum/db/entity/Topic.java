package com.virex.e1forum.db.entity;


import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
/**
 * Описание таблицы Форум
 */
@Entity(foreignKeys = @ForeignKey(entity = Forum.class,
        parentColumns = "id",
        childColumns = "forum_id"),
        indices = {@Index("forum_id")})
public class Topic {
    //@PrimaryKey(autoGenerate = true)
    //public int _id;

    @PrimaryKey
    public int id;
    public int forum_id;

    public String title;
    public String userName;
    public String lastComment;
    public String comments;
    public boolean isClosed=false;
    public boolean isAttathed=false;
    public int pagesCount;//количество страниц <<<----------------------!!!!


    public long lastmod;  //последняя модификация timestamp
    public boolean isBookMark;      //признак "закладка"
    public int pageID;

    //сравнение для адаптера PagedList (recucleview)
    public static final DiffUtil.ItemCallback<Topic> DIFF_CALLBACK = new DiffUtil.ItemCallback<Topic>(){

        @Override
        public boolean areItemsTheSame(@NonNull Topic oldItem, @NonNull Topic newItem) {
            return oldItem.id == newItem.id && oldItem.forum_id == newItem.forum_id ;
        }

        @Override
        public boolean areContentsTheSame(@NonNull Topic oldItem, @NonNull Topic newItem) {
            return  (oldItem.title.contains(newItem.title) && (oldItem.isBookMark==newItem.isBookMark));
        }
    };
}