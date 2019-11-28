package com.virex.e1forum;

import android.app.Application;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PagedList;
import androidx.paging.PositionalDataSource;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.virex.e1forum.db.database.AppDataBase;
import com.virex.e1forum.db.entity.Forum;
import com.virex.e1forum.db.entity.Post;
import com.virex.e1forum.db.entity.Topic;
import com.virex.e1forum.db.entity.User;
import com.virex.e1forum.network.VoteResponse;
import com.virex.e1forum.network.VoteType;
import com.virex.e1forum.parser.SiteParser;
import com.virex.e1forum.repository.ForumsWorker;
import com.virex.e1forum.repository.PostsWorker;
import com.virex.e1forum.repository.TopicsWorker;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ForumViewModel extends AndroidViewModel {

    private MutableLiveData<Boolean> privIsLogin = new MutableLiveData<>(false);

    public interface NetworkListener {
        void onSuccess(String message);
        void onError(String message);
    }

    private Application application;
    private AppDataBase database;

    public ForumViewModel(@NonNull Application application) {
        super(application);
        this.application=application;

        database=AppDataBase.getAppDatabase(application);

        //заполняем LiveData при запуске (наличие определенного cookie)
        privIsLogin.postValue(((App)getApplication()).initIsLogin());
    }

    public LiveData<Boolean> isLogin() {
        return privIsLogin;
    }

    public LiveData<List<Forum>> getForums(){
        return database.forumDao().dataSource();
    }

    public LiveData<WorkInfo> loadForums(){
        Data data = new Data.Builder()
                .putInt(TopicsWorker.EXTRA_ACTION, TopicsWorker.ACTION_LOAD_FROM_NETWORK)
                .build();
        OneTimeWorkRequest simpleRequest = new OneTimeWorkRequest.Builder(ForumsWorker.class)
                .setInputData(data)
                .build();
        WorkManager.getInstance(application).enqueueUniqueWork("loadForums", ExistingWorkPolicy.REPLACE,simpleRequest);
        return WorkManager.getInstance(getApplication()).getWorkInfoByIdLiveData(simpleRequest.getId());
    }

    public LiveData<PagedList<Topic>> getTopics(final int forum_id){
        //return database.topicDao().dataSource(forum_id);
        LiveData<PagedList<Topic>> pagedListLiveData;

        PagedList.Config config = new PagedList.Config.Builder()
                .setEnablePlaceholders(true)
                .setPageSize(10)
                .build();

        pagedListLiveData = new LivePagedListBuilder<>(database.topicDao().dataSourcePagedList(forum_id),config)
                .setFetchExecutor(Executors.newSingleThreadExecutor())
                .setInitialLoadKey(1)
                .setBoundaryCallback(new PagedList.BoundaryCallback<Topic>() {
                    @Override
                    public void onZeroItemsLoaded() {
                        super.onZeroItemsLoaded();
                        //база пустая
                        loadTopics(forum_id,0);
                    }
                })
                .build();

        return  pagedListLiveData;

    }

    public LiveData<Integer> getTopicsCount(int forum_id){
        return  database.topicDao().getCount(forum_id);
    }

    public LiveData<Integer> getTopicsCount(int forum_id, boolean isAttathed){
        return  database.topicDao().getCount(forum_id,isAttathed);
    }

    public LiveData<WorkInfo> loadTopics(int forum_id, int topic_id){
        Data data = new Data.Builder()
                .putInt(TopicsWorker.EXTRA_ACTION, TopicsWorker.ACTION_LOAD_FROM_NETWORK)
                .putInt(TopicsWorker.EXTRA_FORUM_ID, forum_id)
                .putInt(TopicsWorker.EXTRA_TOPIC_ID, topic_id)
                .build();
        OneTimeWorkRequest simpleRequest = new OneTimeWorkRequest.Builder(TopicsWorker.class).setInputData(data).build();
        WorkManager.getInstance(application).enqueueUniqueWork("loadTopics", ExistingWorkPolicy.REPLACE,simpleRequest);
        //return WorkManager.getInstance(getApplication()).getWorkInfosByTagLiveData(simpleRequest.getId().toString());
        return WorkManager.getInstance(getApplication()).getWorkInfoByIdLiveData(simpleRequest.getId());
    }

    public PagedList<Topic> emptyTopicPagedList(){
        PositionalDataSource<Topic> dataSource = new PositionalDataSource<Topic>() {

            @Override
            public void loadInitial(@NonNull LoadInitialParams params, @NonNull LoadInitialCallback<Topic> callback) {
                ArrayList<Topic> list=new ArrayList<>();
                list.add(null);
                callback.onResult(list, 0,1);
            }

            @Override
            public void loadRange(@NonNull LoadRangeParams params, @NonNull LoadRangeCallback<Topic> callback) {
                callback.onResult(new ArrayList<Topic>());
            }
        };
        PagedList.Config config = new PagedList.Config.Builder()
                .setEnablePlaceholders(true)
                .setPageSize(1)
                .build();

        return new PagedList.Builder<>(dataSource,config)
                .setFetchExecutor(Executors.newSingleThreadExecutor())
                .setNotifyExecutor(Executors.newSingleThreadExecutor())
                .build();
    }

    public PagedList<Post> emptyPostPagedList(){
        PositionalDataSource<Post> dataSource = new PositionalDataSource<Post>() {

            @Override
            public void loadInitial(@NonNull LoadInitialParams params, @NonNull LoadInitialCallback<Post> callback) {
                ArrayList<Post> list=new ArrayList<>();
                list.add(null);
                callback.onResult(list, 0,1);
            }

            @Override
            public void loadRange(@NonNull LoadRangeParams params, @NonNull LoadRangeCallback<Post> callback) {
                callback.onResult(new ArrayList<Post>());
            }
        };
        PagedList.Config config = new PagedList.Config.Builder()
                .setEnablePlaceholders(true)
                .setPageSize(1)
                .build();

        return new PagedList.Builder<>(dataSource,config)
                .setFetchExecutor(Executors.newSingleThreadExecutor())
                .setNotifyExecutor(Executors.newSingleThreadExecutor())
                .build();
    }

    /*
    public LiveData<List<Post>> getPosts(int forum_id, int topic_id){
        return database.postDao().dataSource(forum_id,topic_id);
    }
    */

    public LiveData<PagedList<Post>> getPosts(final int forum_id, final int topic_id){
        //return database.postDao().dataSource(forum_id,topic_id);
        LiveData<PagedList<Post>> pagedListLiveData;

        PagedList.Config config = new PagedList.Config.Builder()
                .setEnablePlaceholders(true)
                .setPageSize(10)
                //.setInitialLoadSizeHint(initial)
                .build();

        pagedListLiveData = new LivePagedListBuilder<>(database.postDao().dataSourcePagedList(forum_id,topic_id),config)
                .setFetchExecutor(Executors.newSingleThreadExecutor())
                .setBoundaryCallback(new PagedList.BoundaryCallback<Post>() {
                    @Override
                    public void onZeroItemsLoaded() {
                        super.onZeroItemsLoaded();
                        //база пустая
                        loadPosts(forum_id, topic_id, 0);
                    }
                })
                .build();

        return  pagedListLiveData;

    }

    public LiveData<WorkInfo> loadPosts(int forum_id, int topic_id, int page_id){
        Data data = new Data.Builder()
                .putInt(PostsWorker.EXTRA_ACTION, PostsWorker.ACTION_LOAD_FROM_NETWORK)
                .putInt(PostsWorker.EXTRA_FORUM_ID, forum_id)
                .putInt(PostsWorker.EXTRA_TOPIC_ID, topic_id)
                .putInt(PostsWorker.EXTRA_PAGE_ID, page_id)
                .build();
        OneTimeWorkRequest simpleRequest = new OneTimeWorkRequest.Builder(PostsWorker.class).setInputData(data).build();

        WorkManager.getInstance(application).enqueueUniqueWork("loadPosts", ExistingWorkPolicy.REPLACE,simpleRequest);
        return WorkManager.getInstance(getApplication()).getWorkInfoByIdLiveData(simpleRequest.getId());
    }

    public LiveData<Integer> getTopicPagesCount(int forum_id, int topic_id){
        return database.topicDao().getPagesCount(forum_id,topic_id);
    }


    public LiveData<Topic> getTopicLive(int forum_id, int topic_id){
        return database.topicDao().getTopicLive(forum_id,topic_id);
    }

    public LiveData<User> getUser(String userNick){
        return database.userDao().getUserLive(userNick);
    }

    public void checkTopicBookmark(Topic topic) {
        Data data = new Data.Builder()
                .putInt(TopicsWorker.EXTRA_ACTION, TopicsWorker.ACTION_CHANGE_BOOKMARK)
                .putInt(TopicsWorker.EXTRA_FORUM_ID, topic.forum_id)
                .putInt(TopicsWorker.EXTRA_TOPIC_ID, topic.id)
                .build();
        OneTimeWorkRequest simpleRequest = new OneTimeWorkRequest.Builder(TopicsWorker.class)
                .setInputData(data)
                .build();
        WorkManager.getInstance(application).beginUniqueWork("checkTopicBookmark", ExistingWorkPolicy.REPLACE,simpleRequest).enqueue();
    }

    public void checkForumBookmark(Forum forum) {
        Data data = new Data.Builder()
                .putInt(ForumsWorker.EXTRA_ACTION, ForumsWorker.ACTION_CHANGE_BOOKMARK)
                .putInt(ForumsWorker.EXTRA_FORUM_ID, forum.id)
                .build();
        OneTimeWorkRequest simpleRequest = new OneTimeWorkRequest.Builder(ForumsWorker.class)
                .setInputData(data)
                .build();
        WorkManager.getInstance(application).beginUniqueWork("checkForumBookmark", ExistingWorkPolicy.REPLACE,simpleRequest).enqueue();
    }

    //кодирование для post запросов
    public static String URLEncodeString(String source){
        //source=source.replace("UTF-8","windows-1251");
        try {
            source= URLEncoder.encode(source, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return source;
    }

    public void loginSite(String login, String password, final NetworkListener loginListener){
        //пробуем залогиниться
        App.getPostApi().login("login",login,password).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    try {
                        BufferedReader br = new BufferedReader(new InputStreamReader(response.body().byteStream(), "utf-8"));
                        //вытаскиваем html
                        StringBuilder text=new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null) {
                            text.append(line);
                        }
                        //вытаскиваем сообщение об ошибке
                        String errorLoginMessage= SiteParser.extractTagText(text.toString(),"p-tooltip__error");
                        if (!TextUtils.isEmpty(errorLoginMessage)) {
                            if (loginListener != null)
                                loginListener.onError(errorLoginMessage);

                            privIsLogin.setValue(((App)getApplication()).isLogin());
                            /*
                            Executors.newSingleThreadExecutor().submit(new Runnable() {
                                @Override
                                public void run() {
                                    privIsLogin.postValue(((App)getApplication()).isLogin());
                                }
                            });
                             */


                            //privIsLogin.postValue(true);
                            return;
                        }


                        if (((App)getApplication()).isLogin()){
                            if (loginListener!=null)
                                loginListener.onSuccess(getString(R.string.login_success));
                        } else {
                            if (loginListener!=null)
                                loginListener.onError(getString(R.string.login_error));
                        }

                    }catch(Exception e){
                        if (loginListener!=null)
                            loginListener.onError(e.getMessage());
                    }
                }

                //отправляем значение
                privIsLogin.setValue(((App)getApplication()).isLogin());
                /*
                Executors.newSingleThreadExecutor().submit(new Runnable() {
                    @Override
                    public void run() {
                        privIsLogin.postValue(((App)getApplication()).isLogin());
                    }
                });
                 */
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                if (loginListener!=null)
                    loginListener.onError(t.getMessage());

                //отправляем значение
                privIsLogin.setValue(((App)getApplication()).isLogin());
                /*
                Executors.newSingleThreadExecutor().submit(new Runnable() {
                    @Override
                    public void run() {
                        privIsLogin.postValue(((App)getApplication()).isLogin());
                    }
                });
                 */
            }
        });

    }

    public void logOut(){
        ((App)getApplication()).clearCookies();
        privIsLogin.setValue(((App)getApplication()).isLogin());
    }


    public void votePost(final Post post, VoteType voteType, final NetworkListener voteListener){
        App.getPostApi().vote(String.valueOf(post.forum_id),String.valueOf(post.topic_id),String.valueOf(post.id),voteType.name()).enqueue(new Callback<VoteResponse>() {
            @Override
            public void onResponse(Call<VoteResponse> call, Response<VoteResponse> response) {
                if (response.isSuccessful()){
                    final VoteResponse voteResponse=response.body();
                    if (voteResponse!=null){
                        Executors.newSingleThreadExecutor().submit(new Runnable() {
                            @Override
                            public void run() {
                                post.carmaPlus=Integer.parseInt(voteResponse.u);
                                post.carmaMinus=Integer.parseInt(voteResponse.d);
                                post.disableCarma=true;
                                database.postDao().update(post);
                            }
                        });
                        if (voteListener!=null)
                            voteListener.onSuccess("");
                    } else {
                        if (voteListener!=null)
                            voteListener.onError(getString(R.string.vote_error));
                    }
                } else {
                    if (voteListener!=null)
                        voteListener.onError(getString(R.string.vote_error));
                }
            }

            @Override
            public void onFailure(Call<VoteResponse> call, Throwable t) {
                if (voteListener!=null)
                    voteListener.onError(t.getMessage());
            }
        });
    }

    public void sendPost(final int forum_id, final int topic_id, final int post_id, String subject, String body, final NetworkListener postListener) {
        App.getPostApi().post(String.valueOf(forum_id),String.valueOf(topic_id),String.valueOf(post_id),subject,body,"Y").enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    try {
                        BufferedReader br = new BufferedReader(new InputStreamReader(response.body().byteStream(), "utf-8"));
                        //вытаскиваем html
                        StringBuilder text=new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null) {
                            text.append(line);
                        }
                        //вытаскиваем сообщение об ошибке
                        String errorPostMessage= SiteParser.extractTagText(text.toString(),"danger");
                        if (!TextUtils.isEmpty(errorPostMessage)) {
                            if (postListener!=null)
                                postListener.onError(errorPostMessage);
                        } else {
                            if (postListener!=null)
                                postListener.onSuccess("");

                            //запускаем запрос к серверу на наличие новых постов
                            Data data = new Data.Builder()
                                    .putInt(PostsWorker.EXTRA_ACTION, PostsWorker.ACTION_LOAD_FROM_NETWORK)
                                    .putInt(PostsWorker.EXTRA_FORUM_ID, forum_id)
                                    .putInt(PostsWorker.EXTRA_TOPIC_ID, topic_id)
                                    .putInt(PostsWorker.EXTRA_PAGE_ID, post_id)
                                    .build();
                            OneTimeWorkRequest simpleRequest = new OneTimeWorkRequest.Builder(PostsWorker.class).setInputData(data).build();

                            WorkManager.getInstance(application).enqueueUniqueWork("loadPosts", ExistingWorkPolicy.REPLACE,simpleRequest);
                        }

                    }catch(Exception e){
                        if (postListener!=null)
                            postListener.onError(e.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                if (postListener!=null)
                    postListener.onError(t.getMessage());
            }
        });
    }

    private String getString(int resId){
        return getApplication().getString(resId);
    }
}
