package com.virex.e1forum.db.dao;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.room.DatabaseView;
import androidx.room.Relation;

import com.virex.e1forum.db.entity.Post;
import com.virex.e1forum.db.entity.User;

@DatabaseView(
        "SELECT * "+
                " FROM post "
                //"left join user as userInfo on userInfo.nick=post.user"
)

public class PostView extends Post {


    @Relation(parentColumn = "user", entityColumn = "nick", entity = User.class)
    public User usr;

    //сравнение для адаптера PagedList (recucleview)
    public static final DiffUtil.ItemCallback<PostView> DIFF_CALLBACK = new DiffUtil.ItemCallback<PostView>(){

        @Override
        public boolean areItemsTheSame(@NonNull PostView oldItem, @NonNull PostView newItem) {
            return (oldItem.id == newItem.id) && (oldItem.topic_id == newItem.topic_id) && (oldItem.forum_id == newItem.forum_id) ;
        }

        @Override
        public boolean areContentsTheSame(@NonNull PostView oldItem, @NonNull PostView newItem) {
            //return  (oldItem.text.contains(newItem.text) && (oldItem.carmaPlus==newItem.carmaPlus) && (oldItem.carmaMinus==newItem.carmaMinus) && (oldItem.lastmod==newItem.lastmod) && (oldItem.disableCarma==newItem.disableCarma));
            return oldItem==newItem;
        }
    };
}
