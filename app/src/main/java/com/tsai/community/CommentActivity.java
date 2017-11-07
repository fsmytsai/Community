package com.tsai.community;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import MyMethod.PersistentCookieStore;
import ViewModel.CommentView;
import ViewModel.IdentityData;
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

public class CommentActivity extends AppCompatActivity {

    private int pid;
    private int AppendTimes = 1;

    private CommentView commentView;
    private List<CommentView.Comments> commentList;
    private List<String> cTimeList = new ArrayList<>();

    private OkHttpClient client;

    private EditText ed_AddComment;
    private ListView lv_comment;
    private CommentListAdapter commentListAdapter;

    //避免重複Toast
    private Toast toast = null;

    private IdentityData identityData = new IdentityData();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comment);

        initView();
        pid = getIntent().getIntExtra("Pid", -1);
        identityData.IsAdmin = getIntent().getBooleanExtra("IsAdmin", false);
        identityData.UserName = getIntent().getStringExtra("UserName");

        ed_AddComment = (EditText) findViewById(R.id.ed_AddComment);

        lv_comment = (ListView) findViewById(R.id.lv_Comment);
        lv_comment.setEmptyView(findViewById(R.id.tv_Empty));
    }

    @Override
    protected void onResume() {
        super.onResume();

        client = new OkHttpClient().newBuilder()
                .cookieJar(new CookieJar() {
                    private final PersistentCookieStore cookieStore = new PersistentCookieStore(getApplicationContext());

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
        GetComments();
    }

    private void initView() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setLogo(R.mipmap.ic_launcher);  //左上方logo圖
        setSupportActionBar(toolbar);
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);  //取消Toolbar的內建靠左title(像Actionbar的特性)
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    //避免返回鍵造成重建MainActivity
    @Override
    public Intent getSupportParentActivityIntent() {
        setResult(RESULT_OK, getIntent());
        finish();
        return null;
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_OK, getIntent());
        finish();
    }

    public void Refresh() {
        //避免重新接上網路時重整導致崩潰
        if (AppendTimes != 1) {
            commentList = new ArrayList<>();
            cTimeList = new ArrayList<>();
            AppendTimes = 1;
            commentListAdapter.notifyDataSetChanged();
        }
        GetComments();
    }

    private void GetComments() {
        Request request = new Request.Builder()
                .url(getString(R.string.server_url) + "Api/CommentApi/DisplayComments?Pid=" + pid + "&AppendTimes=" + AppendTimes)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                showTextToast("請檢察網路連線");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final int StatusCode = response.code();
                final String ResMsg = response.body().string();
                if (StatusCode == 200) {
                    Gson gson = new Gson();
                    commentView = gson.fromJson(ResMsg, CommentView.class);
                    if (commentView.CommentList.size() == 0) {
                        return;
                    }
                    commentList = new ArrayList<>();
                    commentList.addAll(commentView.CommentList);
                    cTimeList.addAll(commentView.CTime);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (AppendTimes == 1) {
                                ListView lv_Comment = (ListView) findViewById(R.id.lv_Comment);
                                lv_Comment.setEmptyView(findViewById(R.id.tv_Empty));
                                commentListAdapter = new CommentListAdapter(CommentActivity.this);
                                lv_Comment.setAdapter(commentListAdapter);
                                lv_Comment.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
                                    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                                        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
                                        menu.setHeaderTitle("操作留言");
                                        if (commentList.get(info.position).Account.equals(identityData.UserName) || identityData.IsAdmin) {
                                            menu.add(0, 0, 0, "修改留言");
                                            menu.add(0, 1, 0, "删除留言");
                                        } else {
                                            showTextToast("無權限操作此留言");
                                        }

                                    }
                                });
                            } else {
                                commentListAdapter.notifyDataSetChanged();
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

        switch (item.getItemId()) {
            case 0:
                Edit(info.position);  //更新事件的方法
                return true;
            case 1:
                Delete(info.position);  //刪除事件的方法
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    public class CommentListAdapter extends BaseAdapter {
        private LayoutInflater mInflater;
        private CommentView.Comments commentByPosition;

        public CommentListAdapter(Context context) {

            mInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return commentList.size();
        }

        @Override
        public Object getItem(int position) {
            return commentList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return commentList.get(position).ComId;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View CL = convertView;
            final Holder holder;
            if (CL == null) {
                CL = mInflater.inflate(R.layout.comment_block, null);
                holder = new Holder();
                holder.tv_Account = (TextView) CL.findViewById(R.id.tv_Account);
                holder.tv_Content = (TextView) CL.findViewById(R.id.tv_Content);
                holder.tv_CreateTime = (TextView) CL.findViewById(R.id.tv_CreateTime);
                CL.setTag(holder);

            } else {
                holder = (Holder) CL.getTag();
            }

            commentByPosition = commentList.get(position);


            holder.tv_Account.setText(commentByPosition.Account);
            holder.tv_Content.setText(commentByPosition.Content);
            holder.tv_CreateTime.setText(cTimeList.get(position));

            if (position > commentList.size() * 0.6 && AppendTimes <= commentView.AppendComment.MaxAppendTimes) {
                AppendTimes++;
                if (AppendTimes > commentView.AppendComment.MaxAppendTimes) {
                    showTextToast("已加載完畢");
                } else {
                    GetComments();
                }
            }
            return CL;
        }

        class Holder {
            TextView tv_Account;
            TextView tv_Content;
            TextView tv_CreateTime;
        }

    }

    public void AddComment(View v) {
        hide_keyboard(this);
        final String Comment = ed_AddComment.getText().toString();
        if (!Comment.trim().equals("")) {
            RequestBody formBody = new FormBody.Builder()
                    .add("PostId", String.valueOf(pid))
                    .add("Content", Comment)
                    .build();

            Request request = new Request.Builder()
                    .url(getString(R.string.server_url) + "Api/CommentApi/Add")
                    .post(formBody)
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
                    final String ResMsg = response.body().string();
                    runOnUiThread(new Runnable() {//这是Activity的方法，会在主线程执行任务
                        @Override
                        public void run() {
                            if (StatusCode == 200) {
                                ed_AddComment.setText("");
                                Refresh();
                                return;
                            } else if (StatusCode == 400) {
                                new AlertDialog.Builder(CommentActivity.this)
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
            new AlertDialog.Builder(CommentActivity.this)
                    .setTitle("錯誤訊息")
                    .setMessage("格式不正確\n   空值")
                    .setPositiveButton("知道了", null)
                    .show();
        }
    }

    public void Edit(final int position) {
        if (commentList.get(position).Account.equals(identityData.UserName) || identityData.IsAdmin) {
            final View editView = LayoutInflater.from(CommentActivity.this).inflate(R.layout.edit_view, null);
            final EditText et_EditContent = (EditText) editView.findViewById(R.id.et_EditContent);
            et_EditContent.setText(commentList.get(position).Content);
            et_EditContent.setSelection(et_EditContent.length());
            new AlertDialog.Builder(this)
                    .setTitle("修改留言")
                    .setView(editView)
                    .setNeutralButton("取消", null)
                    .setPositiveButton("修改", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            RequestBody formBody = new FormBody.Builder()
                                    .add("ComId", String.valueOf(commentList.get(position).ComId))
                                    .add("Content", et_EditContent.getText().toString())
                                    .build();

                            Request request = new Request.Builder()
                                    .url(getString(R.string.server_url) + "Api/CommentApi/Edit")
                                    .post(formBody)
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
                                    final String ResMsg = response.body().string();

                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (StatusCode == 200) {
                                                showTextToast("修改成功");
                                                CommentView.Comments TempData = commentList.get(position);
                                                TempData.Content = et_EditContent.getText().toString();
                                                commentList.set(position, TempData);
                                                commentListAdapter.notifyDataSetChanged();
                                            } else if (StatusCode == 400) {
                                                new AlertDialog.Builder(CommentActivity.this)
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
            showTextToast("你必須是留言者或管理員哦!!");
        }
    }

    public void Delete(final int position) {
        if (commentList.get(position).Account.equals(identityData.UserName) || identityData.IsAdmin) {
            new AlertDialog.Builder(this)
                    .setMessage("確定要刪除此貼文嗎?")
                    .setNeutralButton("取消", null)
                    .setPositiveButton("刪除", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Request request = new Request.Builder()
                                    .url(getString(R.string.server_url) + "Api/CommentApi/Delete?Cid=" + commentList.get(position).ComId)
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
                                    final String ResMsg = response.body().string();

                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (StatusCode == 200) {
                                                showTextToast("刪除成功");
                                                commentList.remove(position);
                                                cTimeList.remove(position);
                                                commentListAdapter.notifyDataSetChanged();
                                            } else if (StatusCode == 400) {
                                                new AlertDialog.Builder(CommentActivity.this)
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
            showTextToast("你必須是留言者或管理員哦!!");
        }

    }


    //關閉鍵盤
    public static void hide_keyboard(Activity activity) {
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
            toast = Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT);
        } else {
            toast.setText(msg);
        }
        toast.show();
    }
}
