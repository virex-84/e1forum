package com.virex.e1forum;

import android.app.Application;
import android.text.TextUtils;
import android.util.Log;

import com.virex.e1forum.common.PrefCookieJar;
import com.virex.e1forum.network.ForumWebService;
import com.virex.e1forum.network.PostWebService;
import com.virex.e1forum.network.TopicWebService;
import com.virex.e1forum.network.UserAgentInterceptor;

import java.util.List;

import okhttp3.Cookie;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class App extends Application {

    private PrefCookieJar prefCookieJar;

    private static ForumWebService forumApi;
    private static TopicWebService topicApi;
    private static PostWebService postApi;
    //private static AnketaWebService anketaApi;

    public static ForumWebService getForumApi() {
        return forumApi;
    }
    public static TopicWebService getTopicApi() {
        return topicApi;
    }
    public static PostWebService getPostApi() {
        return postApi;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        //устанавливаем контроль за куками
        prefCookieJar=new PrefCookieJar(getSharedPreferences("APPP", MODE_PRIVATE), new PrefCookieJar.ChangeListener() {
            @Override
            public void onChange(List<Cookie> cookies) {
                Log.i("COOKIES  change","==START==\n");
                Log.i("","\n"+TextUtils.join("\n", cookies));
                Log.i("COOKIES  change","==END==\n");
            }
        });

        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();

        httpClient.addInterceptor(new UserAgentInterceptor());//добавим UserAgent
        //httpClient.addInterceptor(new AddCookiesInterceptor(getApplicationContext()));//выдача куков на сайт - заменено PrefCookieJar
        //httpClient.addInterceptor(new ReceivedCookiesInterceptor(getApplicationContext()));//сохранение куков с сайта - заменено PrefCookieJar

        OkHttpClient client = httpClient
                .followRedirects(true)
                .followSslRedirects(true)
                //куки
                .cookieJar(prefCookieJar)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://www.e1.ru/talk/") //Базовая часть адреса
                .addConverterFactory(GsonConverterFactory.create()) //Конвертер, необходимый для преобразования JSON'а в объекты при POST запросах
                .client(client)
                .build();

        //создаем объект, при помощи которого будем выполнять запросы
        forumApi = retrofit.create(ForumWebService.class);
        topicApi = retrofit.create(TopicWebService.class);
        postApi = retrofit.create(PostWebService.class);
    }

    //очистка всех куков
    public void clearCookies(){
        prefCookieJar.clearCookies();
    }

    //узнаем залогинены мы или нет
    public boolean isLogin(){
        try {
            for (Cookie cookie : prefCookieJar.getCookies()) {
                //гарантия того что мы залогинены - этот хитрый кук
                if (cookie.name().equals("e1_ttq") && !TextUtils.isEmpty(cookie.value())) return true;
            }
        } catch(NullPointerException e){

        }
        return false;
    }


    public boolean initIsLogin(){
        for (String cookie : prefCookieJar.loadCookies()){
            //гарантия того что мы залогинены - этот хитрый кук
            if (cookie.contains("e1_ttq") && cookie.contains("key")){
                    return true;
            }
        }
        return false;
    }

}
