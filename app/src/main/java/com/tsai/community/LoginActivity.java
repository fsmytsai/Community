package com.tsai.community;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.List;

import MyMethod.CheckInput;
import MyMethod.PersistentCookieStore;
import MyMethod.SendEmail;
import MyMethod.SysApplication;
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

public class LoginActivity extends AppCompatActivity {

    private EditText ed_account;
    private EditText ed_password;
    private String account;
    private String password;
    private OkHttpClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        SysApplication.getInstance().addActivity(this);
        initView();
        findViews();
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
    }

    private void getStr() {
        account = ed_account.getText().toString();
        password = ed_password.getText().toString();
    }

    private void findViews() {
        ed_account = (EditText) findViewById(R.id.ed_Account);
        ed_password = (EditText) findViewById(R.id.ed_Password);
    }

    public void Login(View v) {
        getStr();

        if (new CheckInput().CheckBlank(account) && new CheckInput().CheckBlank(password)) {
            RequestBody formBody = new FormBody.Builder()
                    .add("Account", account)
                    .add("Password", password)
                    .build();

            Request request = new Request.Builder()
                    .url(getString(R.string.server_url) + "Api/MemberApi/Login")
                    .post(formBody)
                    .build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(LoginActivity.this, "請檢察網路連線", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    final int StatusCode = response.code();
                    Gson gson = new Gson();
                    final String ResMsg = gson.fromJson(response.body().string(), String.class);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (StatusCode == 202) {
                                setResult(RESULT_OK, getIntent());
                                finish();
                            } else if (StatusCode == 200) {

                                if (ResMsg.equals("未通過Email驗證，請至信箱收取驗證信。")) {

                                    new AlertDialog.Builder(LoginActivity.this)
                                            .setTitle("請驗證信箱")
                                            .setMessage("重新按寄出以驗證您的Email")
                                            .setPositiveButton("重新寄出", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    new SendEmail(account);
                                                    ed_password.setText("");
                                                }
                                            }).show();
                                } else {
                                    new AlertDialog.Builder(LoginActivity.this)
                                            .setTitle("錯誤訊息")
                                            .setMessage(ResMsg)
                                            .setPositiveButton("知道了", null)
                                            .show();
                                    ed_password.setText("");
                                }
                            } else if (StatusCode == 400) {

                                new AlertDialog.Builder(LoginActivity.this)
                                        .setTitle("錯誤訊息")
                                        .setMessage("格式不正確\n空值或包含空格")
                                        .setPositiveButton("知道了", null)
                                        .show();

                            }
                        }
                    });
                }
            });

        } else {
            new AlertDialog.Builder(LoginActivity.this)
                    .setTitle("錯誤訊息")
                    .setMessage("格式不正確\n空值或包含空格")
                    .setPositiveButton("知道了", null)
                    .show();
        }

    }

    public void GoRegister(View v) {
        Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        SysApplication.getInstance().exit();
    }
}
