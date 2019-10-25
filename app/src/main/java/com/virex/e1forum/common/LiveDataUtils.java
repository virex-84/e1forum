package com.virex.e1forum.common;

import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

public class LiveDataUtils {
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

}
