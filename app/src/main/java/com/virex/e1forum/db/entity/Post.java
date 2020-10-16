package com.virex.e1forum.db.entity;


import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Описание таблицы Форум
 */
@Entity(tableName = "Post",
        foreignKeys = @ForeignKey(entity = Topic.class,
        parentColumns = "id",
        childColumns = "topic_id"),
        indices = {@Index("topic_id")
        }
)
//@Fts4(contentEntity = Post.class)
//@Fts4(tokenizer = FtsOptions.TOKENIZER_UNICODE61)
//@Fts4
public class Post {
    @PrimaryKey(autoGenerate = true)
    public int id;


    /*
    //для fts
    //@ColumnInfo()
    //public int id;

    //@PrimaryKey
    //@Ignore
    //public int rowid;
     */

    public int forum_id;
    public int topic_id;

    @ColumnInfo(collate = ColumnInfo.UNICODE)
    public String user;
    public String userSearch; //для поиска lower

    public String userAvatarURL;

    @ColumnInfo(collate = ColumnInfo.UNICODE)
    public String text;
    public String textSearch;//для поиска lower

    public int carmaPlus;
    public int carmaMinus;

    public boolean disableCarma=false; //доступность "плюсомёта"

    public long lastmod;  //последняя модификация timestamp

    //сравнение для адаптера PagedList (recucleview)
    public static final DiffUtil.ItemCallback<Post> DIFF_CALLBACK = new DiffUtil.ItemCallback<Post>(){

        @Override
        public boolean areItemsTheSame(@NonNull Post oldItem, @NonNull Post newItem) {
            return (oldItem.id == newItem.id) && (oldItem.topic_id == newItem.topic_id) && (oldItem.forum_id == newItem.forum_id) ;
        }

        @Override
        public boolean areContentsTheSame(@NonNull Post oldItem, @NonNull Post newItem) {
            //return  (oldItem.text.contains(newItem.text) && (oldItem.carmaPlus==newItem.carmaPlus) && (oldItem.carmaMinus==newItem.carmaMinus) && (oldItem.lastmod==newItem.lastmod) && (oldItem.disableCarma==newItem.disableCarma));
            return oldItem==newItem;
        }
    };
}