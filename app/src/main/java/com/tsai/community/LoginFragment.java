package com.tsai.community;


import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.List;

import MyMethod.CheckInput;
import MyMethod.PersistentCookieStore;
import MyMethod.SendEmail;
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
public class LoginFragment extends Fragment {
    private EditText et_Account;
    private EditText et_Password;
    private String account;
    private String password;
    private OkHttpClient client;
    private MainActivity mainActivity;

    public LoginFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_login, container, false);
        findViews(view);
        mainActivity = (MainActivity) getActivity();
        return view;
    }


    private void getStr() {
        account = et_Account.getText().toString();
        password = et_Password.getText().toString();
    }

    private void findViews(View view) {
        et_Account = (EditText) view.findViewById(R.id.et_Account);
        et_Password = (EditText) view.findViewById(R.id.et_Password);
        Button bt_Login = (Button) view.findViewById(R.id.bt_Login);
        Button bt_GoRegister = (Button) view.findViewById(R.id.bt_GoRegister);
        bt_Login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Login(v);
            }
        });

        bt_GoRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GoRegister(v);
            }
        });

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

    public void Login(View v) {
        getStr();
        mainActivity.hide_keyboard(getActivity());
        if (new CheckInput().CheckBlank(account) && new CheckInput().CheckBlank(password)) {
            RequestBody formBody = new FormBody.Builder()
                    .add("Account", account)
                    .add("Password", password)
                    .add("CaptchaCode", "c8763")
                    .build();

            Request request = new Request.Builder()
                    .url(getString(R.string.server_url) + "Api/MemberApi/Login")
                    .post(formBody)
                    .build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getActivity(), "請檢察網路連線", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    final int StatusCode = response.code();
                    Gson gson = new Gson();
                    final String ResMsg = gson.fromJson(response.body().string(), String.class);

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (StatusCode == 202) {
                                mainActivity.Login();
                            } else if (StatusCode == 200) {

                                if (ResMsg.equals("未通過Email驗證，請至信箱收取驗證信。")) {

                                    new AlertDialog.Builder(getActivity())
                                            .setTitle("請驗證信箱")
                                            .setMessage("重新按寄出以驗證您的Email")
                                            .setPositiveButton("重新寄出", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    new SendEmail(account);
                                                    et_Password.setText("");
                                                }
                                            }).show();
                                } else {
                                    new AlertDialog.Builder(getActivity())
                                            .setTitle("錯誤訊息")
                                            .setMessage(ResMsg)
                                            .setPositiveButton("知道了", null)
                                            .show();
                                    et_Password.setText("");
                                }
                            } else if (StatusCode == 400) {

                                new AlertDialog.Builder(getActivity())
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
            new AlertDialog.Builder(getActivity())
                    .setTitle("錯誤訊息")
                    .setMessage("格式不正確\n空值或包含空格")
                    .setPositiveButton("知道了", null)
                    .show();
        }

    }

    public void GoRegister(View v) {
        Intent intent = new Intent(getActivity(), RegisterActivity.class);
        startActivity(intent);
    }

}
