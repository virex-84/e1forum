package com.virex.e1forum;

import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.SearchView;
import androidx.core.text.HtmlCompat;
import androidx.core.view.MenuItemCompat;
import androidx.lifecycle.Observer;
import androidx.paging.PagedList;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.WorkInfo;

import com.virex.e1forum.common.Utils;
import com.virex.e1forum.db.entity.Post;
import com.virex.e1forum.db.entity.Topic;
import com.virex.e1forum.db.entity.User;
import com.virex.e1forum.network.VoteType;
import com.virex.e1forum.parser.SiteParser;
import com.virex.e1forum.ui.GlideImageGetter;
import com.virex.e1forum.ui.PostAdapter;
import com.virex.e1forum.ui.PostDialog;
import com.virex.e1forum.ui.SwipyRefreshLayout.SwipyRefreshLayout;
import com.virex.e1forum.ui.SwipyRefreshLayout.SwipyRefreshLayoutDirection;
import com.virex.e1forum.ui.ZoomImageDialog;

import java.util.Locale;

import static com.virex.e1forum.repository.PostsWorker.POSTS_MESSAGE;

/**
 * Фрагмент списка постов
 */
public class PostFragment extends BaseFragment  implements SearchView.OnQueryTextListener {

    private int forum_id=0;
    private int topic_id=0;

    private int current_page_id=0;

    private RecyclerView recyclerView;
    private RecyclerView.LayoutManager linearLayoutManager;
    private String SHARED_OPTIONS;

    private SwipyRefreshLayout swipeRefreshLayout;
    private PostAdapter postAdapter;
    private ForumViewModel forumViewModel;


    private static final String SEARCH_TEXT="SEARCH_TEXT";
    private String retainedSearchString;
    private SearchView searchView;
    private String filter="";

    private final int ID_LK=1;
    private final int ID_MAIL=2;
    private final int ID_ABOUT=3;

    private boolean currentTopicIsClosed=false;

    private PostDialog postDialog;
    private String title;


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
        topic_id = getArguments() != null ? getArguments().getInt(TOPIC_ID) : 0;

        SHARED_OPTIONS=String.format(Locale.ENGLISH,"%s-%d-%d",this.getClass().getSimpleName(),forum_id,topic_id);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.list_layout,container, false);

        postAdapter = new PostAdapter(Post.DIFF_CALLBACK, new PostAdapter.PostListener() {
            @Override
            public void onUserClick(final String userNick, final TextView widget) {
                //observeOnce - подписываемся один раз на событие и сразу отписываемся
                //т.к. после обновления страницы onChanged сработает заново и мы получим кучу диалоговых окон
                Utils.observeOnce(forumViewModel.getUser(userNick), PostFragment.this.getViewLifecycleOwner(), new Observer<User>() {
                    @Override
                    public void onChanged(final User user) {
                        if (user==null) return;

                        //всплывающее меню для пользователя
                        PopupMenu popup = new PopupMenu(maincontext, widget);

                        //обработка событий
                        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                switch(item.getItemId()){
                                    case ID_ABOUT:
                                        aboutUser(user.user_id);
                                        break;
                                    case ID_LK:
                                        sendLK(user.actionLK);
                                        break;
                                    case ID_MAIL:
                                        break;
                                }
                                return false;
                            }
                        });
                        if (user.actionLK!=null) popup.getMenu().add(0,ID_LK,0,getString(R.string.send_private));
                        if (user.actionMail!=null) popup.getMenu().add(0,ID_MAIL,0,getString(R.string.send_to_mail));
                        if (user.user_id>0) popup.getMenu().add(0,ID_ABOUT,0,getString(R.string.about_user));
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

                    ZoomImageDialog zoomImageDialog = new ZoomImageDialog(drawable.getCurrent());
                    zoomImageDialog.show(mainactivity.getSupportFragmentManager(), "zoom_image");
                }
            }

            @Override
            public void onVoteClick(final Post post, VoteType voteType) {
                //сохраняем позицию перед плюсометом
                savePosition(linearLayoutManager,SHARED_OPTIONS);
                forumViewModel.votePost(post, voteType, new ForumViewModel.NetworkListener() {
                    @Override
                    public void onSuccess(String message) {
                        postAdapter.notifyItemChanged(postAdapter.getCurrentList().indexOf(post));
                    }

                    @Override
                    public void onError(String message) {

                    }
                });
            }

            @Override
            public void onReplyClick(Post post) {
                sendPost(post,false);
            }

            @Override
            public void onQuoteClick(Post post) {
                sendPost(post,true);
            }

            @Override
            public void onModeratorClick(final Post post) {
                AlertDialog.Builder dialog = new AlertDialog.Builder(maincontext);
                dialog.setCancelable(true);
                dialog.setMessage(getString(R.string.send_to_moderator));
                dialog.setPositiveButton(getString(R.string.Ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        forumViewModel.sendModerator(post.forum_id, post.id, new ForumViewModel.NetworkListener() {
                            @Override
                            public void onSuccess(String message) {
                                Toast.makeText(maincontext,R.string.send_moderator_success,Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onError(String message) {
                                Toast.makeText(maincontext,message,Toast.LENGTH_SHORT).show();
                            }
                        });
                        dialog.dismiss();
                    }
                }).setNegativeButton(getString(R.string.Cancel),null).show();
            }

        },getResources());
        postAdapter.setColors(getResources().getColor(R.color.colorAccent),getResources().getColor(R.color.white),getResources().getColor(R.color.colorPrimary));

        recyclerView = view.findViewById(R.id.recyclerView);
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
                    int current_page_position=recyclerView.getAdapter().getItemCount();//((LinearLayoutManager)linearLayoutManager).findLastVisibleItemPosition();
                    current_page_id=current_page_position / 25;
                }
                /*
                  логика обновления проста:
                  если постов например 5, то при делении на 25 = 0, поэтому будем обновлять текущую (нулевую) страницу
                  если постов например 30, то 30/25=1,2 (остаток 2 отбрасывается), поэтому будем обновлять вторую страницу (номер 1, т.к. нумерация с нуля)
                */
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

        forumViewModel.getPosts(forum_id,topic_id,filter).observe(this.getViewLifecycleOwner(), new Observer<PagedList<Post>>() {
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
                if (topic.isClosed) {
                    currentTopicIsClosed=true;
                    postAdapter.setIsReadOnly(true);
                }
                title="Re: ".concat(topic.title);
            }
        });


        forumViewModel.isLogin().observe(this.getViewLifecycleOwner(),  new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean isLogin) {
                //если топик не закрыт, но мы не залогинены - делаем его только для чтения
                if (!currentTopicIsClosed)
                    postAdapter.setIsReadOnly(!isLogin);
            }
        });


        if (savedInstanceState != null) {
            retainedSearchString =savedInstanceState.getString(SEARCH_TEXT, null);
        }

        return view;
    }

    private void sendPost(final Post post, boolean isQuote){
        //сохраняем позицию
        savePosition(linearLayoutManager,SHARED_OPTIONS);

        postDialog = new PostDialog(title, new PostDialog.OnDialogClickListener() {
            @Override
            public void onOkClick(String subject, String body) {
                body= SiteParser.convertBBCodeToE1(body);

                subject=SiteParser.URLEncodeString(subject);
                body=SiteParser.URLEncodeString(body);

                postDialog.setStartLoading();
                forumViewModel.sendPost(post.forum_id, post.topic_id, post.id, subject, body, new ForumViewModel.NetworkListener() {
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
        if (isQuote)
            postDialog.setQuote(post.user, post.text);
        postDialog.show(mainactivity.getSupportFragmentManager(),"post");
    }

    private void sendLK(final String actionLK){
        postDialog = new PostDialog(title, new PostDialog.OnDialogClickListener() {
            @Override
            public void onOkClick(String subject, String body) {
                body= SiteParser.convertBBCodeToE1(body);

                subject=SiteParser.URLEncodeString(subject);
                body=SiteParser.URLEncodeString(body);

                postDialog.setStartLoading();

                forumViewModel.sendLK(SiteParser.extractQuotedString(actionLK), subject, body, new ForumViewModel.NetworkListener() {
                    @Override
                    public void onSuccess(String message) {
                        Toast.makeText(maincontext,getString(R.string.send_private_success),Toast.LENGTH_SHORT).show();
                        postDialog.dismiss();
                    }

                    @Override
                    public void onError(String message) {
                        postDialog.setError(message);
                        postDialog.setFinishLoading();
                    }
                });
            }
        });
        postDialog.show(mainactivity.getSupportFragmentManager(),"sendLK");
    }

    private void aboutUser(final int user_id){
        forumViewModel.aboutUser(user_id, new ForumViewModel.NetworkListener() {
            @Override
            public void onSuccess(String message) {
                AlertDialog.Builder dialog = new AlertDialog.Builder(maincontext);
                dialog.setCancelable(true);
                dialog.setMessage(HtmlCompat.fromHtml(message,HtmlCompat.FROM_HTML_MODE_COMPACT));
                dialog.setPositiveButton(getString(R.string.Ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                }).show();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(maincontext,message,Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        savePosition(linearLayoutManager,SHARED_OPTIONS);
    }

    public void setIsReadOnly(Boolean value){
        //если топик не закрыт, но мы не залогинены - делаем его только для чтения
        if (!currentTopicIsClosed)
            postAdapter.setIsReadOnly(value);
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
        forumViewModel.setFilteredPosts(forum_id,topic_id,filter);
        //помечаем фильтр для выделения текста в адаптере
        postAdapter.markText(filter);
        //принудительная перерисовка recycleview
        postAdapter.notifyDataSetChanged();
        return false;
    }
}
