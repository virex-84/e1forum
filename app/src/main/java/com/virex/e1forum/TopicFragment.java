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
import androidx.paging.PagedList;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.WorkInfo;

import com.virex.e1forum.db.entity.Topic;
import com.virex.e1forum.ui.SwipyRefreshLayout.SwipyRefreshLayout;
import com.virex.e1forum.ui.SwipyRefreshLayout.SwipyRefreshLayoutDirection;
import com.virex.e1forum.ui.TopicAdapter;

import java.util.Locale;

import static com.virex.e1forum.repository.TopicsWorker.TOPICS_MESSAGE;

/**
 * Фрагмент списка тем
 */
public class TopicFragment extends BaseFragment {

    private int forum_id=0;

    private SwipyRefreshLayout swipeRefreshLayout;
    private TopicAdapter topicAdapter;
    private ForumViewModel forumViewModel;

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
        forum_id = getArguments() != null ? getArguments().getInt(FORUM_ID) : 0;

        SHARED_OPTIONS=String.format(Locale.ENGLISH,"%s-%d",this.getClass().getSimpleName(),forum_id);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.list_layout,container, false);

        topicAdapter = new TopicAdapter(Topic.DIFF_CALLBACK, new TopicAdapter.TopicListener() {
            @Override
            public void onClick(Topic topic) {
                Bundle bundle = new Bundle();
                bundle.putString(TopicFragment.TITLE,topic.title);
                bundle.putInt(PostFragment.FORUM_ID,topic.forum_id);
                bundle.putInt(PostFragment.TOPIC_ID,topic.id);
                mainactivity.goFragment(PostFragment.class,bundle);
            }

            @Override
            public void onBookMark(Topic topic) {
                savePosition(linearLayoutManager,SHARED_OPTIONS);
                forumViewModel.checkTopicBookmark(topic);
            }
        });

        RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        linearLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(topicAdapter);

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
                clearPosition(SHARED_OPTIONS);
                forumViewModel.loadTopics(forum_id).observe(TopicFragment.this, new Observer<WorkInfo>() {
                    @Override
                    public void onChanged(WorkInfo workInfo) {
                        if (workInfo==null) return;
                        switch(workInfo.getState()){
                            case SUCCEEDED:
                                swipeRefreshLayout.setRefreshing(false);
                                break;
                            case FAILED:
                                Toast.makeText(getContext(),workInfo.getOutputData().getString(TOPICS_MESSAGE),Toast.LENGTH_SHORT).show();
                                swipeRefreshLayout.setRefreshing(false);
                                break;
                        }
                    }
                });
            }
        });

        forumViewModel.getTopics(forum_id).removeObservers(this);
        forumViewModel.getTopics(forum_id).observe(this, new Observer<PagedList<Topic>>() {
            @Override
            public void onChanged(PagedList<Topic> topics) {

                if (topics.size()==0)
                    topicAdapter.submitList(forumViewModel.emptyTopicPagedList());
                else {
                    topicAdapter.submitList(topics);
                    restorePosition(linearLayoutManager,SHARED_OPTIONS);
                }
            }
        });

        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
        savePosition(linearLayoutManager,SHARED_OPTIONS);
    }
}
