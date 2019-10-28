package com.virex.e1forum.repository;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.virex.e1forum.App;
import com.virex.e1forum.db.database.AppDataBase;
import com.virex.e1forum.db.entity.Forum;
import com.virex.e1forum.db.entity.Post;
import com.virex.e1forum.db.entity.Topic;
import com.virex.e1forum.db.entity.User;
import com.virex.e1forum.parser.SiteParser;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Response;

/**
 * "Воркер" для загрузки форумов в БД
 */
public class PostsWorker extends Worker {

    public static final String EXTRA_FORUM_ID = "forum_id";
    public static final String EXTRA_TOPIC_ID = "topic_id";
    public static final String EXTRA_PAGE_ID = "page_id";

    public static final String POSTS_MESSAGE = "POSTS_MESSAGE";

    private AppDataBase database = AppDataBase.getAppDatabase(getApplicationContext());

    public PostsWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        final int forum_id=getInputData().getInt(EXTRA_FORUM_ID,-1);
        final int topic_id=getInputData().getInt(EXTRA_TOPIC_ID,-1);
        int page_id=getInputData().getInt(EXTRA_PAGE_ID,-1);
        try {
            Response<ResponseBody> result= App.getPostApi().getPosts(forum_id,topic_id,page_id).execute();
            if (result.isSuccessful()) {
                String text=result.body().string();
                SiteParser.parsePosts(SiteParser.SiteType.PARCE_MOBILE_SITE, text, forum_id, topic_id, new SiteParser.ParserListener() {
                    @Override
                    public void onParse(Forum forum, SiteParser.ParseStatus parseStatus) {

                    }

                    @Override
                    public void onParse(Topic topic, SiteParser.ParseStatus parseStatus) {

                    }

                    @Override
                    public void onParse(Post post, SiteParser.ParseStatus parseStatus) {
                        if (parseStatus== SiteParser.ParseStatus.INPROCESS && post!=null)
                            database.postDao().insert(post);

                        //по окончании парсинга постов - нам нужно обновить количество постов в топике
                        if (parseStatus== SiteParser.ParseStatus.END){
                            int count=database.postDao().getCount(forum_id, topic_id);
                            Topic oldTopic=database.topicDao().getTopic(forum_id, topic_id);
                            oldTopic.pagesCount=count / 25; //количество страниц кратное 25
                            database.topicDao().update(oldTopic);
                        }
                    }

                    @Override
                    public void onParse(User user, SiteParser.ParseStatus parseStatus) {
                        if (parseStatus!=SiteParser.ParseStatus.INPROCESS && user==null) return;

                        User oldUser=database.userDao().getUser(user.nick);
                        if (oldUser!=null){
                            //обязательно сохраняем поля которые могут перезатереться если войти не залогиненным
                            if (user.actionMail==null) user.actionMail=oldUser.actionMail;
                            if (user.actionLK==null) user.actionLK=oldUser.actionLK;
                            if (user.avatarIMG==null) user.avatarIMG=oldUser.avatarIMG;
                            if (user.avatarURL==null) user.avatarURL=oldUser.avatarURL;
                            if (user.link==null) user.link=oldUser.link;
                        }
                        database.userDao().insert(user);
                    }
                });

            }
        } catch (IOException e) {
            e.printStackTrace();
            //сообщаем результат ошибки
            Data data = new Data.Builder().putString(POSTS_MESSAGE,e.getMessage()).build();
            return Result.failure(data);
        }

        return Result.success();
    }
}
