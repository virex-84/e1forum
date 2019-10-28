package com.virex.e1forum.network;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * Работа с сетью. Форумы
 */
public interface PostWebService {
    //https://m.e1.ru/f/152/
    //https://m.e1.ru/f/234/212/

    @GET("https://m.e1.ru/f/{fid}/{tid}/p/{pid}/")
    Call<ResponseBody> getPosts(@Path("fid") int forum_id, @Path("tid") int topic_id, @Path("pid") int page_id);


    @FormUrlEncoded
    @POST("https://passport.e1.ru/login/?token_name=ngs_token&return=https%3A%2F%2Fm.e1.ru%2Ff%2F67%2F17884060%2F")
    Call<ResponseBody> login(@Field("go") String go,
                                @Field("login") String login,
                                @Field("password") String password);


    @FormUrlEncoded
    @Headers("X-Requested-With:XMLHttpRequest")
    @POST("https://m.e1.ru/f/vote.php")
    Call<VoteResponse> vote(@Field("f") String forum_id,
                             @Field("t") String topic_id,
                             @Field("m") String post_id,
                             @Field("v") String action);
}
