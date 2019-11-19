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

    public interface ChangeListener {
        void onChange(List<Cookie> cookies);
    }

    private static final String PREF_COOKIES="PREF_COOKIES";
    private SharedPreferences sharedPreferences;
    private List<Cookie> cookies;
    private boolean isAnonimouseMode =false;
    private ChangeListener changeListener;

    public PrefCookieJar(@NonNull SharedPreferences sharedPreferences, ChangeListener changeListener){
        this.sharedPreferences=sharedPreferences;
        this.changeListener=changeListener;
    }

    @Override
    public void saveFromResponse(@NonNull HttpUrl url, @NonNull List<Cookie> cookies) {

        if (isAnonimouseMode) return;

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


/*
        //удаление пустых куков
        Iterator<Cookie> iterator = this.cookies.iterator();
        while (iterator.hasNext()) {
            Cookie item = iterator.next();

            //if (TextUtils.isEmpty(item.value()))
            if (item.value().contains("deleted"))
                iterator.remove();
        }

 */


        //подготовка для сохранения куков
        HashSet<String> items = new HashSet<>();
        for(Cookie cookie: this.cookies){
            items.add(cookie.toString());
        }

        //сохраняем куки
        SharedPreferences.Editor memes = sharedPreferences.edit();
        memes.putStringSet(PREF_COOKIES, items).apply();

        if (changeListener!=null)
            changeListener.onChange(cookies);
    }

    @Override
    public List<Cookie> loadForRequest(@NonNull HttpUrl url) {
        if (isAnonimouseMode) return new ArrayList<>();

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

    /*
    public void init(){
        //HttpUrl url = HttpUrl.parse("https://m.e.1ru");
        cookies = new ArrayList<>();
        try {
            HashSet<String> preferences = (HashSet<String>) sharedPreferences.getStringSet(PREF_COOKIES, new HashSet<String>());
            for (String item : preferences) {
                try {

                    //String[] rawCookie = item.split(";");
                    //for (String line : rawCookie){
                    //    String NameAndValue[] = line.trim().split("=");
                    //    String name=NameAndValue[0].trim();
                    //    String value="";
                    //    if (NameAndValue.length>1)
                    //        value=NameAndValue[1].trim();

                    //    if (name.contains("domain")) {
                    //        cookies.add(Cookie.parse(HttpUrl.parse("http://"+value), item));
                    //        //break;
                    //    }
                    //}

                    cookies.add(Cookie.parse(new HttpUrl.Builder().scheme("https").host("e1.ru").build(), item));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //удаляем null элементы
        cookies.removeAll(Collections.singleton(null));
    }

     */


    /*
    public List<Cookie> getCookies(String initialUrl){
        HttpUrl url = HttpUrl.parse(initialUrl);

        List<Cookie> localCookies = new ArrayList<>();
        try {
            HashSet<String> preferences = (HashSet<String>) sharedPreferences.getStringSet(PREF_COOKIES, new HashSet<String>());
            for (String item : preferences) {
                try {
                    localCookies.add(Cookie.parse(url, item));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //удаляем null элементы
        localCookies.removeAll(Collections.singleton(null));

        return localCookies;
    }
     */

    public HashSet<String> loadCookies(){
        return (HashSet<String>) sharedPreferences.getStringSet(PREF_COOKIES, new HashSet<String>());
    }


    public void clearCookies(){
        SharedPreferences.Editor memes = sharedPreferences.edit();
        memes.remove(PREF_COOKIES).commit();

        if (cookies!=null)
            cookies.clear();
    }

    //анонимный режим - когда куки не выдаются сайту и не сохраняются в приложении с сайта
    public void setAnonimouseMode(boolean value){
        this.isAnonimouseMode =value;
    }
}
