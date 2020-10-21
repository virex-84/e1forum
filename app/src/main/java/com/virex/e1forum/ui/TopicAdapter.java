package com.virex.e1forum.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.text.HtmlCompat;
import androidx.paging.PagedList;
import androidx.paging.PagedListAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.virex.e1forum.R;
import com.virex.e1forum.common.Utils;
import com.virex.e1forum.db.dao.TopicView;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class TopicAdapter extends PagedListAdapter<TopicView, RecyclerView.ViewHolder> {

    private static final int ITEM = 1;
    private static final int EMPTY = 2;

    private TopicListener topicListener;

    private String filter;
    private int foregroundColor =-1;
    private int backgroundColor =-1;

    public interface TopicListener {
        void onClick(TopicView topic);
        void onBookMark(TopicView topic);
        void onCurrentListLoaded();
    }

    public TopicAdapter(@NonNull DiffUtil.ItemCallback<TopicView> diffCallback, TopicListener topicListener) {
        super(diffCallback);
        this.topicListener=topicListener;
    }

    @Override
    public void onCurrentListChanged(@Nullable PagedList<TopicView> previousList, @Nullable PagedList<TopicView> currentList) {
        super.onCurrentListChanged(previousList, currentList);
        if (topicListener!=null)
            topicListener.onCurrentListLoaded();
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
        Context context = holder.itemView.getContext();

        switch(getItemViewType(position)){
            case ITEM:
                final TopicView topic = getItem(position);
                TopicHolder topicHolder= ((TopicHolder)holder);

                SpannableStringBuilder title=(SpannableStringBuilder)HtmlCompat.fromHtml(topic.title!=null ? topic.title : "?",HtmlCompat.FROM_HTML_MODE_COMPACT);
                SpannableStringBuilder user=(SpannableStringBuilder)HtmlCompat.fromHtml(topic.userName,HtmlCompat.FROM_HTML_MODE_COMPACT);

                if (!TextUtils.isEmpty(filter)) {
                    title= Utils.makeSpanText(holder.itemView.getContext(),title,filter,foregroundColor,backgroundColor);

                    user=Utils.makeSpanText(holder.itemView.getContext(),user,filter,foregroundColor,backgroundColor);
                }

                topicHolder.tv_title.setText(title);
                topicHolder.tv_username.setText(user);

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

                //если тему открывали
                if (topic.commentsloaded>0) {
                    //если она не вся прочитана - то отображаем оранжевый значок
                    if (topic.commentsloaded<(topic.comments-1))
                        topicHolder.iv_isalreadyread.setBackgroundColor(context.getResources().getColor(R.color.colorAccent));
                    else
                        topicHolder.iv_isalreadyread.setBackgroundColor(context.getResources().getColor(R.color.white));

                    //помечаем ранее открытую тему обычным шрифтом
                    topicHolder.tv_title.setTypeface(null, Typeface.NORMAL);
                } else {
                    //не открытую тему выделяем
                    topicHolder.tv_title.setTypeface(null, Typeface.BOLD);

                    topicHolder.iv_isalreadyread.setBackgroundColor(context.getResources().getColor(R.color.white));
                }


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

    public void setColors(int foregroundColor, int backgroundColor) {
        this.foregroundColor=foregroundColor;
        this.backgroundColor=backgroundColor;
    }

    public void markText(String filter) {
        this.filter=filter;
    }

    class TopicHolder extends RecyclerView.ViewHolder {
        TextView tv_title;
        TextView tv_username;
        ImageView iv_attach;
        ImageView iv_closed;
        ImageButton ib_bookmark;
        TextView tv_date;
        View main;
        View iv_isalreadyread;

        TopicHolder(@NonNull View itemView) {
            super(itemView);
            main = itemView;
            tv_title = itemView.findViewById(R.id.tv_title);
            tv_username = itemView.findViewById(R.id.tv_username);
            iv_attach = itemView.findViewById(R.id.iv_attach);
            iv_closed = itemView.findViewById(R.id.iv_closed);
            ib_bookmark = itemView.findViewById(R.id.ib_bookmark);
            tv_date = itemView.findViewById(R.id.tv_date);
            iv_isalreadyread= itemView.findViewById(R.id.iv_isalreadyread);
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
