package com.virex.e1forum;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;

import static android.content.Context.MODE_PRIVATE;


/**
 * Базовый фрагмент с базовым функционалом
 */
public class BaseFragment extends Fragment implements LifecycleOwner {

    String SHARED_RECYCLER_POSITION = "SHARED_RECYCLER_POSITION";

    static final String TITLE="TITLE";
    static final String FORUM_ID="FORUM_ID";
    static final String TOPIC_ID="TOPIC_ID";

    //!необходимо переопределять в onAttach в предках
    int menuID=0;
    String title="";

    Context maincontext;
    MainActivity mainactivity;
    Fragment thisFragment;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //сохраняем состояние при перевороте экрана
        setRetainInstance(true);

        thisFragment = this;

        title = getArguments() != null ? getArguments().getString(TITLE) : "";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        //обязательно переназначаем новый активити (при создании и повороте экрана)
        maincontext=context;
        mainactivity= (MainActivity) getActivity();
    }

    @Override
    public void onResume() {
        super.onResume();
        mainactivity.getSupportActionBar().setTitle(String.valueOf(title));
    }

    void savePosition(RecyclerView.LayoutManager linearLayoutManager, String SHARED_OPTIONS) {
        super.onPause();
        String pos=new Gson().toJson(linearLayoutManager.onSaveInstanceState());
        Log.e("ttt_SAVE",pos);
        SharedPreferences settings = mainactivity.getSharedPreferences(SHARED_OPTIONS, MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(SHARED_RECYCLER_POSITION, pos);
        editor.apply();
    }

    void restorePosition(RecyclerView.LayoutManager linearLayoutManager, String SHARED_OPTIONS) {
        SharedPreferences settings = mainactivity.getSharedPreferences(SHARED_OPTIONS, MODE_PRIVATE);
        String pos = settings.getString(SHARED_RECYCLER_POSITION, "");

        /*
        //{"mAnchorLayoutFromEnd":false,"mAnchorOffset":0,"mAnchorPosition":0}
        if (pos!=null) {
            JsonElement jelement = new JsonParser().parse(pos);
            JsonObject jobject = jelement.getAsJsonObject();
            int posx = jobject.get("mAnchorPosition").getAsInt();
            int offset = jobject.get("mAnchorOffset").getAsInt();

            //сначала переходим в приблизительную позицию (быстрое перемещение)
            ((LinearLayoutManager)linearLayoutManager).scrollToPositionWithOffset(posx,offset);
        }
         */

        Log.e("ttt_LOAD",pos);
        //а потом точное позиционирование
        LinearLayoutManager.SavedState position =new Gson().fromJson(pos, LinearLayoutManager.SavedState.class);
        linearLayoutManager.onRestoreInstanceState(position);
    }

    void clearPosition(String SHARED_OPTIONS){
        SharedPreferences settings = mainactivity.getSharedPreferences(SHARED_OPTIONS, MODE_PRIVATE);
        settings.edit().remove(SHARED_RECYCLER_POSITION).apply();
    }

}
