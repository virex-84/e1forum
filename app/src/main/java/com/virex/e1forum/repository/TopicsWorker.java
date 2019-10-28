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
public class TopicsWorker extends Worker {

    public static final String EXTRA_FORUM_ID = "forum_id";
    public static final String EXTRA_TOPIC_ID = "topic_id";
    public static final String EXTRA_ACTION = "action";

    public static final int ACTION_LOAD_FROM_NETWORK = 1;
    public static final int ACTION_CHANGE_BOOKMARK = 2;

    public static final String TOPICS_MESSAGE = "TOPICS_MESSAGE";

    private AppDataBase database = AppDataBase.getAppDatabase(getApplicationContext());

    public TopicsWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        int action=getInputData().getInt(EXTRA_ACTION,-1);
        int forum_id=getInputData().getInt(EXTRA_FORUM_ID,-1);
        int topic_id=getInputData().getInt(EXTRA_TOPIC_ID,-1);

        switch(action){
            case ACTION_LOAD_FROM_NETWORK:
                try {
                    Response<ResponseBody> result= App.getTopicApi().getTopics(forum_id).execute();
                    if (result.isSuccessful()) {
                        String text=result.body().string();
                        SiteParser.parseTopics(SiteParser.SiteType.PARCE_MOBILE_SITE, text, forum_id, new SiteParser.ParserListener() {
                            @Override
                            public void onParse(Forum forum, SiteParser.ParseStatus parseStatus) {

                            }

                            @Override
                            public void onParse(Topic topic, SiteParser.ParseStatus parseStatus) {
                                if (parseStatus!=SiteParser.ParseStatus.INPROCESS && topic==null) return;

                                Topic oldTopic=database.topicDao().getTopic(topic.forum_id,topic.id);
                                if (oldTopic!=null){
                                    //обязательно сохраняем флаг установленный пользователем иначе перетрется
                                    topic.isBookMark=oldTopic.isBookMark;
                                }

                                database.topicDao().insert(topic);
                            }

                            @Override
                            public void onParse(Post post, SiteParser.ParseStatus parseStatus) {

                            }

                            @Override
                            public void onParse(User user, SiteParser.ParseStatus parseStatus) {

                            }
                        });

                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    //сообщаем результат ошибки
                    Data data = new Data.Builder().putString(TOPICS_MESSAGE,e.getMessage()).build();
                    return Result.failure(data);
                }
                break;

            case ACTION_CHANGE_BOOKMARK:
                    database.topicDao().changeBookMark(forum_id,topic_id);
                break;
        }


        return Result.success();
    }
}
