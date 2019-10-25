package com.virex.e1forum;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.lifecycle.Observer;
import androidx.paging.PagedList;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.WorkInfo;

import com.virex.e1forum.db.entity.Post;
import com.virex.e1forum.db.entity.Topic;
import com.virex.e1forum.db.entity.User;
import com.virex.e1forum.ui.GlideImageGetter;
import com.virex.e1forum.ui.PostAdapter;
import com.virex.e1forum.ui.SwipyRefreshLayout.SwipyRefreshLayout;
import com.virex.e1forum.ui.SwipyRefreshLayout.SwipyRefreshLayoutDirection;
import com.virex.e1forum.ui.ZoomImageDialog;

import java.util.Locale;

import static com.virex.e1forum.repository.PostsWorker.POSTS_MESSAGE;

/**
 * Фрагмент списка постов
 */
public class PostFragment extends BaseFragment {

    private int forum_id=0;
    private int topic_id=0;

    private int max_pages=0;

    private int current_page_id=0;

    private RecyclerView.LayoutManager linearLayoutManager;
    private String SHARED_OPTIONS;

    private SwipyRefreshLayout swipeRefreshLayout;
    private PostAdapter postAdapter;
    private ForumViewModel forumViewModel;

    private final int ID_LK=1;
    private final int ID_MAIL=2;
    private final int ID_MODERATOR=3;


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
        topic_id = getArguments() != null ? getArguments().getInt(TOPIC_ID) : 0;

        SHARED_OPTIONS=String.format(Locale.ENGLISH,"%s-%d-%d",this.getClass().getSimpleName(),forum_id,topic_id);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.list_layout,container, false);

        postAdapter = new PostAdapter(Post.DIFF_CALLBACK, new PostAdapter.PostListener() {
            @Override
            public void onUserClick(String userNick, final TextView widget) {
                forumViewModel.getUser(userNick).observe(PostFragment.this.getViewLifecycleOwner(), new Observer<User>() {
                    @Override
                    public void onChanged(User user) {
                        if (user==null) return;

                        //всплывающее меню для пользователя
                        PopupMenu popup = new PopupMenu(maincontext, widget);

                        //обработка событий
                        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                switch(item.getItemId()){
                                    case ID_LK:
                                        break;
                                    case ID_MAIL:
                                        break;
                                    case ID_MODERATOR:
                                        break;
                                }
                                return false;
                            }
                        });
                        if (user.actionLK!=null) popup.getMenu().add(0,ID_LK,0,"Личное сообщение");
                        if (user.actionMail!=null) popup.getMenu().add(0,ID_MAIL,0,"Отправить письмо на почту");
                        if (user.id>0) popup.getMenu().add(0,ID_MODERATOR,0,"Сообщить модератору");
                        popup.show();
                    }
                });
            }

            @Override
            public void onLinkClick(String link) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                startActivity(intent);
            }

            @Override
            public void onImageClick(Drawable drawable) {
                if (drawable == null )return;
                if (drawable instanceof GlideImageGetter.FutureDrawable) {
                    //не открываем окно просмотра для локальных смайлов
                    if (((GlideImageGetter.FutureDrawable) drawable).isLocalImage) return;

                    /*
                    Toast toast = Toast.makeText(getContext(), "OPEN", Toast.LENGTH_SHORT);
                    ImageView imageView = new ImageView(getContext());
                    imageView.setImageDrawable(drawable.getCurrent());
                    toast.setView(imageView);
                    toast.show();
                    */
                    ZoomImageDialog zoomImageDialog = new ZoomImageDialog(drawable.getCurrent());
                    zoomImageDialog.show(mainactivity.getSupportFragmentManager(), "zoom_image");
                }
            }
        },getResources().getColor(R.color.colorAccent), getResources());

        RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        linearLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(postAdapter);

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
                    current_page_id=0;
                //если обновляют снизу - увеличиваем счетчик
                } else if (direction==SwipyRefreshLayoutDirection.BOTTOM) {
                    if (max_pages>current_page_id)
                        current_page_id=current_page_id+1;
                }
                forumViewModel.loadPosts(forum_id,topic_id,current_page_id).observe(PostFragment.this.getViewLifecycleOwner(), new Observer<WorkInfo>() {
                    @Override
                    public void onChanged(WorkInfo workInfo) {
                        if (workInfo==null) return;
                        switch(workInfo.getState()){
                            case SUCCEEDED:
                                swipeRefreshLayout.setRefreshing(false);
                                break;
                            case FAILED:
                                Toast.makeText(getContext(),workInfo.getOutputData().getString(POSTS_MESSAGE),Toast.LENGTH_SHORT).show();
                                swipeRefreshLayout.setRefreshing(false);
                                break;
                        }
                    }
                });
            }
        });

        forumViewModel.getPosts(forum_id,topic_id).removeObservers(this);
        forumViewModel.getPosts(forum_id,topic_id).observe(this.getViewLifecycleOwner(), new Observer<PagedList<Post>>() {
            @Override
            public void onChanged(PagedList<Post> posts) {

                if (posts.size()==0)
                    postAdapter.submitList(forumViewModel.emptyPostPagedList());
                else {
                    postAdapter.submitList(posts);
                    restorePosition(linearLayoutManager,SHARED_OPTIONS);
                }
            }
        });

        forumViewModel.getTopicLive(forum_id,topic_id).observe(this.getViewLifecycleOwner(), new Observer<Topic>() {
            @Override
            public void onChanged(Topic topic) {
                max_pages=topic.pagesCount;
                if (topic.isClosed)
                    postAdapter.setIsReadOnly(true);
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
