package com.virex.e1forum.repository;

import android.content.Context;

import com.virex.e1forum.App;
import com.virex.e1forum.db.database.AppDataBase;
import com.virex.e1forum.db.entity.Forum;
import com.virex.e1forum.db.entity.Post;
import com.virex.e1forum.db.entity.Topic;
import com.virex.e1forum.db.entity.User;
import com.virex.e1forum.parser.SiteParser;

import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import okhttp3.ResponseBody;
import retrofit2.Response;

/**
 * "Воркер" для загрузки форумов в БД
 */
public class ForumsWorker extends Worker {

    public static final String EXTRA_FORUM_ID = "forum_id";
    public static final String EXTRA_ACTION = "action";

    public static final int ACTION_LOAD_FROM_NETWORK = 1;
    public static final int ACTION_CHANGE_BOOKMARK = 2;

    public static final String FORUMS_MESSAGE = "FORUMS_MESSAGE";

    private AppDataBase database = AppDataBase.getAppDatabase(getApplicationContext());

    public ForumsWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        int action=getInputData().getInt(EXTRA_ACTION,-1);
        int forum_id=getInputData().getInt(EXTRA_FORUM_ID,-1);

        switch(action){
            case ACTION_LOAD_FROM_NETWORK:
                try {
                    Response<ResponseBody> result= App.getForumApi().getForums().execute();
                    if (result.isSuccessful()) {
                        String text=result.body().string();
                        //полный список форумов есть только на старой версии сайта
                        SiteParser.parseForums(SiteParser.SiteType.PARCE_OLD_SITE, text, new SiteParser.ParserListener() {
                            @Override
                            public void onParse(Forum forum) {
                                Forum oldForum=database.forumDao().getForum(forum.id);
                                if (oldForum!=null){
                                    //обязательно сохраняем флаг установленный пользователем иначе перетрется
                                    forum.isBookMark=oldForum.isBookMark;
                                }
                                database.forumDao().insert(forum);
                            }

                            @Override
                            public void onParse(Topic topic) {

                            }

                            @Override
                            public void onParse(Post post) {

                            }

                            @Override
                            public void onParse(User user) {

                            }
                        });

                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    //сообщаем результат ошибки
                    Data data = new Data.Builder().putString(FORUMS_MESSAGE,e.getMessage()).build();
                    return Result.failure(data);
                }
                break;
            case ACTION_CHANGE_BOOKMARK:
                database.forumDao().changeBookMark(forum_id);
                break;
        }

        return Result.success();
    }
}
