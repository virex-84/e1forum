package com.virex.e1forum.common;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;

import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
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



}
