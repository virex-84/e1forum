package com.virex.e1forum;

import android.app.SearchManager;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuItemCompat;
import androidx.lifecycle.Observer;
import androidx.paging.PagedList;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Data;
import androidx.work.WorkInfo;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.virex.e1forum.db.entity.Topic;
import com.virex.e1forum.ui.FooterAdapter;
import com.virex.e1forum.ui.PostDialog;
import com.virex.e1forum.ui.SwipyRefreshLayout.SwipyRefreshLayout;
import com.virex.e1forum.ui.SwipyRefreshLayout.SwipyRefreshLayoutDirection;
import com.virex.e1forum.ui.TopicAdapter;

import java.util.Locale;

import static androidx.work.WorkInfo.State.CANCELLED;
import static androidx.work.WorkInfo.State.ENQUEUED;
import static androidx.work.WorkInfo.State.FAILED;
import static androidx.work.WorkInfo.State.RUNNING;
import static androidx.work.WorkInfo.State.SUCCEEDED;
import static com.virex.e1forum.repository.TopicsWorker.TOPICS_MESSAGE;

/**
 * Фрагмент списка тем
 */
public class TopicFragment extends BaseFragment implements SearchView.OnQueryTextListener {

    private int forum_id=0;

    private int max_pages_of_topics=0;
    private int current_topic_id=0;

    private SwipyRefreshLayout swipeRefreshLayout;
    private TopicAdapter topicAdapter;
    private FooterAdapter footerAdapter;
    private ForumViewModel forumViewModel;
    private FloatingActionButton fab;

    private static final String SEARCH_TEXT="SEARCH_TEXT";
    private String retainedSearchString;
    private SearchView searchView;
    private String filter="";

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

        //создаем меню
        setHasOptionsMenu(true);

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
        topicAdapter.setColors(getResources().getColor(R.color.white),getResources().getColor(R.color.colorPrimary));

        RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
        //recyclerView.setHasFixedSize(true);
        linearLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        footerAdapter = new FooterAdapter(new FooterAdapter.OnItemClickListener() {
            @Override
            public void onReloadClick() {
                forumViewModel.loadTopics(forum_id,0);
            }
        });

        ConcatAdapter concatAdapter=new ConcatAdapter(topicAdapter,footerAdapter);
        recyclerView.setAdapter(concatAdapter);


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
                    current_topic_id=max_pages_of_topics;
                }
                forumViewModel.loadTopics(forum_id,current_topic_id);
            }
        });

        forumViewModel.getAllMessages().observe(this.getViewLifecycleOwner(), new Observer<WorkInfo>() {
            @Override
            public void onChanged(WorkInfo workInfo) {
                if (workInfo==null) return;

                if (workInfo.getState()==RUNNING || workInfo.getState()==ENQUEUED ){
                    footerAdapter.setStatus(FooterAdapter.Status.LOADING,null);
                }

                if (workInfo.getState()==FAILED || workInfo.getState()==CANCELLED) {
                    Data data = workInfo.getOutputData();
                    if (data.getString(TOPICS_MESSAGE) != null) {
                        footerAdapter.setStatus(FooterAdapter.Status.ERROR,workInfo.getOutputData().getString(TOPICS_MESSAGE));
                    }
                }

                if (workInfo.getState()==SUCCEEDED ){
                    footerAdapter.setStatus(FooterAdapter.Status.SUCCESS,null);
                }

                switch(workInfo.getState()){
                    case SUCCEEDED:
                        swipeRefreshLayout.setRefreshing(false);
                        break;
                    case FAILED:
                    case CANCELLED:
                        Toast.makeText(getContext(),workInfo.getOutputData().getString(TOPICS_MESSAGE),Toast.LENGTH_SHORT).show();
                        swipeRefreshLayout.setRefreshing(false);
                        break;
                }
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

        forumViewModel.getTopics(forum_id,filter).observe(this.getViewLifecycleOwner(), new Observer<PagedList<Topic>>() {
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
        //fix на первой странице могут быть приаттаченные темы - их не учитываем т.к. будет больше 40 тем на форум
        forumViewModel.getTopicsCount(forum_id,false).observe(this.getViewLifecycleOwner(), new Observer<Integer>() {
            @Override
            public void onChanged(Integer count) {
                if (count!=null)
                    max_pages_of_topics=count / 40;//на 1 странице умещается 40 тем
            }
        });

        if (savedInstanceState != null) {
            retainedSearchString =savedInstanceState.getString(SEARCH_TEXT, null);
        }

        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
        savePosition(linearLayoutManager,SHARED_OPTIONS);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        if (searchView!=null) {
            outState.putString(SEARCH_TEXT, searchView.getQuery().toString());
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.search, menu);

        //ассоциируем настройку поиска с SearchView
        //!без этой строки иконка поиска будет добавлятся при каждом переключении на фрагмент
        SearchManager searchManager = (SearchManager) mainactivity.getSystemService(Context.SEARCH_SERVICE);

        MenuItem item = menu.findItem(R.id.search);
        searchView = new SearchView((mainactivity).getSupportActionBar().getThemedContext());
        MenuItemCompat.setActionView(item, searchView);
        searchView.setOnQueryTextListener(this);

        //раскрываем меню
        searchView.onActionViewExpanded();
        searchView.setFocusable(true);
        searchView.requestFocusFromTouch();

        if (!TextUtils.isEmpty(retainedSearchString)) {
            item.expandActionView();
            searchView.setQuery(retainedSearchString, true);
            searchView.clearFocus();
        }

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        filter=newText;
        savePosition(linearLayoutManager,SHARED_OPTIONS);
        forumViewModel.setFilteredTopics(forum_id,filter);
        //помечаем фильтр для выделения текста в адаптере
        topicAdapter.markText(filter);
        //принудительная перерисовка recycleview
        topicAdapter.notifyDataSetChanged();
        return false;
    }
}
