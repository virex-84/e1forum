package com.virex.e1forum.network;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class UserAgentInterceptor  implements Interceptor {
    //final String userAgent="e1forum.".concat(BuildConfig.VERSION_NAME);
    private final String userAgent="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.90 Safari/537.36";

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request original = chain.request();

        Request request = original.newBuilder()
                .header("User-Agent", userAgent)
                //.addHeader("Content-Type", "text/html; charset=windows-1251")
                .method(original.method(), original.body())
                .build();
        return chain.proceed(request);
    }
}
