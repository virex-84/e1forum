package com.virex.e1forum.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;
import androidx.paging.PagedListAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.virex.e1forum.R;
import com.virex.e1forum.db.entity.Topic;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class TopicAdapter extends PagedListAdapter<Topic, RecyclerView.ViewHolder> {

    private static final int ITEM = 1;
    private static final int EMPTY = 2;

    private TopicListener topicListener;

    public interface TopicListener {
        void onClick(Topic topic);
        void onBookMark(Topic topic);
    }

    public TopicAdapter(@NonNull DiffUtil.ItemCallback<Topic> diffCallback, TopicListener topicListener) {
        super(diffCallback);
        this.topicListener=topicListener;
    }

    @Override
    public int getItemViewType(int position) {
        if (getItem(position)==null)
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
                View viewItem= inflater.inflate(R.layout.topic_item, parent, false);
                viewHolder = new TopicHolder(viewItem);
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
        //Context context = holder.itemView.getContext();

        switch(getItemViewType(position)){
            case ITEM:
                final Topic topic = getItem(position);
                TopicHolder topicHolder= ((TopicHolder)holder);

                if (topic.title==null) {
                    topicHolder.tv_title.setText("?");
                }
                    else
                    topicHolder.tv_title.setText(HtmlCompat.fromHtml(topic.title,HtmlCompat.FROM_HTML_MODE_COMPACT));

                topicHolder.tv_username.setText(HtmlCompat.fromHtml(topic.userName,HtmlCompat.FROM_HTML_MODE_COMPACT));

                if (topic.isAttathed)
                    topicHolder.iv_attach.setVisibility(View.VISIBLE);
                else
                    topicHolder.iv_attach.setVisibility(View.GONE);

                if (topic.isClosed)
                    topicHolder.iv_closed.setVisibility(View.VISIBLE);
                else
                    topicHolder.iv_closed.setVisibility(View.GONE);

                if (topic.isBookMark)
                    topicHolder.ib_bookmark.setImageResource(R.drawable.ic_bookmark);
                else
                    topicHolder.ib_bookmark.setImageResource(R.drawable.ic_unbookmark);

                String text=new SimpleDateFormat("HH:mm dd.MM.yyyy", Locale.ENGLISH).format(topic.lastmod);
                topicHolder.tv_date.setText(text);

                topicHolder.main.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (topicListener!=null){
                            topicListener.onClick(topic);
                        }
                    }
                });

                topicHolder.ib_bookmark.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (topicListener!=null){
                            topicListener.onBookMark(topic);
                        }
                    }
                });
                break;
            default:
                break;
        }
    }

    class TopicHolder extends RecyclerView.ViewHolder {
        TextView tv_title;
        TextView tv_username;
        ImageView iv_attach;
        ImageView iv_closed;
        ImageButton ib_bookmark;
        TextView tv_date;
        View main;

        TopicHolder(@NonNull View itemView) {
            super(itemView);
            main = itemView;
            tv_title = itemView.findViewById(R.id.tv_title);
            tv_username = itemView.findViewById(R.id.tv_username);
            iv_attach = itemView.findViewById(R.id.iv_attach);
            iv_closed = itemView.findViewById(R.id.iv_closed);
            ib_bookmark = itemView.findViewById(R.id.ib_bookmark);
            tv_date = itemView.findViewById(R.id.tv_date);
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
