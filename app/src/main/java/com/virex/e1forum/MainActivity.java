package com.virex.e1forum;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.view.GravityCompat;
import androidx.appcompat.app.ActionBarDrawerToggle;

import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.google.android.material.navigation.NavigationView;
import com.virex.e1forum.common.Utils;
import com.virex.e1forum.ui.BadgeDrawerArrowDrawable;
import com.virex.e1forum.ui.LoginDialog;

import androidx.core.view.MenuItemCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    static final String CURRENT_FRAGMENT="CURRENT_FRAGMENT";

    SharedPreferences options;
    NavigationView navigationView;
    SwitchCompat switcher;

    private boolean isDrawerFixed=false;
    private BadgeDrawerArrowDrawable badgeDrawerArrowDrawable;

    private ForumViewModel forumViewModel;
    private LoginDialog loginDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme_NoActionBar);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        options= PreferenceManager.getDefaultSharedPreferences(this);

        forumViewModel = getDefaultViewModelProviderFactory().create(ForumViewModel.class);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
            @Override
            public void onDrawerOpened(View drawerView) {
            }
        };
        if (!isDrawerFixed) {
            badgeDrawerArrowDrawable = new BadgeDrawerArrowDrawable(getSupportActionBar().getThemedContext());
            toggle.setDrawerArrowDrawable(badgeDrawerArrowDrawable);
        }
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        //---ночная тема
        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        Menu menu = navigationView.getMenu();
        MenuItem menuItem = menu.findItem(R.id.nav_dark_mode);
        View actionView = MenuItemCompat.getActionView(menuItem);

        switcher = actionView.findViewById(R.id.drawer_switch);
        boolean is_dark_theme= Utils.isDarkTheme(options);
        switcher.setChecked(is_dark_theme);
        switcher.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                Utils.changeTheme(options);
            }
        });
        //---

        //восстанавливаем ранее открытый фрагмент
        if (savedInstanceState != null) {
            //не восстанавливаем фрагмент
            //Fragment currentFragment = getSupportFragmentManager().getFragment(savedInstanceState, CURRENT_FRAGMENT);
            //goFragment(currentFragment,null);
        } else {
            //либо стартовая страница - каталог
            onMenuClick(R.id.nav_forums);
        }

        options= PreferenceManager.getDefaultSharedPreferences(this);

        forumViewModel.isLogin().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean isLogin) {
                if (isLogin) {
                    getSupportActionBar().setDisplayShowHomeEnabled(true);
                    getSupportActionBar().setIcon(R.drawable.online);

                    onMenuEnabled(R.id.nav_login,false);
                    onMenuEnabled(R.id.nav_logout,true);
                } else {
                    getSupportActionBar().setDisplayShowHomeEnabled(true);
                    getSupportActionBar().setIcon(R.drawable.offline);

                    onMenuEnabled(R.id.nav_login,true);
                    onMenuEnabled(R.id.nav_logout,false);
                }


                Fragment currentFragment=getSupportFragmentManager().findFragmentById(R.id.content);
                if (currentFragment==null) return;

                if (currentFragment instanceof PostFragment){
                    ((PostFragment) currentFragment).setIsReadOnly(!isLogin);
                }
            }
        });
    }

    public void onMenuClick(int id){
        try {
            MenuItem menuItem = navigationView.getMenu().findItem(id);
            onNavigationItemSelected(menuItem);
        } catch (Exception ignore){
        }
    }

    public void onMenuEnabled(int id, boolean value){
        try {
            MenuItem menuItem = navigationView.getMenu().findItem(id);
            menuItem.setEnabled(value);
        } catch (Exception ignore){
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        //сохраняем текущий фрагмент
        try {
            Fragment fragment = getSupportFragmentManager().getFragments().get(getSupportFragmentManager().getFragments().size()-1);
            getSupportFragmentManager().putFragment(outState, CURRENT_FRAGMENT, fragment);
        } catch(Exception e){
            e.printStackTrace();
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            if (!isDrawerFixed) {
                drawer.closeDrawer(GravityCompat.START);
            }
        } else {
            //возвращаемся по стеку назад если есть в кеше
            //==1 если включен стек
            if (getSupportFragmentManager().getBackStackEntryCount()>1){
                //возврат по стеку
                getSupportFragmentManager().popBackStack();
            } else
                finishAffinity();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();


        Fragment fragment = null;
        Bundle bundle=new Bundle();

        if (id == R.id.nav_forums) {

            bundle.putString(ForumFragment.TITLE,item.getTitle().toString());
            fragment = new ForumFragment();

        } else if (id == R.id.nav_login) {
            loginDialog = new LoginDialog(new LoginDialog.OnDialogClickListener() {
                @Override
                public void onOkClick(String login, String password) {
                    loginDialog.setStartLoading();
                    forumViewModel.loginSite(login, password, new ForumViewModel.NetworkListener() {
                        @Override
                        public void onSuccess(String message) {
                            Toast.makeText(MainActivity.this,message,Toast.LENGTH_SHORT).show();
                            loginDialog.dismiss();
                        }

                        @Override
                        public void onError(String message) {
                            //Toast.makeText(MainActivity.this,message,Toast.LENGTH_SHORT).show();
                            loginDialog.setError(message);
                            loginDialog.setFinishLoading();
                        }
                    });
                }
            });
            loginDialog.show(getSupportFragmentManager(),"login");

        } else if (id == R.id.nav_logout) {

            AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
            alertDialog
                    .setCancelable(true)
                    .setMessage(getString(R.string.logout_warning))
                    .setPositiveButton(R.string.Cancel, null)
                    .setNegativeButton(R.string.Ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    forumViewModel.logOut();
                }
            });
            alertDialog.show();

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (!isDrawerFixed) {
            drawer.closeDrawer(GravityCompat.START);
        }

        if (goFragment(fragment, bundle)){

            //если был переход на фрагмент - помечаем меню (для вызова из onMenuClick)
            item.setCheckable(true);
            item.setChecked(true);
        } else
            return false;

        return true;
    }

    //метод для перехода фрагмент1->фрагмент2
    public void goFragment(Class clazz, Bundle bundle){
        try {
            String className = clazz.getName();
            ClassLoader classLoader = clazz.getClassLoader();
            Fragment fragment = getSupportFragmentManager().getFragmentFactory().instantiate(classLoader, className);
            goFragment(fragment, bundle);
        }catch(Exception ignore){
        }
    }

    //переход на фрагмент
    private boolean goFragment(Fragment fragment, Bundle bundle){
        if (fragment==null) return false;
        if (bundle!=null) fragment.setArguments(bundle);

        try {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.content, fragment);
            ft.addToBackStack("asd");
            ft.commit();

            //если переход удачный - отмечаем меню
            if (fragment instanceof BaseFragment)
                checkMenuItem(((BaseFragment) fragment).menuID);

            return true;
        } catch(Exception e){
            return false;
        }
    }

    void checkMenuItem(int id) {
        if (navigationView.getMenu().findItem(id)!=null) {
            //ищем пунк меню
            MenuItem menuItem=navigationView.getMenu().findItem(id);

            //заголовок из меню
            String title=String.format("%s: %s",getString(R.string.app_name), menuItem.getTitle());

            //помечаем меню
            menuItem.setChecked(true);

            //устанавливаем заголовок
            getSupportActionBar().setTitle(title);
        }
    }

    //отображаем на бургере значок для новых пользователей
    private void setMenuNotify(String text){
        if (!isDrawerFixed && badgeDrawerArrowDrawable!=null){
            badgeDrawerArrowDrawable.setText(text);
            badgeDrawerArrowDrawable.showBadge();
        }
    }

    //скрываем значок навсегда
    private void hideMenuNotify(){
        if (!isDrawerFixed && badgeDrawerArrowDrawable!=null) {
            if (badgeDrawerArrowDrawable.isBadgeEnabled()) {
                badgeDrawerArrowDrawable.hideBadge();
            }
        }
    }


}
