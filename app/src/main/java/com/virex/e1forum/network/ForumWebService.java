package com.virex.e1forum.network;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;

/**
 * Работа с сетью. Форумы
 */
public interface ForumWebService {
    //https://www.e1.ru/talk/forum/
    //https://www.e1.ru/talk/forum/list.php?f=67

    @GET("https://www.e1.ru/talk/forum/")
    Call<ResponseBody> getForums();
}
