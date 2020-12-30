package com.virex.e1forum.common;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;

import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
    public static String pref_dark_theme="pref_dark_theme";

    /**
     * Observes the given {@link LiveData} until the first change.
     * If the LiveData's value is available, the {@link Observer} will be
     * called right away.
     */
    @UiThread
    public static <T> void observeOnce(final LiveData<T> liveData,
                                       LifecycleOwner owner, final Observer<T> observer) {
        liveData.observe(owner, new Observer<T>() {
            @Override
            public void onChanged(@Nullable T t) {
                observer.onChanged(t);
                liveData.removeObserver(this);
            }
        });
    }

    /**
     * Same as {@link #observeOnce(LiveData, LifecycleOwner, Observer)},
     * but without a {@link LifecycleOwner}.
     *
     * Warning: Do NOT call from objects that have a lifecycle.
     */
    @UiThread
    public static <T> void observeForeverOnce(final LiveData<T> liveData,
                                              final Observer<T> observer) {
        liveData.observeForever(new Observer<T>() {
            @Override
            public void onChanged(@Nullable T t) {
                observer.onChanged(t);
                liveData.removeObserver(this);
            }
        });
    }

    //выделяем текст
    public static SpannableStringBuilder makeSpanText(Context context, SpannableStringBuilder source, String findtext, int foregroundColor, int backgroundColor){
        if (context==null || TextUtils.isEmpty(source) || TextUtils.isEmpty(findtext)) return null;

        //убираем специфические для поиска символы
        Pattern word = Pattern.compile(findtext.toLowerCase().replaceAll("[-\\[\\]^/,'*:.!><~@#$%+=?|\"\\\\()]+", ""),Pattern.CASE_INSENSITIVE);
        Matcher match = word.matcher(source.toString().toLowerCase());

        while (match.find()) {
            //ForegroundColorSpan fcs = new ForegroundColorSpan(ContextCompat.getColor(context, R.color.white));
            ForegroundColorSpan fcs = new ForegroundColorSpan(foregroundColor);
            //BackgroundColorSpan bcs = new BackgroundColorSpan(ContextCompat.getColor(context, R.color.colorPrimary));
            BackgroundColorSpan bcs = new BackgroundColorSpan(backgroundColor);
            source.setSpan(fcs, match.start(), match.end(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            source.setSpan(bcs, match.start(), match.end(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        }

        return source;
    }

    public static void writeFileOnInternalStorage(Context context,String fileName, byte[] body){
        try{
            String download= Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
            File file = new File(download,fileName);
            if(!file.exists()){
                file.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(body);
            fos.close();
        }catch (Exception e){
            e.printStackTrace();

        }
    }

    //смена темы "дневная/ночная"
    public static void changeTheme(SharedPreferences sharedPreferences){
        boolean is_dark_theme=!sharedPreferences.getBoolean(pref_dark_theme,false);
        sharedPreferences.edit().putBoolean(pref_dark_theme,is_dark_theme).apply();
    }

    public static void setDarkTheme(boolean is_dark_theme){
        if (is_dark_theme){
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    public static boolean isDarkTheme(SharedPreferences sharedPreferences){
        return sharedPreferences.getBoolean(pref_dark_theme,false);
    }

}
