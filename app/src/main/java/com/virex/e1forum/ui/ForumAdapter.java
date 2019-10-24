package com.virex.e1forum.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.virex.e1forum.R;
import com.virex.e1forum.db.entity.Forum;


import java.util.ArrayList;
import java.util.List;

public class ForumAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>  {

    private static final int ITEM = 1;
    private static final int EMPTY = 2;

    private ForumListener forumListener;

    public interface ForumListener {
        void onClick(Forum Forum);
        void onBookMark(Forum forum);
    }

    private ArrayList<Forum> items=new ArrayList<>();

    public ForumAdapter(ForumListener ForumListener){
        this.forumListener =ForumListener;
    }

    public void submitList(List<Forum> data){
        items.clear();
        items.addAll(data);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        if (items.size()==0)
            return EMPTY;
        else
            return ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder viewHolder = null;
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        switch (viewType) {
            case ITEM:
                View viewItem= inflater.inflate(R.layout.forum_item, parent, false);
                viewHolder = new ForumHolder(viewItem);
                break;
            default:
                View viewEmpty= inflater.inflate(R.layout.empty_item, parent, false);
                viewHolder = new EmptyHolder(viewEmpty);
                break;
        }
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        switch(getItemViewType(position)){
            case ITEM:
                final Forum forum = items.get(position);
                ForumHolder forumHolder=((ForumHolder)holder);

                forumHolder.tv_title.setText(HtmlCompat.fromHtml(forum.title,HtmlCompat.FROM_HTML_MODE_COMPACT));

                if (forum.isBookMark)
                    forumHolder.ib_bookmark.setImageResource(R.drawable.ic_bookmark);
                else
                    forumHolder.ib_bookmark.setImageResource(R.drawable.ic_unbookmark);

                forumHolder.main.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (forumListener !=null){
                            forumListener.onClick(forum);
                        }
                    }
                });

                forumHolder.ib_bookmark.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (forumListener!=null){
                            forumListener.onBookMark(forum);
                        }
                    }
                });

                break;
            default:
                break;
        }
    }

    @Override
    public int getItemCount() {
        if (items.size()==0)
            return 1;
        else
            return items.size();
    }

    class ForumHolder extends RecyclerView.ViewHolder {
        TextView tv_title;
        ImageButton ib_bookmark;
        View main;

        ForumHolder(@NonNull View itemView) {
            super(itemView);
            main = itemView;
            tv_title = itemView.findViewById(R.id.tv_title);
            ib_bookmark = itemView.findViewById(R.id.ib_bookmark);
        }
    }

    class EmptyHolder extends RecyclerView.ViewHolder {
        TextView tv_title;

        EmptyHolder(@NonNull View itemView) {
            super(itemView);
            tv_title = itemView.findViewById(R.id.tv_title);
        }
    }
}
