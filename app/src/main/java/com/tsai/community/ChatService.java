package com.tsai.community;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import java.util.concurrent.ExecutionException;

import ViewModel.GroupChatView;
import microsoft.aspnet.signalr.client.SignalRFuture;
import microsoft.aspnet.signalr.client.hubs.HubConnection;
import microsoft.aspnet.signalr.client.hubs.HubProxy;
import microsoft.aspnet.signalr.client.hubs.SubscriptionHandler2;
import microsoft.aspnet.signalr.client.transport.ServerSentEventsTransport;

public class ChatService extends Service {
    private final String HUB_URL = "http://163.17.136.228/ConGroup/signalr";
    private final String HUB_NAME = "chatHub_All";

    private SignalRFuture<Void> mSignalRFuture;
    private HubProxy mHub;

    public ChatService() {

    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        HubConnection connection = new HubConnection(HUB_URL);
        mHub = connection.createHubProxy(HUB_NAME);
        mSignalRFuture = connection.start(new ServerSentEventsTransport(connection.getLogger()));
        //可以理解為訊息or事件監聽器
        mHub.on("addMessage", new SubscriptionHandler2<GroupChatView.ChatData, String>() {
            @Override
            public void run(GroupChatView.ChatData chatData, String CTime) {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.putExtra("ViewItem", 1);
                PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(),
                        0,
                        intent,
                        PendingIntent.FLAG_CANCEL_CURRENT);
                final Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION); // 通知音效的URI
                Notification notification = new NotificationCompat.Builder(getApplicationContext())
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("聊天室有新的訊息")
                        .setContentText(chatData.SendAccount + " 說了些話")
                        .setContentIntent(pendingIntent)
                        .setSound(soundUri)
                        .setAutoCancel(true)
                        .setWhen(System.currentTimeMillis())
                        .build();
                NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                manager.notify(1, notification);
            }


        }, GroupChatView.ChatData.class, String.class);

        mHub.on("addNewMember", new SubscriptionHandler2<String, Boolean>() {
            @Override
            public void run(String name, final Boolean isAdmin) {
                name = isAdmin ? "管理員 " + name : name;

                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.putExtra("ViewItem", 1);
                PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(),
                        0,
                        intent,
                        PendingIntent.FLAG_CANCEL_CURRENT);
                final Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION); // 通知音效的URI
                Notification notification = new NotificationCompat.Builder(getApplicationContext())
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(name + " 上線了!")
                        .setContentText("去跟他聊天吧^^")
                        .setContentIntent(pendingIntent)
                        .setSound(soundUri)
                        .setAutoCancel(true)
                        .setWhen(System.currentTimeMillis())
                        .build();
                NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                manager.notify(2, notification);
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
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        //關閉連線
        if (mSignalRFuture != null) {
            mSignalRFuture.cancel();
        }
        super.onDestroy();
    }
}
