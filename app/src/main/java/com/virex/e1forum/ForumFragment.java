package com.virex.e1forum;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.WorkInfo;

import com.virex.e1forum.db.entity.Forum;
import com.virex.e1forum.ui.ForumAdapter;
import com.virex.e1forum.ui.SwipyRefreshLayout.SwipyRefreshLayout;
import com.virex.e1forum.ui.SwipyRefreshLayout.SwipyRefreshLayoutDirection;

import java.util.List;
import java.util.Locale;

import static com.virex.e1forum.repository.ForumsWorker.FORUMS_MESSAGE;

/**
 * Фрагмент списка форумов
 */
public class ForumFragment extends BaseFragment {

    private int forum_id=0;

    private ForumAdapter forumAdapter;
    private ForumViewModel forumViewModel;
    private SwipyRefreshLayout swipeRefreshLayout;

    private RecyclerView.LayoutManager linearLayoutManager;
    private String SHARED_OPTIONS;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.menuID=R.id.nav_forums;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        forumViewModel = getDefaultViewModelProviderFactory().create(ForumViewModel.class);

        SHARED_OPTIONS=String.format(Locale.ENGLISH,"%s-%d",this.getClass().getSimpleName(),forum_id);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.list_layout,container, false);

        forumAdapter = new ForumAdapter(new ForumAdapter.ForumListener() {
            @Override
            public void onClick(Forum forum) {
                savePosition(linearLayoutManager,SHARED_OPTIONS);

                Bundle bundle = new Bundle();
                bundle.putString(TopicFragment.TITLE,forum.title);
                bundle.putInt(TopicFragment.FORUM_ID,forum.id);
                mainactivity.goFragment(TopicFragment.class,bundle);
            }

            @Override
            public void onBookMark(Forum forum) {
                savePosition(linearLayoutManager,SHARED_OPTIONS);
                forumViewModel.checkForumBookmark(forum);
            }
        });

        RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        linearLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(forumAdapter);

        swipeRefreshLayout =  view.findViewById(R.id.swipeRefreshLayout);
        //цвета анимации загрузки
        swipeRefreshLayout.setColorSchemeColors(
                getResources().getColor(android.R.color.holo_blue_bright),
                getResources().getColor(android.R.color.holo_green_light),
                getResources().getColor(android.R.color.holo_orange_light),
                getResources().getColor(android.R.color.holo_red_light)
        );
        swipeRefreshLayout.setOnRefreshListener(new SwipyRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh(SwipyRefreshLayoutDirection direction) {
                forumViewModel.loadForums().observe(ForumFragment.this.getViewLifecycleOwner(), new Observer<WorkInfo>() {
                    @Override
                    public void onChanged(WorkInfo workInfo) {
                        if (workInfo==null) return;
                        switch(workInfo.getState()){
                            case SUCCEEDED:
                                swipeRefreshLayout.setRefreshing(false);
                                break;
                            case FAILED:
                                Toast.makeText(getContext(),workInfo.getOutputData().getString(FORUMS_MESSAGE),Toast.LENGTH_SHORT).show();
                                swipeRefreshLayout.setRefreshing(false);
                                break;
                        }
                    }
                });
            }
        });

        forumViewModel.getForums().observe(this.getViewLifecycleOwner(), new Observer<List<Forum>>() {
            @Override
            public void onChanged(List<Forum> forums) {

                if (forums.size()==0)
                    forumViewModel.loadForums();
                else {
                    forumAdapter.submitList(forums);
                    restorePosition(linearLayoutManager,SHARED_OPTIONS);
                }
            }
        });

        return view;
    }
}
