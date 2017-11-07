package com.tsai.community;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.util.LruCache;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import MyMethod.PersistentCookieStore;
import ViewModel.PostView;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


/**
 * A simple {@link Fragment} subclass.
 */
public class PostListFragment extends Fragment {
    private static final int GET_IMAGE = 66;
    private static final int Comment_CODE = 89;
    private static final int EDIT_CODE = 78;

    private OkHttpClient client;

    private PostView postView;
    private List<PostView.Posts> postList = new ArrayList<>();
    private List<String> cTimeList = new ArrayList<>();
    private List<Boolean> isLikeList = new ArrayList<>();
    private List<Integer> commentNumList = new ArrayList<>();


    //第一次進入DataListFragment才會判斷登入印留言
    private boolean isFirstLoad = true;
    private boolean isLoading = true;
    private boolean isFinishLoad = false;

    private String addImageUrl = "";
    private ImageView iv_addImage;
    public ListView lv_post;
    private PostListAdapter postListAdapter;

    private int maxMemory = (int) Runtime.getRuntime().maxMemory();
    private int cacheSizes = maxMemory / 5;
    //用於修改與回覆的position值
    private int NowPosition = -1;

    public SwipeRefreshLayout mSwipeLayout;
    private Toast toast = null;

    private MainActivity mainActivity;

    public PostListFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_data_list, container, false);
        mainActivity = (MainActivity) getActivity();
        setSwipeRefresh(view);
        mainActivity.CheckLogon();
        return view;
    }

    public void SetCookies() {
        client = new OkHttpClient().newBuilder()
                .cookieJar(new CookieJar() {
                    private final PersistentCookieStore cookieStore = new PersistentCookieStore(getActivity());

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

    private void setSwipeRefresh(View view) {
        mSwipeLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_container);
        mSwipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                //檢查網路連線
                if (!mainActivity.CheckNetWork()) {
                    showTextToast("請檢察網路連線");
                    mSwipeLayout.setRefreshing(false);
                    return;
                }
                //一開始就沒網路，重新確認登入狀態
                if (mainActivity.identityData == null) {
                    mainActivity.CheckLogon();
                    //避免兩次請求
                    return;
                }
                Refresh();
            }
        });
        // 设置下拉圆圈上的颜色，蓝色、绿色、橙色、红色
        mSwipeLayout.setColorSchemeResources(android.R.color.holo_blue_bright, android.R.color.holo_green_light,
                android.R.color.holo_orange_light, android.R.color.holo_red_light);
        mSwipeLayout.setDistanceToTriggerSync(400);// 设置手指在屏幕下拉多少距离会触发下拉刷新
        //mSwipeLayout.setProgressBackgroundColor(R.color.red);
        mSwipeLayout.setSize(SwipeRefreshLayout.DEFAULT);
        //有網路才出現圈圈
        if (mainActivity.CheckNetWork()) {
            mSwipeLayout.setRefreshing(true);
        }
    }

    public void Refresh() {
        //避免重新接上網路時重整導致崩潰
        if (!isFirstLoad) {
            postList = new ArrayList<>();
            cTimeList = new ArrayList<>();
            isLikeList = new ArrayList<>();
            commentNumList = new ArrayList<>();
            isFinishLoad = false;
            NowPosition = -1;
            postListAdapter.notifyDataSetChanged();
        }
        SetCookies();
        GetPosts();
    }

    public void GetPosts() {
        String PidList = "";
        for (int position = 0; position < postList.size(); position++) {
            PidList += "PidList=";
            PidList += String.valueOf(postList.get(position).PostId);
            PidList += "&";
        }

        Request request = new Request.Builder()
                .url(getString(R.string.server_url) + "Api/PostApi/DisplayPosts?" + PidList)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showTextToast("請檢察網路連線");
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final int StatusCode = response.code();
                final String ResMsg = response.body().string();
                if (StatusCode == 200) {

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //設置EmptyView
//                            TextView tv_Empty = (TextView) getActivity().findViewById(R.id.tv_Empty);
//                            tv_Empty.setVisibility(View.VISIBLE);

//                            lv_post.setEmptyView(View.inflate(getActivity(), R.layout.add_view, null));

                            // 停止刷新
                            mSwipeLayout.setRefreshing(false);
                            //請求完畢
                            isLoading = false;

                            Gson gson = new Gson();
                            postView = gson.fromJson(ResMsg, PostView.class);
                            if (postView.PostList.size() == 0) {
                                showTextToast("沒有文章QQ");
                            }
                            postList.addAll(postView.PostList);
                            cTimeList.addAll(postView.CTime);
                            isLikeList.addAll(postView.IsLike);
                            commentNumList.addAll(postView.CommentNum);
                            if (postView.PostList.size() < 10) {
                                isFinishLoad = true;
                                showTextToast("已加載完畢");
                            }
                            if (isFirstLoad) {
                                isFirstLoad = false;
                                lv_post = (ListView) getActivity().findViewById(R.id.lv_Post);
                                View headerView = View.inflate(getActivity(), R.layout.add_view, null);
                                lv_post.addHeaderView(headerView);
                                SetBtClick();

                                EditText et_AddContent = (EditText) getActivity().findViewById(R.id.et_AddContent);
                                et_AddContent.setOnTouchListener(new etTouchListener());

                                postListAdapter = new PostListAdapter(getActivity(), lv_post);
                                lv_post.setAdapter(postListAdapter);

                                lv_post.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
                                    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                                        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;

                                        //由於加了HeaderView導致position會多加1
                                        if (postList.get(info.position - 1).Account.equals(mainActivity.identityData.UserName) || mainActivity.identityData.IsAdmin) {
                                            menu.setHeaderTitle("操作貼文");
                                            menu.add(0, 0, 0, "修改貼文");
                                            menu.add(0, 1, 0, "删除貼文");
                                        } else {
                                            showTextToast("無權限操作此貼文");
                                        }

                                    }
                                });
                            } else {
                                postListAdapter.notifyDataSetChanged();
                            }
                        }
                    });

                }

            }
        });
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        //由於加了HeaderView導致position會多加1
        int position = info.position - 1;
        switch (item.getItemId()) {
            case 0:
                Edit(position);  //更新事件的方法
                return true;
            case 1:
                Delete(position);  //刪除事件的方法
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    class etTouchListener implements View.OnTouchListener {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (v.getId()) {
                case R.id.et_AddContent:
                    // 解决scrollView中嵌套EditText导致不能上下滑动的问题
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                    switch (event.getAction() & MotionEvent.ACTION_MASK) {
                        case MotionEvent.ACTION_UP:
                            v.getParent().requestDisallowInterceptTouchEvent(false);
                            break;
                    }
            }
            return false;
        }
    }

    private void SetBtClick() {

        Button bt_AddImage = (Button) getActivity().findViewById(R.id.bt_AddImage);
        bt_AddImage.setOnClickListener(BtClick);
        Button bt_AddPost = (Button) getActivity().findViewById(R.id.bt_AddPost);
        bt_AddPost.setOnClickListener(BtClick);
    }

    View.OnClickListener BtClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.bt_Like:
                    Like(v);
                    break;
                case R.id.bt_Comment:
                    Comment(v);
                    break;
                case R.id.bt_AddPost:
                    AddPost(v);
                    break;
                case R.id.bt_AddImage:
                    AddImage(v);
                    break;
            }
        }
    };

    public class PostListAdapter extends BaseAdapter implements AbsListView.OnScrollListener {
        private LayoutInflater mInflater;
        private ListView mListView;
        private boolean isFirstIn;
        private ImageLoader imageLoader;
        private int mStart;
        private int mEnd;
        private PostView.Posts postByPosition;

        public PostListAdapter(Context context, ListView listView) {

            mInflater = LayoutInflater.from(context);
            mListView = listView;
            isFirstIn = true;

            imageLoader = new ImageLoader(mListView);
            mListView.setOnScrollListener(this);
        }

        @Override
        public int getCount() {
            return postList.size();
        }

        @Override
        public Object getItem(int position) {
            return postList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return postList.get(position).PostId;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View PL = convertView;
            final Holder holder;
            if (PL == null) {
                PL = mInflater.inflate(R.layout.post_block, null);
                holder = new Holder();
                holder.imageView = (ImageView) PL.findViewById(R.id.imageView);
                holder.tv_Account = (TextView) PL.findViewById(R.id.tv_Account);
                holder.tv_Content = (TextView) PL.findViewById(R.id.tv_Content);
                holder.tv_CreateTime = (TextView) PL.findViewById(R.id.tv_CreateTime);
                holder.bt_Like = (Button) PL.findViewById(R.id.bt_Like);
                holder.bt_Comment = (Button) PL.findViewById(R.id.bt_Comment);
                PL.setTag(holder);

            } else {
                holder = (Holder) PL.getTag();
            }

            postByPosition = postList.get(position);

            //每次都要重設，避免刪除單筆後造成Tag混亂
            holder.imageView.setTag("");
            holder.imageView.setImageDrawable(null);
            if (postByPosition.ImgUrl != null) {
                holder.imageView.setTag(postByPosition.ImgUrl);
                imageLoader.showImage(holder.imageView, postByPosition.ImgUrl);
            }

            holder.tv_Account.setText(postByPosition.Account);
            holder.tv_Content.setText(postByPosition.Content);
            holder.tv_CreateTime.setText(cTimeList.get(position));

            //避免重複請求
            if (position > postList.size() * 0.6 && !isFinishLoad && !isLoading) {
                isLoading = true;
                GetPosts();
            }
            if (postByPosition.LikeNum > 0) {
                holder.bt_Like.setText(String.valueOf(postByPosition.LikeNum) + "個蚌");
            } else {
                holder.bt_Like.setText("蒸蚌");
            }
            if (isLikeList.get(position)) {
                holder.bt_Like.setBackgroundColor(Color.parseColor("#0080ff"));
            } else {
                holder.bt_Like.setBackgroundColor(Color.parseColor("#ffe6e6"));
            }

            if (commentNumList.get(position) > 0) {
                holder.bt_Comment.setText(commentNumList.get(position) + "則留言");
            } else {
                holder.bt_Comment.setText("留言");
            }

            holder.bt_Comment.setBackgroundColor(Color.parseColor("#ffe6e6"));
            holder.bt_Like.setTag(position);
            holder.bt_Like.setOnClickListener(BtClick);
            holder.bt_Comment.setTag(position);
            holder.bt_Comment.setOnClickListener(BtClick);
            return PL;
        }

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
            if (scrollState == SCROLL_STATE_IDLE) {
                imageLoader.loadImages(mStart, mEnd);
            } else {
                imageLoader.cancelAllAsyncTask();
            }
        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            mStart = firstVisibleItem;
            mEnd = firstVisibleItem + visibleItemCount - 1;

            if (isFirstIn && visibleItemCount > 0) {
                imageLoader.loadImages(mStart, mEnd);
                isFirstIn = false;
            }
        }

        class Holder {
            ImageView imageView;
            TextView tv_Account;
            TextView tv_Content;
            TextView tv_CreateTime;
            Button bt_Like;
            Button bt_Comment;
        }

    }

    private class ImageLoader {
        Set<LoadImgAsyncTask> mTasks = new HashSet<>();
        private ListView dataListView;

        public ImageLoader(ListView listView) {
            dataListView = listView;
        }

        public void showImage(ImageView imageView, String url) {

            Bitmap bitmap = getBitmapFromLrucache(url);
            if (bitmap == null) {
                imageView.setImageDrawable(null);
            } else {
                imageView.setImageBitmap(bitmap);
            }
        }

        public void loadImages(int start, int end) {

            for (int i = start; i < end; i++) {
                String loadUrl = postList.get(i).ImgUrl;
                if (loadUrl != null) {
                    ImageView imageView = (ImageView) dataListView.findViewWithTag(loadUrl);
                    if (getBitmapFromLrucache(loadUrl) != null) {
                        imageView.setImageBitmap(getBitmapFromLrucache(loadUrl));
                    } else {
                        LoadImgAsyncTask imgAsyncTask = new LoadImgAsyncTask(imageView, loadUrl);
                        mTasks.add(imgAsyncTask);
                        imgAsyncTask.execute(loadUrl);
                    }
                }

            }
        }

        public void cancelAllAsyncTask() {
            if (mTasks != null) {
                for (LoadImgAsyncTask imgAsyncTask : mTasks) {
                    imgAsyncTask.cancel(false);
                }
            }
        }
    }

    public class LoadImgAsyncTask extends AsyncTask<String, Void, Bitmap> {
        private String mUrl;
        private ImageView dataImageView;

        public LoadImgAsyncTask(ImageView imageView, String url) {
            mUrl = url;
            dataImageView = imageView;
        }

        @Override
        protected Bitmap doInBackground(String... params) {

            String url = params[0];
            Bitmap bitmap;

            bitmap = getBitmapFromUrl(url);
            if (bitmap != null) {
                addBitmapToLrucaches(url, bitmap);
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            if (dataImageView.getTag().equals(mUrl)) {
                dataImageView.setImageBitmap(bitmap);
            }
        }

        public Bitmap getBitmapFromUrl(String urlString) {
            Bitmap bitmap;
            InputStream is = null;
            try {
                URL mUrl = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) mUrl.openConnection();
                is = new BufferedInputStream(connection.getInputStream());
                bitmap = BitmapFactory.decodeStream(is);
                connection.disconnect();
                return bitmap;
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (is != null) {
                        is.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

    }

    private LruCache<String, Bitmap> mMemoryCaches = new LruCache<String, Bitmap>(cacheSizes) {
        @SuppressLint("NewApi")
        @Override
        protected int sizeOf(String key, Bitmap value) {
            return value.getByteCount();
        }
    };

    public Bitmap getBitmapFromLrucache(String url) {
        return mMemoryCaches.get(url);
    }

    public void addBitmapToLrucaches(String url, Bitmap bitmap) {
        if (getBitmapFromLrucache(url) == null) {
            mMemoryCaches.put(url, bitmap);
        }
    }

    public void clearLruCache() {
        if (mMemoryCaches != null) {
            if (mMemoryCaches.size() > 0) {
                mMemoryCaches.evictAll();
            }
        }
    }

    public void AddPost(View v) {
        //關閉鍵盤
        mainActivity.hide_keyboard(getActivity());
        final EditText et_AddContent = (EditText) getActivity().findViewById(R.id.et_AddContent);
        String Content = et_AddContent.getText().toString();
        if (!Content.trim().equals("")) {
            RequestBody formBody = new FormBody.Builder()
                    .add("newPost.GroupId","1")
                    .add("newPost.Content", Content)
//                    .add("ImgUrl", addImageUrl)
                    .build();

            Request request = new Request.Builder()
                    .url(getString(R.string.server_url) + "Api/PostApi/Add")
                    .post(formBody)
                    .build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showTextToast("請檢察網路連線");
                        }
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    final int StatusCode = response.code();
                    final String ResMsg = response.body().string();

                    getActivity().runOnUiThread(new Runnable() {//这是Activity的方法，会在主线程执行任务
                        @Override
                        public void run() {
                            if (StatusCode == 200) {
                                showTextToast("發文成功");
                                Refresh();
                            } else if (StatusCode == 400) {
                                new AlertDialog.Builder(getActivity())
                                        .setTitle("錯誤訊息")
                                        .setMessage(ResMsg)
                                        .setPositiveButton("知道了", null)
                                        .show();
                            }
                            et_AddContent.setText("");
                            if (!addImageUrl.equals("")) {
                                addImageUrl = "";
                                iv_addImage.setImageDrawable(null);
                                iv_addImage.setTag("");
                            }

                        }
                    });
                }
            });

        } else {
            new AlertDialog.Builder(getActivity())
                    .setTitle("錯誤訊息")
                    .setMessage("格式不正確\n空值")
                    .setPositiveButton("知道了", null)
                    .show();
        }

    }

    public void AddImage(View v) {
        clearLruCache();
        Intent intent = new Intent(getActivity(), GalleryActivity.class);
        startActivityForResult(intent, GET_IMAGE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case GET_IMAGE:
                if (resultCode == getActivity().RESULT_OK) {
                    String ImageUrl = data.getStringExtra("ImageUrl");
                    iv_addImage = (ImageView) getActivity().findViewById(R.id.iv_AddImage);
                    addImageUrl = ImageUrl;
                    iv_addImage.setTag(ImageUrl);
                    LoadImgAsyncTask imgAsyncTask = new LoadImgAsyncTask(iv_addImage, ImageUrl);
                    imgAsyncTask.execute(ImageUrl);
                    iv_addImage.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (!addImageUrl.equals("")) {
                                addImageUrl = "";
                                iv_addImage.setImageDrawable(null);
                                iv_addImage.setTag("");
                            }
                        }
                    });
                }
                return;
            case Comment_CODE:
                if (resultCode == getActivity().RESULT_OK) {
                    UpdateDataByPosition();
                }
                return;
            case EDIT_CODE:
                if (resultCode == getActivity().RESULT_OK) {
                    UpdateDataByPosition();
                }
                return;
        }
    }

    public void Comment(View v) {
        final int position = (Integer) v.getTag();
        NowPosition = position;
        Intent intent = new Intent(getActivity(), CommentActivity.class);
        intent.putExtra("Pid", postList.get(position).PostId);
        intent.putExtra("IsAdmin", mainActivity.identityData.IsAdmin);
        intent.putExtra("UserName", mainActivity.identityData.UserName);
        startActivityForResult(intent, Comment_CODE);
        getActivity().overridePendingTransition(R.animator.activity_open,0);
    }

    public void Like(View v) {
        final int position = (Integer) v.getTag();
        NowPosition = position;
        Request request = new Request.Builder()
                .url(getString(R.string.server_url) + "Api/PostApi/Like?Pid=" + postList.get(position).PostId)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showTextToast("請檢察網路連線");
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final int StatusCode = response.code();

                final String ResMsg = response.body().string();

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (StatusCode == 200) {
                            UpdateDataByPosition();
                        } else if (StatusCode == 400) {
                            new AlertDialog.Builder(getActivity())
                                    .setTitle("錯誤訊息")
                                    .setMessage(ResMsg)
                                    .setPositiveButton("知道了", null)
                                    .show();
                        }
                    }
                });
            }
        });
    }

    public void Edit(int position) {
        if (postList.get(position).Account.equals(mainActivity.identityData.UserName) || mainActivity.identityData.IsAdmin) {
            NowPosition = position;
            Intent intent = new Intent(getActivity(), EditActivity.class);
            intent.putExtra("Pid", postList.get(position).PostId);
            intent.putExtra("Content", postList.get(position).Content);
            startActivityForResult(intent, EDIT_CODE);
        } else {
            showTextToast("你必須是發文者或管理員哦!!");
        }
    }

    public void Delete(final int position) {
        if (postList.get(position).Account.equals(mainActivity.identityData.UserName) || mainActivity.identityData.IsAdmin) {
            new AlertDialog.Builder(getActivity())
                    .setMessage("確定要刪除此貼文嗎?")
                    .setNeutralButton("取消", null)
                    .setPositiveButton("確定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Request request = new Request.Builder()
                                    .url(getString(R.string.server_url) + "Api/PostApi/Delete?Pid=" + postList.get(position).PostId)
                                    .build();
                            client.newCall(request).enqueue(new Callback() {
                                @Override
                                public void onFailure(Call call, IOException e) {
                                    getActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            showTextToast("請檢察網路連線");
                                        }
                                    });
                                }

                                @Override
                                public void onResponse(Call call, Response response) throws IOException {
                                    final int StatusCode = response.code();
                                    final String ResMsg = response.body().string();

                                    getActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (StatusCode == 200) {
                                                showTextToast("刪除成功");
                                                postList.remove(position);
                                                cTimeList.remove(position);
                                                isLikeList.remove(position);
                                                commentNumList.remove(position);
                                                postListAdapter.notifyDataSetChanged();
                                            } else if (StatusCode == 400) {
                                                new AlertDialog.Builder(getActivity())
                                                        .setTitle("錯誤訊息")
                                                        .setMessage(ResMsg)
                                                        .setPositiveButton("知道了", null)
                                                        .show();
                                            }
                                        }
                                    });
                                }
                            });
                        }
                    })
                    .show();
        } else {
            showTextToast("你必須是發文者或管理員哦!!");
        }

    }

    private void UpdateDataByPosition() {
        if (NowPosition != -1) {
            Request request = new Request.Builder()
                    .url(getString(R.string.server_url) + "Api/PostApi/GetDataByPid?Pid=" + postList.get(NowPosition).PostId)
                    .build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showTextToast("請檢察網路連線");
                        }
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    final int StatusCode = response.code();

                    final String ResMsg = response.body().string();

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (StatusCode == 200) {
                                Gson gson = new Gson();
                                PostView tempData = gson.fromJson(ResMsg, PostView.class);
                                postList.set(NowPosition, tempData.PostList.get(0));
                                cTimeList.set(NowPosition, tempData.CTime.get(0));
                                isLikeList.set(NowPosition, tempData.IsLike.get(0));
                                commentNumList.set(NowPosition, tempData.CommentNum.get(0));
                                postListAdapter.notifyDataSetChanged();
                                NowPosition = -1;
                            } else if (StatusCode == 400) {
                                new AlertDialog.Builder(getActivity())
                                        .setTitle("錯誤訊息")
                                        .setMessage(ResMsg)
                                        .setPositiveButton("知道了", null)
                                        .show();
                            }
                        }
                    });
                }
            });
        } else {
            showTextToast("更新資料失敗");
        }

    }

    public void showTextToast(String msg) {
        if (toast == null) {
            toast = Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT);
        } else {
            toast.setText(msg);
        }
        toast.show();
    }
}
