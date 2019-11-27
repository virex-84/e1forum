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

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.virex.e1forum.db.entity.Topic;
import com.virex.e1forum.ui.PostDialog;
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

    private int max_pages_of_topics=0;
    private int current_topic_id=0;

    private SwipyRefreshLayout swipeRefreshLayout;
    private TopicAdapter topicAdapter;
    private ForumViewModel forumViewModel;
    private FloatingActionButton fab;

    private PostDialog postDialog;

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
        View view = inflater.inflate(R.layout.topics_layout,container, false);

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
                //если обновляют сверху - обновляем только первую страницу
                if (direction==SwipyRefreshLayoutDirection.TOP){
                    current_topic_id=0;
                    //если обновляют снизу - увеличиваем счетчик
                } else if (direction==SwipyRefreshLayoutDirection.BOTTOM) {
                    current_topic_id=max_pages_of_topics+1;
                }
                forumViewModel.loadTopics(forum_id,current_topic_id).observe(TopicFragment.this.getViewLifecycleOwner(), new Observer<WorkInfo>() {
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

        fab = view.findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                postDialog = new PostDialog(null, new PostDialog.OnDialogClickListener() {
                    @Override
                    public void onOkClick(String subject, String body) {
                        postDialog.setStartLoading();
                        //t и p должны быть 0
                        forumViewModel.sendPost(forum_id,0,0,  subject, body, new ForumViewModel.NetworkListener() {
                            @Override
                            public void onSuccess(String message) {
                                postDialog.dismiss();
                            }

                            @Override
                            public void onError(String message) {
                                //Toast.makeText(MainActivity.this,message,Toast.LENGTH_SHORT).show();
                                postDialog.setError(message);
                                postDialog.setFinishLoading();
                            }
                        });
                    }
                });
                postDialog.show(mainactivity.getSupportFragmentManager(),"post");
            }
        });

        forumViewModel.getTopics(forum_id).observe(this.getViewLifecycleOwner(), new Observer<PagedList<Topic>>() {
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
        forumViewModel.getTopicsCount(forum_id).observe(this.getViewLifecycleOwner(), new Observer<Integer>() {
            @Override
            public void onChanged(Integer count) {
                if (count!=null)
                    max_pages_of_topics=count / 40;//на 1 странице умещается 40 тем
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
