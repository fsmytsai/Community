package com.tsai.community;

import android.content.DialogInterface;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import MyMethod.SendEmail;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RegisterActivity extends AppCompatActivity {
    OkHttpClient client = new OkHttpClient();
    private EditText ed_account;
    private EditText ed_password;
    private EditText ed_passwordCheck;
    private EditText ed_name;
    private EditText ed_email;
    private String account;
    private String password;
    private String passwordCheck;
    private String email;
    private String name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initView();

        findViews();

    }

    private void initView() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setLogo(R.mipmap.ic_launcher);  //左上方logo圖
        setSupportActionBar(toolbar);
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);  //取消Toolbar的內建靠左title(像Actionbar的特性)
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    private void getStr() {
        account = ed_account.getText().toString();
        password = ed_password.getText().toString();
        passwordCheck = ed_passwordCheck.getText().toString();
        email = ed_email.getText().toString();
        name = ed_name.getText().toString();
    }

    private void findViews() {
        ed_account = (EditText) findViewById(R.id.ed_Account);
        ed_password = (EditText) findViewById(R.id.ed_Password);
        ed_passwordCheck = (EditText) findViewById(R.id.ed_PasswordCheck);
        ed_name = (EditText) findViewById(R.id.ed_Name);
        ed_email = (EditText) findViewById(R.id.ed_Email);
    }

    public void Register(View v) {
        getStr();


        if (CheckInput()) {
            RequestBody formBody = new FormBody.Builder()
                    .add("newMember.Account", account)
                    .add("newMember.Name", name)
                    .add("newMember.Email", email)
                    .add("Password", password)
                    .add("PasswordCheck", passwordCheck)
                    .build();

            Request request = new Request.Builder()
                    .url(getString(R.string.server_url) + "Api/MemberApi/Register")
                    .post(formBody)
                    .build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(RegisterActivity.this, "請檢察網路連線", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    final int StatusCode = response.code();
                    final String ResMsg = parseJSON(response.body().string());
                    runOnUiThread(new Runnable() {//这是Activity的方法，会在主线程执行任务
                        @Override
                        public void run() {

                            if (StatusCode == 200) {

                                if (ResMsg.equals("0")) {

                                    new AlertDialog.Builder(RegisterActivity.this)
                                            .setTitle("註冊成功")
                                            .setMessage("按寄出以驗證您的Email")
                                            .setPositiveButton("寄出", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    new SendEmail(account);
                                                    finish();
                                                }
                                            }).show();
                                } else {
                                    new AlertDialog.Builder(RegisterActivity.this)
                                            .setTitle("錯誤訊息")
                                            .setMessage(ResMsg)
                                            .setPositiveButton("知道了", null)
                                            .show();
                                    ed_account.setText("");
                                    ed_password.setText("");
                                    ed_passwordCheck.setText("");
                                }
                            } else if (StatusCode == 400) {

                                new AlertDialog.Builder(RegisterActivity.this)
                                        .setTitle("錯誤訊息")
                                        .setMessage("格式不正確\n空值或包含空格，或兩次密碼輸入不同，也可能是Email格式錯誤")
                                        .setPositiveButton("知道了", null)
                                        .show();

                            }
                        }
                    });
                }
            });

        } else {
            new AlertDialog.Builder(this)
                    .setTitle("錯誤訊息")
                    .setMessage("格式不正確\n空值或包含空格，或兩次密碼輸入不同，也可能是Email格式錯誤")
                    .setPositiveButton("知道了", null)
                    .show();
        }

    }

    private boolean CheckInput() {
        if (CheckBlanck(account) && CheckBlanck(password) && CheckBlanck(passwordCheck) && CheckBlanck(name) && CheckBlanck(email) && password.equals(passwordCheck)) {
            return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
        }
        return false;
    }

    private boolean CheckBlanck(String str) {
        //回傳true表示通過
        Pattern pattern = Pattern.compile("[\\s]+");
        Matcher matcher = pattern.matcher(str);
        while (matcher.find()) {
            return false;
        }
        if (str.equals("")) return false;
        return true;
    }

    private String parseJSON(String JSONStr) {
        Gson gson = new Gson();

        String ResMsg = gson.fromJson(JSONStr, String.class);
        return ResMsg;
    }

}
