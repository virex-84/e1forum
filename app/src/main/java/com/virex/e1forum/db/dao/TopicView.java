package com.virex.e1forum.db.dao;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.room.DatabaseView;

import com.virex.e1forum.db.entity.Topic;


@DatabaseView(
        "SELECT * "+
        ", (SELECT COUNT(*) FROM post where forum_id=topic.forum_id and topic_id=topic.id) as commentsloaded "+
        " FROM topic ")

public class TopicView extends Topic {
    public long commentsloaded; //количество загруженных постов

    //сравнение для адаптера PagedList (recucleview)
    public static final DiffUtil.ItemCallback<TopicView> DIFF_CALLBACK = new DiffUtil.ItemCallback<TopicView>(){

        @Override
        public boolean areItemsTheSame(@NonNull TopicView oldItem, @NonNull TopicView newItem) {
            return oldItem.id == newItem.id && oldItem.forum_id == newItem.forum_id ;
        }

        @Override
        public boolean areContentsTheSame(@NonNull TopicView oldItem, @NonNull TopicView newItem) {
            //return  (oldItem.title.contains(newItem.title) && (oldItem.isBookMark==newItem.isBookMark));
            return oldItem==newItem;
        }
    };
}
