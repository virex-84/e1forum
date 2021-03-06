package com.virex.e1forum.ui;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.Html;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.QuoteSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.URLSpan;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Button;
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

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.virex.e1forum.R;
import com.virex.e1forum.common.GlideApp;
import com.virex.e1forum.common.Utils;
import com.virex.e1forum.db.dao.PostView;
import com.virex.e1forum.network.VoteType;

import org.xml.sax.XMLReader;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class PostAdapter extends PagedListAdapter<PostView, RecyclerView.ViewHolder> {


    private boolean isReadOnly=false;
    private static final int ITEM = 1;
    private static final int EMPTY = 2;

    private final Resources resources;

    private final LinkMovementMethod linkMovementMethod;
    private final PostListener postListener;

    private String filter;
    private int spanColor=0;
    private int foregroundColor =-1;
    private int backgroundColor =-1;

    public interface PostListener {
        void onLinkClick(String link);
        void onUserClick(String userNick, TextView widget);
        void onImageClick(Drawable drawable, String filename,boolean isLongClick);
        void onVoteClick(PostView post, VoteType voteType);
        void onReplyClick(PostView post);
        void onQuoteClick(PostView post);
        void onModeratorClick(PostView post);
        void onCurrentListLoaded();
    }

    public PostAdapter(@NonNull DiffUtil.ItemCallback<PostView> diffCallback, PostListener postListener, Resources resources) {
        super(diffCallback);
        this.postListener =postListener;

        this.resources=resources;

        //отрабатываем нажатие на ссылки
        this.linkMovementMethod=new LinkMovementMethod(){
            long longClickDelay = ViewConfiguration.getLongPressTimeout();
            long startTime;
            boolean isLong=false;

            @Override
            public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event) {

                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    startTime = System.currentTimeMillis();
                    isLong=false;
                }

                if (event.getAction() == MotionEvent.ACTION_UP) {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - startTime >= longClickDelay)
                        isLong=true;
                }

                if (event.getAction() != MotionEvent.ACTION_UP)
                    return super.onTouchEvent(widget, buffer, event);

                int x = (int) event.getX();
                int y = (int) event.getY();

                x -= widget.getTotalPaddingLeft();
                y -= widget.getTotalPaddingTop();

                x += widget.getScrollX();
                y += widget.getScrollY();

                Layout layout = widget.getLayout();
                int line = layout.getLineForVertical(y);
                int off = layout.getOffsetForHorizontal(line, x);

                URLSpan[] spanLink = buffer.getSpans(off, off, URLSpan.class);
                if (spanLink.length != 0) {
                    //находим ссылку
                    String link = spanLink[0].getURL();
                    if (link.contains("user:")){
                        String userNick=link.replace("user:","");
                        if(PostAdapter.this.postListener!=null)
                            PostAdapter.this.postListener.onUserClick(userNick,widget);
                    } else
                    if(PostAdapter.this.postListener!=null)
                        PostAdapter.this.postListener.onLinkClick(link);
                }

                ImageSpan[] spanImage = buffer.getSpans(off, off, ImageSpan.class);
                if (spanImage.length != 0) {
                    if(PostAdapter.this.postListener!=null)
                        PostAdapter.this.postListener.onImageClick(spanImage[0].getDrawable(),spanImage[0].getSource(),isLong);
                }

                return true;
            }
        };
    }

    @Override
    public void onCurrentListChanged(@Nullable PagedList<PostView> previousList, @Nullable PagedList<PostView> currentList) {
        super.onCurrentListChanged(previousList, currentList);
        if(PostAdapter.this.postListener!=null)
            PostAdapter.this.postListener.onCurrentListLoaded();
    }

    public void setColors(int spanColor, int foregroundColor, int backgroundColor) {
        this.spanColor=spanColor;
        this.foregroundColor=foregroundColor;
        this.backgroundColor=backgroundColor;
    }

    public void setIsReadOnly(boolean isReadOnly){
        this.isReadOnly=isReadOnly;
        this.notifyDataSetChanged();
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
                View viewItem= inflater.inflate(R.layout.post_item, parent, false);
                viewHolder = new PostHolder(viewItem);
                break;
            default:
                View viewEmpty= inflater.inflate(R.layout.empty_item, parent, false);
                viewHolder = new EmptyHolder(viewEmpty);
                break;
        }
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, int position) {

        switch(getItemViewType(position)){
            case ITEM:
                final PostView post = getItem(position);
                final PostHolder postHolder=((PostHolder)holder);

                String userLink=String.format(Locale.ENGLISH,"<a href=\"%s\">%s</a>","user:".concat(post.user),post.user);
                SpannableStringBuilder user=(SpannableStringBuilder)HtmlCompat.fromHtml(userLink,HtmlCompat.FROM_HTML_MODE_COMPACT);
                SpannableStringBuilder text=(SpannableStringBuilder)HtmlCompat.fromHtml(post.text, HtmlCompat.FROM_HTML_MODE_LEGACY,new GlideImageGetter(resources,postHolder.tv_text), new Html.TagHandler() {
                    @Override
                    public void handleTag(boolean opening, String tag, Editable editable, XMLReader xmlReader) {

                        //добавляем цвет для "от пользователя xxx"
                        if (tag.equalsIgnoreCase("reply-cont")) {
                            if (!opening)
                                editable.setSpan(new ForegroundColorSpan(spanColor), 0, editable.length(), 0);
                        }

                        if (opening) {
                            //добавим надпись перед цитатой
                            if (tag.equalsIgnoreCase("blockquote")) {
                                editable.append("Цитата:\n");
                            }
                        } else {
                            //переделываем все "цитаты" на свой компонент
                            QuoteSpan[] quoteSpans = editable.getSpans(0, editable.length(), QuoteSpan.class);
                            for (QuoteSpan quoteSpan : quoteSpans) {
                                int start = editable.getSpanStart(quoteSpan);
                                int end = editable.getSpanEnd(quoteSpan);
                                int flags = editable.getSpanFlags(quoteSpan);
                                editable.removeSpan(quoteSpan);
                                MyQuoteSpan qs=new MyQuoteSpan(spanColor,20,40);

                                editable.setSpan(qs,start,end,flags);
                            }
                        }
                    }
                });

                if (!TextUtils.isEmpty(filter)) {
                    user=Utils.makeSpanText(holder.itemView.getContext(),user,filter,foregroundColor,backgroundColor);

                    text=Utils.makeSpanText(holder.itemView.getContext(),text,filter,foregroundColor,backgroundColor);
                }

                postHolder.tv_user.setText(user);
                postHolder.tv_text.setText(text);

                postHolder.tv_date.setText(getDate(post.lastmod));
                //postHolder.tv_carma_plus.setText(String.valueOf(post.carmaPlus));
                //postHolder.tv_carma_minus.setText(String.valueOf(post.carmaMinus));

                postHolder.btn_plus.setText("+".concat(String.valueOf(post.carmaPlus)));
                postHolder.btn_minus.setText("-".concat(String.valueOf(post.carmaMinus)));

                //грузим аватар
                //if (post.userAvatarURL!=null && post.userAvatarURL.length()>0){

                //по умолчанию - "пустой" аватар
                postHolder.iv_avatar.setImageResource(R.drawable.ic_empty_image);
                //postHolder.iv_avatar.setAlpha(0.3f);

                if (post.usr.avatarURL!=null && post.usr.avatarURL.length()>0){
                    try {
                        GlideApp
                                .with(holder.itemView)
                                //.load(post.userAvatarURL)
                                .load(post.usr.avatarURL)
                                .placeholder(R.drawable.ic_empty_image)
                                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                                //.override(30, 30)
                                .transform(new CircleCrop())
                                .error(R.drawable.ic_error_image)
                                .into(postHolder.iv_avatar);
                        //postHolder.iv_avatar.setAlpha(1f);
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                }

                //пост для чтения - запрет на ответ и цитирование
                if (isReadOnly){
                    postHolder.btn_reply.setEnabled(false);
                    postHolder.btn_quote.setEnabled(false);
                    postHolder.btn_moderator.setEnabled(false);
                } else {
                    postHolder.btn_reply.setEnabled(true);
                    postHolder.btn_quote.setEnabled(true);
                    postHolder.btn_moderator.setEnabled(true);

                    postHolder.btn_reply.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (PostAdapter.this.postListener != null)
                                PostAdapter.this.postListener.onReplyClick(post);
                        }
                    });
                    postHolder.btn_quote.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (PostAdapter.this.postListener != null)
                                PostAdapter.this.postListener.onQuoteClick(post);
                        }
                    });
                    postHolder.btn_moderator.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (PostAdapter.this.postListener != null)
                                PostAdapter.this.postListener.onModeratorClick(post);
                        }
                    });
                }

                //реагируем на нажатие ссылок и т.д.
                postHolder.tv_text.setMovementMethod(this.linkMovementMethod);
                postHolder.tv_user.setMovementMethod(this.linkMovementMethod);

                //плюсомет
                if (isReadOnly || post.disableCarma){
                    postHolder.btn_plus.setEnabled(false);
                    postHolder.btn_minus.setEnabled(false);
                } else {
                    postHolder.btn_plus.setEnabled(true);
                    postHolder.btn_minus.setEnabled(true);

                    postHolder.btn_plus.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (PostAdapter.this.postListener != null)
                                //PostAdapter.this.postListener.onVoteClick(post.forum_id,post.topic_id, post.id, VoteType.up);
                                PostAdapter.this.postListener.onVoteClick(post, VoteType.up);
                        }
                    });

                    postHolder.btn_minus.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (PostAdapter.this.postListener != null)
                                //PostAdapter.this.postListener.onVoteClick(post.forum_id,post.topic_id, post.id, VoteType.down);
                                PostAdapter.this.postListener.onVoteClick(post, VoteType.down);
                        }
                    });
                }

                break;
            default:
                break;
        }
    }

    private String getDate(long timeStamp){
        try{
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm dd.MM.yyyy", Locale.ENGLISH);
            Date netDate = (new Date(timeStamp));
            return sdf.format(netDate);
        }
        catch(Exception ex){
            return "xx";
        }
    }

    class PostHolder extends RecyclerView.ViewHolder {
        TextView tv_user;
        TextView tv_text;
        Button btn_reply;
        Button btn_quote;
        Button btn_moderator;
        //TextView tv_carma_plus;
        //TextView tv_carma_minus;
        Button btn_plus;
        Button btn_minus;
        ImageView iv_avatar;


        ImageView iv_attach;
        ImageButton ib_bookmark;
        TextView tv_date;
        View main;

        PostHolder(@NonNull View itemView) {
            super(itemView);
            main = itemView;
            tv_user = itemView.findViewById(R.id.tv_user);
            tv_text = itemView.findViewById(R.id.tv_text);
            tv_date = itemView.findViewById(R.id.tv_date);
            btn_reply = itemView.findViewById(R.id.btn_reply);
            btn_quote = itemView.findViewById(R.id.btn_quote);
            btn_moderator = itemView.findViewById(R.id.btn_moderator);
            //tv_carma_plus = itemView.findViewById(R.id.tv_carma_plus);
            //tv_carma_minus = itemView.findViewById(R.id.tv_carma_minus);
            btn_plus = itemView.findViewById(R.id.btn_plus);
            btn_minus = itemView.findViewById(R.id.btn_minus);
            iv_avatar = itemView.findViewById(R.id.iv_avatar);
        }
    }

    class EmptyHolder extends RecyclerView.ViewHolder {
        TextView tv_title;

        EmptyHolder(@NonNull View itemView) {
            super(itemView);
            tv_title = itemView.findViewById(R.id.tv_title);
        }
    }

    private static <T> Object getLast(Spanned text, Class<T> kind) {
        /*
         * This knows that the last returned object from getSpans()
         * will be the most recently added.
         */
        Object[] objs = text.getSpans(0, text.length(), kind);

        if (objs.length == 0) {
            return null;
        } else {
            return objs[objs.length - 1];
        }
    }

    private static void start(Editable text, Object mark) {
        int len = text.length();
        text.setSpan(mark, len, len, Spannable.SPAN_MARK_MARK);
    }

    private static <T> void end(Editable text, Class<T> kind,  Object... repl) {
        int len = text.length();
        Object obj = getLast(text, kind);
        int where = text.getSpanStart(obj);

        text.removeSpan(obj);

        if (where != len) {
            //text.setSpan(repl, where, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            for (Object replace : repl) {
                text.setSpan(replace, where, len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    public void markText(String filter) {
        this.filter=filter;
    }
}
