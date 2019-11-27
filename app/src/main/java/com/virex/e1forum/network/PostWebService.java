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

    @GET("https://m.e1.ru/f/{fid}/{tid}/p/{last_pid}/")
    Call<ResponseBody> getPosts(@Path("fid") int forum_id, @Path("tid") int topic_id, @Path("last_pid") int last_pid);


    @FormUrlEncoded
    //@POST("https://passport.e1.ru/login/?token_name=ngs_token&return=https%3A%2F%2Fm.e1.ru%2Ff%2F67%2F17884060%2F")
    //@POST("https://passport.e1.ru/login/?token_name=ngs_token&return=https%3A%2F%2Fm.e1.ru%2Ff%2F")
    //@POST("https://passport.e1.ru/signin/?return=https%3A%2F%2Fm.e1.ru%2Ff%2F&token_name=ngs_token")
    //@POST("https://passport.e1.ru/signin/?return=https%3A%2F%2Fm.e1.ru%2Ff%2F&token_name=ngs_token&cookie_disabled=0")
    @POST("https://passport.e1.ru/login/?token_name=ngs_token&return=https%3A%2F%2Fm.e1.ru%2Ff%2F")
    @Headers({"referer: https://m.e1.ru/f/?login=y",
            "sec-fetch-mode: navigate",
            "sec-fetch-site: same-site",
            "sec-fetch-user: ?1",
            "upgrade-insecure-requests: 1"})
    Call<ResponseBody> login(
                             @Field("sub") String sub,
                                @Field("login") String login,
                                @Field("password") String password);


    @FormUrlEncoded
    @Headers("X-Requested-With:XMLHttpRequest")
    @POST("https://m.e1.ru/f/vote.php")
    Call<VoteResponse> vote(@Field("f") String forum_id,
                             @Field("t") String topic_id,
                             @Field("m") String post_id,
                             @Field("v") String action);

    @FormUrlEncoded
    @Headers("X-Requested-With:XMLHttpRequest")
    @POST("https://m.e1.ru/f/post.php")
    Call<ResponseBody> post(@Field("f") String forum_id,
                            @Field("t") String topic_id,
                            @Field("p") String post_id,
                            @Field(value="subject", encoded=true) String subject,
                            @Field(value="body", encoded=true) String body,
                            @Field("email_reply") String email_reply
                            );
}
