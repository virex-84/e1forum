package com.virex.e1forum.network;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Url;

/**
 * Работа с сетью. Форумы
 */
public interface TopicWebService {
    //https://m.e1.ru/f/152/

    @GET("https://m.e1.ru/f/{id}/p/{tid}")
    Call<ResponseBody> getTopics(@Path("id") int forum_id, @Path("tid") int topic_id);
}
