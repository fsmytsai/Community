package com.tsai.community;


import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import MyMethod.PersistentCookieStore;
import MyMethod.SimpleDividerItemDecoration;
import ViewModel.GroupChatView;
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
public class ChatFragment extends Fragment {
    private OkHttpClient client;
    private int AddTimesForChat = 1;
    private GroupChatView chatView;
    private List<GroupChatView.ChatData> chatDataList = new ArrayList<>();
    private RecyclerView rv_Chat;
    private ChatAdapter chatAdapter;

    private MainActivity mainActivity;
    private Toast toast = null;

    public ChatFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_chat, container, false);
        mainActivity = (MainActivity) getActivity();

        SetBtClick(view);
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

    public void Refresh() {
        //避免重新接上網路時重整導致崩潰
        if (AddTimesForChat!=1) {
            chatDataList = new ArrayList<>();
            AddTimesForChat = 1;
        }
        SetCookies();
        SetCookies();
        GetChats();
    }

    private void SetBtClick(View view) {

        Button bt_SendChat = (Button) view.findViewById(R.id.bt_SendChat);
        bt_SendChat.setOnClickListener(BtClick);

    }

    View.OnClickListener BtClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.bt_SendChat:
                    SendChat(v);
                    break;
            }
        }
    };

    public void GetChats() {
        Request request = new Request.Builder()
                .url(getString(R.string.server_url)+"Api/ChatApi/GetChat?AppendTimes=" + AddTimesForChat)
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
                    Gson gson = new Gson();
                    chatView = gson.fromJson(ResMsg, GroupChatView.class);

                    if (chatView.GroupChatList.size() == 0) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showTextToast("沒有聊天訊息");
                                setuprv_Chat();
                            }
                        });
                        return;
                    }

                    chatDataList.addAll(chatView.GroupChatList);
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (AddTimesForChat == 1) {
                                setuprv_Chat();
                            } else {
                                chatAdapter.notifyItemRangeInserted(chatAdapter.getItemCount() - 1, chatView.Paging.ItemNum);
                            }
                        }
                    });

                }

            }
        });
    }

    public void setuprv_Chat() {
        rv_Chat = (RecyclerView) getActivity().findViewById(R.id.rv_Chat);
        chatAdapter = new ChatAdapter();
        rv_Chat.setAdapter(chatAdapter);
        rv_Chat.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, true));
        rv_Chat.smoothScrollToPosition(0);
        rv_Chat.addItemDecoration(new SimpleDividerItemDecoration(getActivity()));
    }

    public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            Context context = parent.getContext();
            View view = LayoutInflater.from(context).inflate(R.layout.chat_block, parent, false);
            ViewHolder viewHolder = new ViewHolder(view);
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.tv_ChatName.setText(chatDataList.get(position).SendAccount + " : ");
            holder.tv_ChatContent.setText(chatDataList.get(position).Content);
            if (chatDataList.get(position).SendAccount.equals(mainActivity.identityData.UserName)) {
                holder.tv_ChatName.setTextColor(Color.BLUE);
            } else {
                holder.tv_ChatName.setTextColor(Color.BLACK);
            }


            if (position == chatDataList.size() - 1 && AddTimesForChat <= chatView.Paging.MaxPage) {
                AddTimesForChat++;
                if (AddTimesForChat > chatView.Paging.MaxPage) {
                    //避免只能加載一次的時候也顯示
                    if (AddTimesForChat != 2) {
                        showTextToast("沒有更多聊天訊息了");
                    }
                } else {
                    GetChats();
                }
            }
        }

        @Override
        public int getItemCount() {
            return chatDataList.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            private final TextView tv_ChatName;
            private final TextView tv_ChatContent;

            public ViewHolder(View itemView) {
                super(itemView);
                tv_ChatName = (TextView) itemView.findViewById(R.id.tv_ChatName);
                tv_ChatContent = (TextView) itemView.findViewById(R.id.tv_ChatContent);
            }
        }
    }

    public void addMessage(GroupChatView.ChatData chatData) {
        chatDataList.add(0, chatData);
        chatAdapter.notifyItemInserted(0);
        rv_Chat.smoothScrollToPosition(0);
    }

    public void SendChat(View v) {
        //關閉鍵盤
        mainActivity.hide_keyboard(getActivity());
        final EditText et_ChatContent = (EditText) getActivity().findViewById(R.id.et_ChatContent);

        String Content = et_ChatContent.getText().toString();
        if (!Content.trim().equals("")) {
            RequestBody formBody = new FormBody.Builder()
                    .add("GroupId", "1")
                    .add("Content", Content)
                    .build();

            Request request = new Request.Builder()
                    .url(getString(R.string.server_url)+"Api/ChatApi/Send")
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
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            et_ChatContent.setText("");
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

    public void showTextToast(String msg) {
        if (toast == null) {
            toast = Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT);
        } else {
            toast.setText(msg);
        }
        toast.show();
    }
}
