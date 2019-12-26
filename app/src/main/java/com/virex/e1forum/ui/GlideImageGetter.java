package com.virex.e1forum.ui;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.Html;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.virex.e1forum.R;
import com.virex.e1forum.common.GlideApp;


public class GlideImageGetter implements Html.ImageGetter,Drawable.Callback {
    private Resources resources;
    private TextView textView;

    GlideImageGetter(Resources resources, TextView target) {
        super();
        this.resources=resources;
        this.textView = target;
    }

    @Override
    public Drawable getDrawable(String source) {
        final FutureDrawable result = new FutureDrawable(resources);

        //загружаем "пустую картинку"
        Drawable empty = ContextCompat.getDrawable(textView.getContext(), R.drawable.ic_empty_image);
        result.setBounds(0, 0, empty.getIntrinsicWidth(), empty.getIntrinsicHeight());
        result.setDrawable(empty);

        final String imgsource;
        //if (source.contains("https://www.e1.ru/talk/forum/images/smiles2/")) {
        //поменялся путь к локальным смайлам форума
        if (source.contains("https://cdn.e1.ru/talk/forum/images/smiles2/")) {
            imgsource = source.replace("https://cdn.e1.ru/talk/forum/images/smiles2/", "file:///android_asset/icons/");
            result.isLocalImage=true;
        } else
            imgsource=source;

        GlideApp.with(textView)
                .asDrawable()
                .load(Uri.parse(imgsource))
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .into(new SimpleTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {

                        int maxWidth = 300;
                        if (imgsource.contains(".gif")) {
                            GifDrawable gifDrawable = (GifDrawable) resource;

                            if (gifDrawable.getIntrinsicWidth() > maxWidth) {
                                float aspectRatio = (float) gifDrawable.getIntrinsicHeight() / (float) gifDrawable.getIntrinsicWidth();
                                gifDrawable.setBounds(0, 0, maxWidth, (int) (aspectRatio * maxWidth));
                            } else {
                                int width=(int) (gifDrawable.getIntrinsicWidth() * 2.5) ;
                                int height=(int) (gifDrawable.getIntrinsicHeight() * 2.5);
                                  gifDrawable.setBounds(0, 0, width, height);
                            }

                            gifDrawable.setCallback(GlideImageGetter.this);
                            gifDrawable.start();
                            result.isGif=true;
                            result.setDrawable(gifDrawable);

                        } else {
                            if (resource.getIntrinsicWidth() > maxWidth) {
                                float aspectRatio = (float) resource.getIntrinsicHeight() / (float) resource.getIntrinsicWidth();
                                resource.setBounds(0, 0, maxWidth, (int) (aspectRatio * maxWidth));
                            } else {
                                resource.setBounds(0, 0, resource.getIntrinsicWidth(), resource.getIntrinsicHeight());
                            }
                            result.setDrawable(resource);
                        }
                    }
                });

        return result;
    }

    @Override
    public void invalidateDrawable(@NonNull Drawable drawable) {
        textView.invalidate();
    }

    @Override
    public void scheduleDrawable(@NonNull Drawable drawable, @NonNull Runnable runnable, long l) {
        textView.postDelayed(runnable, l);
    }

    @Override
    public void unscheduleDrawable(@NonNull Drawable drawable, @NonNull Runnable runnable) {
        textView.removeCallbacks(runnable);
    }

    public class FutureDrawable extends Drawable  {
        private Drawable drawable;
        private boolean isGif=false;
        public boolean isLocalImage=false;

        FutureDrawable(Resources res) {
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            if(drawable != null) {
                drawable.draw(canvas);
            }
        }

        @NonNull
        @Override
        public Drawable getCurrent() {
            return drawable;
        }

        @Override
        public void setAlpha(int i) {
        }

        @Override
        public void setColorFilter(@Nullable ColorFilter colorFilter) {
        }

        @Override
        public int getOpacity() {
            return PixelFormat.UNKNOWN;
        }

        public void setDrawable(Drawable drawable){
            this.drawable=drawable;

            int drawableWidth = drawable.getIntrinsicWidth();
            int drawableHeight = drawable.getIntrinsicHeight();

            if (isGif){
                drawableWidth = (int) (drawable.getIntrinsicWidth() * 2.5);
                drawableHeight = (int) (drawable.getIntrinsicHeight()* 2.5);
                ((GifDrawable)this.drawable).start();
            }

            int maxWidth = textView.getMeasuredWidth();
            if (drawableWidth > maxWidth) {
                int calculatedHeight = maxWidth * drawableHeight / drawableWidth;
                drawable.setBounds(0, 0, maxWidth, calculatedHeight);
                setBounds(0, 0, maxWidth, calculatedHeight);
            } else {
                drawable.setBounds(0, 0, drawableWidth, drawableHeight);
                setBounds(0, 0, drawableWidth, drawableHeight);
            }

            //для изменения размера картинок
            textView.setText(textView.getText());
        }

    }
}
