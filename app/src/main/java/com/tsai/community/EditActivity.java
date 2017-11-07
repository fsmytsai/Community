package com.tsai.community;

import android.content.Intent;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;

import MyMethod.PersistentCookieStore;
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

public class EditActivity extends AppCompatActivity {

    private int pid;
    private String content;
    private OkHttpClient client;
    private EditText ed_editContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);

        initView();
        pid = getIntent().getIntExtra("Pid", -1);
        content = getIntent().getStringExtra("Content");

        ed_editContent = (EditText) findViewById(R.id.ed_EditContent);
        ed_editContent.setInputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        ed_editContent.setSingleLine(false);
        ed_editContent.setHorizontallyScrolling(false);
        ed_editContent.setText(content);
        ed_editContent.setSelection(ed_editContent.length());
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
        finish();
        return null;
    }

    public void Edit(View v) {

        final String EditContent = ed_editContent.getText().toString();
        if (!EditContent.trim().equals("")) {
            RequestBody formBody = new FormBody.Builder()
                    .add("Pid", String.valueOf(pid))
                    .add("Content", EditContent)
                    .build();

            Request request = new Request.Builder()
                    .url(getString(R.string.server_url) + "Api/PostApi/Edit")
                    .post(formBody)
                    .build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(EditActivity.this, "請檢察網路連線", Toast.LENGTH_SHORT).show();
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
                                Toast.makeText(EditActivity.this, "修改成功", Toast.LENGTH_SHORT).show();
                                setResult(RESULT_OK, getIntent());
                                finish();
                            } else if (StatusCode == 400) {
                                new AlertDialog.Builder(EditActivity.this)
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
            new AlertDialog.Builder(EditActivity.this)
                    .setTitle("錯誤訊息")
                    .setMessage("格式不正確\n   空值")
                    .setPositiveButton("知道了", null)
                    .show();
        }
    }
}
