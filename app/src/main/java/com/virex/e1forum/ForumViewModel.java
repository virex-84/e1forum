package com.virex.e1forum;

import android.app.Application;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ForumViewModel extends AndroidViewModel {

    private MutableLiveData<Boolean> privIsLogin = new MutableLiveData<>(false);

    //фильтрованный список топиков
    private MediatorLiveData<PagedList<Topic>> filteredTopics=new MediatorLiveData<>();
    private LiveData<PagedList<Topic>> topics;

    //фильтрованный список постов
    private MediatorLiveData<PagedList<Post>> filteredPosts=new MediatorLiveData<>();
    private LiveData<PagedList<Post>> posts;

    //все сообщения
    private MediatorLiveData<WorkInfo> messages=new MediatorLiveData<>();

    Observer<WorkInfo> observer = new Observer<WorkInfo>() {
        @Override
        public void onChanged(WorkInfo workInfo) {
            messages.setValue(workInfo);
        }
    };

    public LiveData<WorkInfo> getAllMessages(){
        return messages;
    }

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

    LiveData<Boolean> isLogin() {
        return privIsLogin;
    }

    LiveData<List<Forum>> getForums(){
        return database.forumDao().dataSource();
    }

    LiveData<WorkInfo> loadForums(){
        Data data = new Data.Builder()
                .putInt(TopicsWorker.EXTRA_ACTION, TopicsWorker.ACTION_LOAD_FROM_NETWORK)
                .build();
        OneTimeWorkRequest simpleRequest = new OneTimeWorkRequest.Builder(ForumsWorker.class)
                .setInputData(data)
                .build();
        WorkManager.getInstance(application).enqueueUniqueWork("loadForums", ExistingWorkPolicy.REPLACE,simpleRequest);
        LiveData<WorkInfo> result=WorkManager.getInstance(getApplication()).getWorkInfoByIdLiveData(simpleRequest.getId());
        result.observeForever(observer);
        return result;
    }

    /*
    LiveData<PagedList<Topic>> getTopics(final int forum_id){
        //return database.topicDao().dataSource(forum_id);
        LiveData<PagedList<Topic>> pagedListLiveData;

        PagedList.Config config = new PagedList.Config.Builder()
                .setEnablePlaceholders(false)
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
     */
    private LiveData<PagedList<Topic>> createFilteredTopic(final int forum_id, String filter){
        filter=filter.toLowerCase();
        LiveData<PagedList<Topic>> pagedListLiveData;

        PagedList.Config config = new PagedList.Config.Builder()
                .setEnablePlaceholders(true)
                .setPageSize(10)
                //.setInitialLoadSizeHint(initial)
                .build();

        if (TextUtils.isEmpty(filter)){
            pagedListLiveData = new LivePagedListBuilder<>(database.topicDao().dataSourcePagedList(forum_id),config)
                    .setInitialLoadKey(1)
                    .setFetchExecutor(Executors.newSingleThreadExecutor())
                    .setBoundaryCallback(new PagedList.BoundaryCallback<Topic>() {
                        @Override
                        public void onZeroItemsLoaded() {
                            super.onZeroItemsLoaded();
                            //база пустая и фильтр не установлен
                            loadTopics(forum_id, 0);
                        }
                    })
                    .build();
        } else {
            pagedListLiveData = new LivePagedListBuilder<>(database.topicDao().dataSourcePagedList(forum_id, filter), config)
                    .setFetchExecutor(Executors.newSingleThreadExecutor())
                    .build();
        }

        return  pagedListLiveData;
    }

    //при установке фильтра - "переподписываемся" к новым данным
    void setFilteredTopics(int forum_id, final String filter){

        filteredTopics.removeSource(topics);
        topics=createFilteredTopic(forum_id, filter);

        filteredTopics.addSource(topics, new Observer<PagedList<Topic>>() {
            @Override
            public void onChanged(@Nullable PagedList<Topic> topics) {
                filteredTopics.setValue(topics);
            }
        });
    }

    LiveData<PagedList<Topic>> getTopics(int forum_id, String filter) {
        setFilteredTopics(forum_id, filter);
        return filteredTopics;
    }



    LiveData<Integer> getTopicsCount(int forum_id, boolean isAttathed){
        return  database.topicDao().getCount(forum_id,isAttathed);
    }

    LiveData<WorkInfo> loadTopics(int forum_id, int topic_id){
        Data data = new Data.Builder()
                .putInt(TopicsWorker.EXTRA_ACTION, TopicsWorker.ACTION_LOAD_FROM_NETWORK)
                .putInt(TopicsWorker.EXTRA_FORUM_ID, forum_id)
                .putInt(TopicsWorker.EXTRA_TOPIC_ID, topic_id)
                .build();
        OneTimeWorkRequest simpleRequest = new OneTimeWorkRequest.Builder(TopicsWorker.class).setInputData(data).build();
        WorkManager.getInstance(application).enqueueUniqueWork("loadTopics", ExistingWorkPolicy.REPLACE,simpleRequest);
        LiveData<WorkInfo> result=WorkManager.getInstance(getApplication()).getWorkInfoByIdLiveData(simpleRequest.getId());
        result.observeForever(observer);
        return result;
    }

    PagedList<Topic> emptyTopicPagedList(){
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

    PagedList<Post> emptyPostPagedList(){
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
    LiveData<PagedList<Post>> getPosts(final int forum_id, final int topic_id){
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
     */
    private LiveData<PagedList<Post>> createFilteredPost(final int forum_id, final int topic_id, String filter){
        filter=filter.toLowerCase();
        LiveData<PagedList<Post>> pagedListLiveData;

        PagedList.Config config = new PagedList.Config.Builder()
                .setEnablePlaceholders(true)
                .setPageSize(10)
                //.setInitialLoadSizeHint(initial)
                .build();

        //SimpleSQLiteQuery query= new SimpleSQLiteQuery(String.format(Locale.ENGLISH,"SELECT * FROM post WHERE (forum_id=%d AND topic_id=%d) AND ((user LIKE '%%%s%%') OR (text LIKE '%%%s%%')) ORDER BY lastmod asc",forum_id,topic_id,filter,filter));
        //SimpleSQLiteQuery query= new SimpleSQLiteQuery(String.format(Locale.ENGLISH,"SELECT * FROM post WHERE (forum_id=%d AND topic_id=%d) AND ((user GLOB '[^a-zA-Z0-9_]%s[^a-zA-Z0-9_]') OR (text GLOB '[^a-zA-Z0-9_]%s[^a-zA-Z0-9_]')) ORDER BY lastmod asc",forum_id,topic_id,filter,filter));
        //SimpleSQLiteQuery query= new SimpleSQLiteQuery(String.format(Locale.ENGLISH,"SELECT * FROM post WHERE (forum_id=%d AND topic_id=%d) AND (text LIKE '%%%s%%' ) ORDER BY lastmod asc",forum_id,topic_id,filter,filter));

        if (TextUtils.isEmpty(filter)){
            pagedListLiveData = new LivePagedListBuilder<>(database.postDao().dataSourcePagedList(forum_id,topic_id),config)
                    //pagedListLiveData = new LivePagedListBuilder<>(database.postDao().dataSourcePagedListRaw(query),config)
                    .setFetchExecutor(Executors.newSingleThreadExecutor())
                    .setBoundaryCallback(new PagedList.BoundaryCallback<Post>() {
                        @Override
                        public void onZeroItemsLoaded() {
                            super.onZeroItemsLoaded();
                            //база пустая и фильтр не установлен
                            loadPosts(forum_id, topic_id, 0);
                        }
                    })
                    .build();
        } else {
            pagedListLiveData = new LivePagedListBuilder<>(database.postDao().dataSourcePagedList(forum_id, topic_id, filter), config)
                    .setFetchExecutor(Executors.newSingleThreadExecutor())
                    .build();
        }

        return  pagedListLiveData;
    }

    //при установке фильтра - "переподписываемся" к новым данным
    void setFilteredPosts(int forum_id, int topic_id, final String filter){

        filteredPosts.removeSource(posts);
        posts=createFilteredPost(forum_id, topic_id, filter);

        filteredPosts.addSource(posts, new Observer<PagedList<Post>>() {
            @Override
            public void onChanged(@Nullable PagedList<Post> posts) {
                filteredPosts.setValue(posts);
            }
        });
    }

    LiveData<PagedList<Post>> getPosts(int forum_id, int topic_id, String filter) {
        setFilteredPosts(forum_id, topic_id, filter);
        return filteredPosts;
    }

    LiveData<WorkInfo> loadPosts(int forum_id, int topic_id, int page_id){
        Data data = new Data.Builder()
                .putInt(PostsWorker.EXTRA_ACTION, PostsWorker.ACTION_LOAD_FROM_NETWORK)
                .putInt(PostsWorker.EXTRA_FORUM_ID, forum_id)
                .putInt(PostsWorker.EXTRA_TOPIC_ID, topic_id)
                .putInt(PostsWorker.EXTRA_PAGE_ID, page_id)
                .build();
        OneTimeWorkRequest simpleRequest = new OneTimeWorkRequest.Builder(PostsWorker.class).setInputData(data).build();

        WorkManager.getInstance(application).enqueueUniqueWork("loadPosts", ExistingWorkPolicy.REPLACE,simpleRequest);
        LiveData<WorkInfo> result=WorkManager.getInstance(getApplication()).getWorkInfoByIdLiveData(simpleRequest.getId());
        result.observeForever(observer);
        return result;
    }

    LiveData<Topic> getTopicLive(int forum_id, int topic_id){
        return database.topicDao().getTopicLive(forum_id,topic_id);
    }

    LiveData<User> getUser(String userNick){
        return database.userDao().getUserLive(userNick);
    }

    void checkTopicBookmark(Topic topic) {
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

    void checkForumBookmark(Forum forum) {
        Data data = new Data.Builder()
                .putInt(ForumsWorker.EXTRA_ACTION, ForumsWorker.ACTION_CHANGE_BOOKMARK)
                .putInt(ForumsWorker.EXTRA_FORUM_ID, forum.id)
                .build();
        OneTimeWorkRequest simpleRequest = new OneTimeWorkRequest.Builder(ForumsWorker.class)
                .setInputData(data)
                .build();
        WorkManager.getInstance(application).beginUniqueWork("checkForumBookmark", ExistingWorkPolicy.REPLACE,simpleRequest).enqueue();
    }

    void loginSite(String login, String password, final NetworkListener loginListener){
        //пробуем залогиниться
        App.getPostApi().login("login",login,password).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    try {
                        //вытаскиваем html
                        StringBuilder text=readStream(response.body().byteStream(),"utf-8");
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
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
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

    void logOut(){
        ((App)getApplication()).clearCookies();
        privIsLogin.setValue(((App)getApplication()).isLogin());
    }


    void votePost(final Post post, VoteType voteType, final NetworkListener voteListener){
        App.getPostApi().vote(String.valueOf(post.forum_id),String.valueOf(post.topic_id),String.valueOf(post.id),voteType.name()).enqueue(new Callback<VoteResponse>() {
            @Override
            public void onResponse(@NonNull Call<VoteResponse> call, @NonNull Response<VoteResponse> response) {
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
            public void onFailure(@NonNull Call<VoteResponse> call, @NonNull Throwable t) {
                if (voteListener!=null)
                    voteListener.onError(t.getMessage());
            }
        });
    }

    void sendPost(final int forum_id, final int topic_id, final int post_id, String subject, String body, final NetworkListener postListener) {
        App.getPostApi().post(String.valueOf(forum_id),String.valueOf(topic_id),String.valueOf(post_id),subject,body,"Y").enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    try {
                        StringBuilder text=readStream(response.body().byteStream(),"utf-8");
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
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                if (postListener!=null)
                    postListener.onError(t.getMessage());
            }
        });
    }

    void sendModerator(int forum_id,int post_id, final NetworkListener postListener){
        //https://www.e1.ru/talk/forum/moderator.php?f=22&m=64253&mobile=1
        App.getPostApi().sendModerator(forum_id,post_id).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    try {
                        StringBuilder text=readStream(response.body().byteStream(),"windows-1251");

                        if (text.toString().contains("Спасибо")){
                            if (postListener!=null)
                                postListener.onSuccess("");
                        } else {
                            if (postListener!=null)
                                postListener.onError(text.toString());
                        }
                    }catch(IOException e){
                        if (postListener!=null)
                            postListener.onError(e.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                if (postListener!=null)
                    postListener.onError(t.getMessage());
            }
        });
    }

    void sendLK(String user_id, final String theme, final String body, final NetworkListener postListener){
        App.getPostApi().prepareLK(user_id).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    try {
                        final StringBuilder text = readStream(response.body().byteStream(), "windows-1251");
                        HashMap<String, String> values = SiteParser.extractFormValues(text.toString());

                        if (values.size() == 0) {
                            if (postListener != null)
                                postListener.onError(getString(R.string.send_private_error));
                            return;
                        }

                        App.getPostApi().sendLK(
                                "1",
                                values.get("type_message"),
                                values.get("type_message_checksum"),
                                values.get("type_send"),
                                values.get("type_send_checksum"),
                                values.get("service_id"),
                                values.get("service_id_checksum"),
                                values.get("sender"),
                                values.get("sender_checksum"),
                                values.get("recipient"),
                                values.get("recipient_checksum"),
                                values.get("dialog_id"),
                                values.get("dialog_id_checksum"),
                                values.get("protected_list"),
                                values.get("protected_list_checksum"),
                                theme,
                                body,
                                SiteParser.URLEncodeString("Отправить")
                        ).enqueue(new Callback<ResponseBody>() {
                            @Override
                            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                                try {
                                    StringBuilder result = readStream(response.body().byteStream(), "windows-1251");
                                    if (result.toString().contains("Ваше сообщение отправлено")) {
                                        if (postListener != null)
                                            postListener.onSuccess("");
                                    } else {
                                        if (postListener != null)
                                            postListener.onError(text.toString());
                                    }
                                } catch (IOException e) {
                                    if (postListener != null)
                                        postListener.onError(e.getMessage());
                                }
                            }

                            @Override
                            public void onFailure(Call<ResponseBody> call, Throwable t) {
                                if (postListener != null)
                                    postListener.onError(t.getMessage());
                            }
                        });
                    } catch (IOException e) {
                        if (postListener!=null)
                            postListener.onError(e.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                if (postListener!=null)
                    postListener.onError(t.getMessage());
            }
        });
    }

    void sendMail(String user_id, final String theme, final String body, final NetworkListener postListener) {
        App.getPostApi().prepareMail(user_id).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    try {
                        final StringBuilder text = readStream(response.body().byteStream(), "windows-1251");
                        HashMap<String, String> values = SiteParser.extractFormValues(text.toString());
                        values.put("subject",theme);
                        values.put("text",body);
                        values.put("save",SiteParser.URLEncodeString("Отправить"));

                    } catch (IOException e) {
                        if (postListener!=null)
                            postListener.onError(e.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                if (postListener!=null)
                    postListener.onError(t.getMessage());
            }
        });
    }

    void aboutUser(final int user_id, final NetworkListener networkListener){
        App.getPostApi().aboutUser(user_id).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    try {
                        StringBuilder text = readStream(response.body().byteStream(), "windows-1251");
                        final HashMap<String, String> values = SiteParser.extractTableValues(text.toString());
                        if (values.size()==0){
                            if (networkListener != null)
                                networkListener.onError(getString(R.string.about_user_error));
                            return;
                        } else {
                            StringBuilder result = new StringBuilder();
                            for (Map.Entry<String, String> item : values.entrySet()) {
                                result.append("\n").append(String.format("<b>%s</b>: %s<br>",item.getKey(), item.getValue()));
                            }
                            //сообщение
                            if (networkListener != null)
                                networkListener.onSuccess(result.toString());
                        }

                        //сохраняем данные о пользователе в базу
                        Executors.newSingleThreadExecutor().submit(new Runnable() {
                            @Override
                            public void run() {
                                //не затираем все поля а только обновляем нужные
                                User user=database.userDao().getUser(user_id);
                                user.info=values;
                                //сохранение в базу
                                database.userDao().update(user);
                            }
                        });

                    } catch (IOException e) {
                        if (networkListener != null)
                            networkListener.onError(e.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                if (networkListener != null)
                    networkListener.onError(t.getMessage());
            }
        });
    }

    private String getString(int resId){
        return getApplication().getString(resId);
    }

    private StringBuilder readStream(InputStream is, String charsetName) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is, charsetName));
        //вытаскиваем html
        StringBuilder text=new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            text.append(line);
        }
        return text;
    }
}
