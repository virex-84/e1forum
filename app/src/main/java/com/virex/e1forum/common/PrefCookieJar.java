package com.virex.e1forum.common;

import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

/**
 * Контроль куков: сохранение, чтение, анонимный режим
 */
public class PrefCookieJar implements CookieJar {

    private static final String PREF_COOKIES="PREF_COOKIES";
    private SharedPreferences sharedPreferences;
    private List<Cookie> cookies;
    private boolean isAnonimusMode=false;

    public PrefCookieJar(@NonNull SharedPreferences sharedPreferences){
        this.sharedPreferences=sharedPreferences;
    }

    @Override
    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
        if (isAnonimusMode) return;

        //замена одинаковых по имени куков новым значением
        for(int i = 0; i< this.cookies.size(); i++){
            for(int y=0; y<cookies.size(); y++) {
                if (this.cookies.get(i).name().contains(cookies.get(y).name()))
                    this.cookies.set(i, cookies.get(y));
            }
        }

        //добавление новых куков
        for(int y=0; y<cookies.size(); y++) {
            if (!this.cookies.contains(cookies.get(y)))
                this.cookies.add(cookies.get(y));
        }

        //подготовка для сохранения куков
        HashSet<String> items = new HashSet<>();
        for(Cookie cookie: this.cookies){
            items.add(cookie.toString());
        }

        //сохраняем куки
        SharedPreferences.Editor memes = sharedPreferences.edit();
        memes.putStringSet(PREF_COOKIES, items).apply();
        memes.commit();
    }

    @Override
    public List<Cookie> loadForRequest(HttpUrl url) {
        if (isAnonimusMode) return new ArrayList<>();

        if (cookies ==null) {
            cookies = new ArrayList<>();

            //первый раз (после запуска приложения) загружаем куки
            try {
                HashSet<String> preferences = (HashSet<String>) sharedPreferences.getStringSet(PREF_COOKIES, new HashSet<String>());
                for (String item : preferences) {
                    try {
                        cookies.add(Cookie.parse(url, item));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        //удаляем null элементы
        cookies.removeAll(Collections.singleton(null));

        return cookies;
    }

    public List<Cookie> getCookies(){
        return cookies;
    }

    public void clearCookies(){
        SharedPreferences.Editor memes = sharedPreferences.edit();
        memes.remove(PREF_COOKIES).apply();
        memes.commit();
    }

    //анонимный режим - когда куки не выдаются сайту и не сохраняются в приложении с сайта
    public void setAnonimusMode(boolean value){
        this.isAnonimusMode=value;
    }
}
