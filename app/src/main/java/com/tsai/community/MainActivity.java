package com.tsai.community;


import android.app.Activity;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.design.widget.NavigationView;

import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import MyMethod.PersistentCookieStore;
import MyMethod.SysApplication;
import ViewModel.GroupChatView;
import ViewModel.IdentityData;
import microsoft.aspnet.signalr.client.SignalRFuture;
import microsoft.aspnet.signalr.client.hubs.HubConnection;
import microsoft.aspnet.signalr.client.hubs.HubProxy;
import microsoft.aspnet.signalr.client.hubs.SubscriptionHandler2;
import microsoft.aspnet.signalr.client.transport.ServerSentEventsTransport;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private HubConnection connection;
    private static final String HUB_URL = "http://163.17.136.228/ConGroup/signalr";
    private static final String HUB_NAME = "chatHub_All";
    private SignalRFuture<Void> mSignalRFuture;
    private HubProxy mHub;

    private OkHttpClient client;
    public IdentityData identityData;

    private HomeFragment homeFragment;
    private GalleryFragment galleryFragment;

    //避免重複Toast
    private Toast toast = null;
    private PostListFragment dataListFragment;
    private ChatFragment chatFragment;
    private NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //關閉背景的連線
        Intent intent = new Intent(this, ChatService.class);
        stopService(intent);

        //加入一次關閉所有Activity的集合
        SysApplication.getInstance().addActivity(this);

        //導入頁面中的內容
        initView();
        SetCookies();
        //CheckLogon();改為從DataListFragment的OnCreate執行
    }

    private void initView() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setLogo(R.mipmap.ic_launcher);  //左上方logo圖

        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);  //取消Toolbar的內建靠左title(像Actionbar的特性)

        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.flContent, new HomeFragment(), "HomeFragment").commit();
        navigationView.setCheckedItem(R.id.nav_home);
    }

    public void SetCookies() {
        client = new OkHttpClient().newBuilder()
                .cookieJar(new CookieJar() {
                    private final PersistentCookieStore cookieStore = new PersistentCookieStore(MainActivity.this);

                    @Override
                    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                        if (cookies != null && cookies.size() > 0) {
                            for (Cookie item : cookies) {
                                cookieStore.add(url, item);
                            }
                        }
                    }

                    @Override
                    public List<Cookie> loadForRequest(HttpUrl url) {
                        List<Cookie> cookies = cookieStore.get(url);
                        return cookies;
                    }
                })
                .build();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (getSupportFragmentManager().findFragmentByTag("GalleryFragment") != null) {
            menu.findItem(R.id.HomeDescription).setVisible(false);
            menu.findItem(R.id.About).setVisible(false);
            menu.findItem(R.id.GalleryDescription).setVisible(true);
        } else if (getSupportFragmentManager().findFragmentByTag("HomeFragment") != null) {
            menu.findItem(R.id.HomeDescription).setVisible(true);
            menu.findItem(R.id.About).setVisible(true);
            menu.findItem(R.id.GalleryDescription).setVisible(false);
        } else if (getSupportFragmentManager().findFragmentByTag("LoginFragment") != null) {
            menu.findItem(R.id.HomeDescription).setVisible(false);
            menu.findItem(R.id.About).setVisible(true);
            menu.findItem(R.id.GalleryDescription).setVisible(false);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.HomeDescription:
                new AlertDialog.Builder(this)
                        .setTitle("說明")
                        .setMessage("動態分頁可下拉更新哦")
                        .setPositiveButton("水啦", null)
                        .show();
                break;
            case R.id.About:
                new AlertDialog.Builder(this)
                        .setTitle("關於")
                        .setMessage("這是一個練習用的社群網站\n製作者 : Tsai\n版本 : 2.0\n持續更新中~")
                        .setPositiveButton("哦哦", null)
                        .show();
                break;
            case R.id.GalleryDescription:
                new AlertDialog.Builder(this)
                        .setTitle("說明")
                        .setMessage("這是你個人的雲端圖庫\n長按圖片可刪除\n快上傳一些圖片吧!!")
                        .setPositiveButton("蒸蚌", null)
                        .show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_home) {

            homeFragment = (HomeFragment) getSupportFragmentManager().findFragmentByTag("HomeFragment");
            //避免崩潰
            if (homeFragment == null) {
                FragmentManager fragmentManager = getSupportFragmentManager();
                fragmentManager.beginTransaction().replace(R.id.flContent, new HomeFragment(), "HomeFragment").commit();
                //動態更換Menu
                invalidateOptionsMenu();
            }

        } else if (id == R.id.nav_gallery) {
            galleryFragment = (GalleryFragment) getSupportFragmentManager().findFragmentByTag("GalleryFragment");
            //避免崩潰
            if (galleryFragment == null) {
                FragmentManager fragmentManager = getSupportFragmentManager();
                fragmentManager.beginTransaction().replace(R.id.flContent, new GalleryFragment(), "GalleryFragment").commit();
                //動態更換Menu
                invalidateOptionsMenu();
            }

        } else if (id == R.id.nav_groups) {

        } else if (id == R.id.nav_logout) {
            Logout();
        } else if (id == R.id.nav_leave) {
            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            drawer.closeDrawer(GravityCompat.START);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    finish();
                }
            }, 400);
            return true;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onDestroy() {
        //關閉連線
        if (mSignalRFuture != null) {
            mSignalRFuture.cancel();
            Intent intent = new Intent(this, ChatService.class);
            startService(intent);
        }
        super.onDestroy();
    }

    public boolean CheckNetWork() {
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connManager.getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isConnected()) {
            return false;
        }
        return true;
    }

    public void CheckLogon() {
        SetCookies();
        new CheckLogon().execute();
    }

    class CheckLogon extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                Request request = new Request.Builder()
                        .url(getString(R.string.server_url) + "Api/MemberApi/GetIdentity")
                        .build();
                Response response = client.newCall(request).execute();
                int StatusCode = response.code();
                if (StatusCode == 200) {
                    String ResMsg = response.body().string();
                    Gson gson = new Gson();
                    identityData = gson.fromJson(ResMsg, IdentityData.class);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            //設置homeFragment
            homeFragment = (HomeFragment) getSupportFragmentManager().findFragmentByTag("HomeFragment");
            //設置dataListFragment
            dataListFragment = homeFragment.getDataListFragment();
            //設置ChatFragment
            chatFragment = homeFragment.getChatFragment();

            //關閉轉圈
            dataListFragment.mSwipeLayout.setRefreshing(false);

            //檢查網路連線
            if (!CheckNetWork()) {
                showTextToast("請檢察網路連線");
                return;
            }
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction().replace(R.id.flContent, new LoginFragment(), "LoginFragment").commit();
            //動態更換Menu
            invalidateOptionsMenu();
            /*if (identityData != null) {
                if (identityData.UserName.equals("")) {
                    FragmentManager fragmentManager = getSupportFragmentManager();
                    fragmentManager.beginTransaction().replace(R.id.flContent, new LoginFragment(), "LoginFragment").commit();
                    //動態更換Menu
                    invalidateOptionsMenu();
                } else {
                    UseSignalR();
                    TextView tv_UserName = (TextView) findViewById(R.id.tv_UserName);
                    tv_UserName.setText(identityData.UserName);
                    dataListFragment.Refresh();
                    chatFragment.Refresh();

                    //只有首次登入的時候Hello
                    if (isFirstInMain) {
                        HelloToAll();
                        isFirstInMain = false;
                    }
                }

            } else {
                showTextToast("請檢察網路連線");
            }*/
        }
    }

    private void UseSignalR() {
        if (connection == null) {
            connection = new HubConnection(HUB_URL);
            mHub = connection.createHubProxy(HUB_NAME);
            mSignalRFuture = connection.start(new ServerSentEventsTransport(connection.getLogger()));
            //可以理解為訊息or事件監聽器
            mHub.on("addMessage", new SubscriptionHandler2<GroupChatView.ChatData, String>() {
                @Override
                public void run(GroupChatView.ChatData chatData, String CTime) {
                    //使用AsyncTask來更新畫面
                    new AsyncTask<GroupChatView.ChatData, Void, GroupChatView.ChatData>() {
                        @Override
                        protected GroupChatView.ChatData doInBackground(GroupChatView.ChatData... param) {
                            GroupChatView.ChatData chatData = param[0];
                            return chatData;
                        }

                        @Override
                        protected void onPostExecute(GroupChatView.ChatData chatData) {
                            chatFragment.addMessage(chatData);
                            super.onPostExecute(chatData);
                        }
                    }.execute(chatData);
                }


            }, GroupChatView.ChatData.class, String.class);

            mHub.on("addNewMember", new SubscriptionHandler2<String, Boolean>() {
                @Override
                public void run(String name, final Boolean isAdmin) {
                    new AsyncTask<String, Void, String>() {
                        @Override
                        protected String doInBackground(String... param) {
                            return param[0];
                        }

                        @Override
                        protected void onPostExecute(String UserName) {
                            if (isAdmin) {
                                showTextToast("歡迎管理員 : " + UserName);
                            } else {
                                showTextToast("歡迎 : " + UserName);
                            }

                        }
                    }.execute(name);

                }
            }, String.class, Boolean.class);

            //開啟連線
            try {
                mSignalRFuture.get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    private void HelloToAll() {
        Request request = new Request.Builder()
                .url(getString(R.string.server_url) + "Api/ChatApi/HelloToAll?connectionId=" + connection.getConnectionId())
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showTextToast("請檢察網路連線");
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
            }
        });
    }

    public void Login() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.flContent, new HomeFragment(), "HomeFragment").commit();
        navigationView.setCheckedItem(R.id.nav_home);
        //動態更換Menu
        invalidateOptionsMenu();
    }

    private void Logout() {

        Request request = new Request.Builder()
                .url(getString(R.string.server_url) + "Api/MemberApi/Logout")
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showTextToast("請檢察網路連線");
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final int StatusCode = response.code();
                if (StatusCode == 200) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            FragmentManager fragmentManager = getSupportFragmentManager();
                            fragmentManager.beginTransaction().replace(R.id.flContent, new LoginFragment(), "LoginFragment").commit();
                        }
                    });

                }
            }

        });
    }

    //關閉鍵盤
    public void hide_keyboard(Activity activity) {
        InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        View view = activity.getCurrentFocus();
        if (view == null) {
            view = new View(activity);
        }
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    //避免重複Toast
    private void showTextToast(String msg) {
        if (toast == null) {
            toast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        } else {
            toast.setText(msg);
        }
        toast.show();
    }

}
