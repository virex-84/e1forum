package com.virex.e1forum.network;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

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

    //https://www.e1.ru/talk/forum/moderator.php?f=22&m=64253&mobile=1
    @GET("https://www.e1.ru/talk/forum/moderator.php")
    Call<ResponseBody> sendModerator(@Query("f") int forum_id, @Query("m") int post_id);

    //https://www.e1.ru/talk/forum/pm/message.php?u=21580372p5266bae852ef5bc4f9b242e13a6bdfe8
    @GET("https://www.e1.ru/talk/forum/pm/message.php")
    Call<ResponseBody> prepareLK(@Query("u") String user_id);

    @FormUrlEncoded
    @POST("https://www.e1.ru/talk/forum/pm/message.php")
    Call<ResponseBody> sendLK(@Field("_submit") String _submit,
                              @Field("type_message") String type_message,
                              @Field("type_message_checksum") String type_message_checksum,
                              @Field("type_send") String type_send,
                              @Field("type_send_checksum") String type_send_checksum,
                              @Field("service_id") String service_id,
                              @Field("service_id_checksum") String service_id_checksum,
                              @Field("sender") String sender,
                              @Field("sender_checksum") String sender_checksum,
                              @Field("recipient") String recipient,
                              @Field("recipient_checksum") String recipient_checksum,
                              @Field("dialog_id") String dialog_id,
                              @Field("dialog_id_checksum") String dialog_id_checksum,
                              @Field("protected_list") String protected_list,
                              @Field("protected_list_checksum") String protected_list_checksum,
                              @Field(value="theme", encoded=true) String theme,
                              @Field(value="body", encoded=true) String body,
                              @Field(value="send", encoded=true) String send
    );

    //www.e1.ru/talk/forum/personal_info.php?user=
    @GET("https://www.e1.ru/talk/forum/personal_info.php")
    Call<ResponseBody> aboutUser(@Query("user") int user_id);
}
