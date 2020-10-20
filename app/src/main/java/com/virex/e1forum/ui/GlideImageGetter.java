package com.virex.e1forum.ui;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
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
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.virex.e1forum.R;
import com.virex.e1forum.common.GlideApp;

import java.util.HashMap;


public class GlideImageGetter implements Html.ImageGetter,Drawable.Callback {
    private final Resources resources;
    private final TextView textView;
    //private ArrayList<Pair<String, String>> icons = new ArrayList<Pair<String, String>>();
    HashMap<String, String> icons = new HashMap<String, String>();



    GlideImageGetter(Resources resources, TextView target) {
        super();
        this.resources=resources;
        this.textView = target;
        icons.put(":-)","smile3.gif");
        icons.put(";-)","wink.gif");
        icons.put(":-D","hollywood.gif");
        icons.put(":lol:","lol.gif");
        icons.put(":-(","sad.gif");
        icons.put(":weep:","cray.gif");
        icons.put(":-p","blum2.gif");
        icons.put(":hi:","preved.gif");
        icons.put(":cool:","dirol.gif");
        icons.put(":super:","dance4.gif");
        icons.put(":ultra:","rtfm.gif");
        icons.put(":beach:","beach.gif");
        icons.put("8(","shok.gif");
        icons.put(":fotku:","fotku.gif");
        icons.put(":-o","blush2.gif");
        icons.put(":mad:","crazy.gif");
        icons.put(":confused:","confused.gif");
        icons.put(":facepalm:","facepalm.gif");
        icons.put(":vote:","wild.gif");
        icons.put(":away:","suicide2.gif");
        icons.put(":help:","help.gif");
        icons.put(":kiss:","air_kiss.gif");
        icons.put(":gun:","gigakach_01.gif");
        icons.put(":bs:","mda.gif");
        icons.put(":flowers:","i_daisy.gif");
        icons.put(":beer:","drinks.gif");
        icons.put(":food:","d_sweet.gif");
        icons.put(":bayan:","laie_48.gif");
        icons.put(":cen:","smile_27.gif");
        icons.put(":popcorn:","popcorm2.gif");
        icons.put(":nunu:","threaten.gif");
        icons.put(":smoke:","smoke.gif");
        icons.put(":hihiks:","sarcastic_hand.gif");
        icons.put(":box:","kez_02.gif");
        icons.put(":vis:","hang1.gif");
        icons.put(":nud:","mega_shok.gif");
        icons.put(":figa:","snooks.gif");
        icons.put(":ban:","close_tema.gif");
        icons.put(":appl:","clapping.gif");
        icons.put(":bes:","diablo.gif");
        icons.put(":manyak:","vampire.gif");
        icons.put(":suxx:","dash1.gif");
        icons.put(":puke:","bad.gif");
        icons.put(":write:","write.gif");
        icons.put(":exclam:","exclam.gif");
        icons.put(":disco:","disco.gif");
        icons.put(":heat:","heat.gif");
        icons.put(":niceread:","niceread.gif");
        icons.put(":coolsaint:","coolsaint.gif");
        icons.put(":redcard:","redcard.gif");
        icons.put(":cold:","cold.gif");
        icons.put(":fear:","fear.gif");
        icons.put(":fizra:","fizra.gif");
        icons.put(":music:","music.gif");
        icons.put(":ded+1:","ded.gif");
        icons.put(":hohoho:","hohoho.gif");
        icons.put(":petard:","petard.gif");
        icons.put(":snowball:","snowball.gif");
        icons.put(":biggun:","biggun.gif");
        icons.put(":commando:","commando.gif");
        icons.put(":crazypilot:","crazypilot.gif");
        icons.put(":girlkiss:","girlkiss.gif");
        icons.put(":military:","military.gif");
        icons.put(":pogranichnik:","pogranichnik.gif");
        icons.put(":boykissgirl:","boykissgirl.gif");
        icons.put(":girldance:","girldance.gif");
        icons.put(":pinkglasses:","pinkglasses.gif");
        icons.put(":spruceup:","spruceup.gif");
        icons.put(":teddy:","teddy.gif");
        icons.put(":whirlindance:","whirlindance.gif");
        icons.put(":stadium:","stadium.gif");
        icons.put(":cheerleading:","cheerleading.gif");
        icons.put(":fire:","fire.gif");
        icons.put(":laie:","laie.gif");
        icons.put(":padre:","padre.gif");
        icons.put(":boom:","boom.gif");
        icons.put(":girl_sigh:","girl_sigh.gif");
        icons.put(":girl_witch:","girl_witch.gif");
        icons.put(":king:","king.gif");
        icons.put(":war:","war.gif");
        icons.put(":gifts:","gifts.gif");
        icons.put(":sled:","sled.gif");
        icons.put(":jump:","jump.gif");

    }

    @Override
    public Drawable getDrawable(String source) {
        if (source==null) return null;

        final FutureDrawable result = new FutureDrawable(resources);

        //загружаем "пустую картинку"
        Drawable empty = ContextCompat.getDrawable(textView.getContext(), R.drawable.ic_empty_image);
        result.setBounds(0, 0, empty.getIntrinsicWidth(), empty.getIntrinsicHeight());
        result.setDrawable(empty);

        final String imgsource;
        //if (source.contains("https://cdn.e1.ru/talk/forum/static/images/smiles2/")) {
        //поменялся путь к локальным смайлам форума
        if (source.contains("local=")) {
            //imgsource = source.replace("local=", "file:///android_asset/icons/");
            String src=icons.get(source.replace("local=",""));
            imgsource = source.replace(source, "file:///android_asset/icons/"+src);
            result.isLocalImage=true;
        } else
            imgsource=source;

        GlideApp.with(textView)
                .asDrawable()
                .placeholder(R.drawable.ic_empty_image)
                .error(R.drawable.ic_error_image)
                .load(Uri.parse(imgsource))
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .into(new CustomTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {

                        int maxWidth = 300;
                        if (resource instanceof GifDrawable) {
                            GifDrawable gifDrawable = (GifDrawable) resource;

                            if (gifDrawable.getIntrinsicWidth() > maxWidth) {
                                float aspectRatio = (float) gifDrawable.getIntrinsicHeight() / (float) gifDrawable.getIntrinsicWidth();
                                gifDrawable.setBounds(0, 0, maxWidth, (int) (aspectRatio * maxWidth));
                            } else {
                                int width=(int) (gifDrawable.getIntrinsicWidth() * 2.5) ;
                                int height=(int) (gifDrawable.getIntrinsicHeight() * 2.5);
                                gifDrawable.setBounds(0, 0, width, height);
                            }

                            //смайлы-гифки запускаем автоматически
                            if (result.isLocalImage) {
                                gifDrawable.setCallback(GlideImageGetter.this);
                                gifDrawable.start();
                            }

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

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        if (null != placeholder) {
                            if (placeholder instanceof GifDrawable)
                                ((GifDrawable) placeholder).stop();
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
        public boolean isGif=false;
        public boolean isLocalImage=false;
        Bitmap play;

        private Bitmap getBitmap(Drawable vectorDrawable) {
            Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(),
                    vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            vectorDrawable.draw(canvas);
            return bitmap;
        }

        @SuppressLint("UseCompatLoadingForDrawables")
        FutureDrawable(Resources res) {
            play = getBitmap(res.getDrawable(R.drawable.ic_play));
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            if(drawable != null) {
                drawable.draw(canvas);
                if (!isLocalImage)
                if (isGif)
                    //если гифка в паузе
                    //рисуем значок поверх изображения
                    if(!((GifDrawable)this.drawable).isRunning()){
                        //int width=play.getWidth();
                        //int height=play.getHeight();

                        //т.к. размер картинки может быть не пропорциональным
                        //берем размер например высоты
                        int width=getBounds().width() / 4;
                        int height=width;

                        if (width==0 | height==0){
                            width=play.getWidth();
                            height=play.getHeight();
                        }

                        play=Bitmap.createScaledBitmap(play,  width, height, false);

                        int x = (getBounds().width() - width) / 2;
                        int y = (getBounds().height() - height) / 2;
                        canvas.drawBitmap(play, x, y, new Paint(Paint.FILTER_BITMAP_FLAG));
                    }
            }
        }

        public void play() {
            if (drawable != null) {
                if (isGif) {
                    GifDrawable gif=((GifDrawable) this.drawable);
                    if (gif.isRunning()) {
                        gif.stop();
                        gif.invalidateSelf();
                        gif.setCallback(null);
                    }else {
                        gif.setCallback(GlideImageGetter.this);
                        gif.start();
                    }

                }
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
