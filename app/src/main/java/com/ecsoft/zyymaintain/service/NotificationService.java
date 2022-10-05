package com.ecsoft.zyymaintain.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.ecsoft.zyymaintain.R;
import com.ecsoft.zyymaintain.WorkOrderDetailActivity;
import com.ecsoft.zyymaintain.config.GlobalConfiguration;
import com.ecsoft.zyymaintain.ui.list.adapter.entity.WorkOrderPO;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class NotificationService extends Service {
    private String wsUrl = "";
    // 创建客户端和ws实例
    private OkHttpClient wsClient;
    private WebSocket    webSocket;
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {

        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createChannel(); // 创建信道
        Log.e("EEEEE","服务启动！！！！！");
        Bundle data = intent.getBundleExtra("data");
        String userId  = data.getString("userId");
        String userToken = data.getString("userToken");
        wsUrl = GlobalConfiguration.webSocket+"/websocket/broadcast/"+userId+"/"+userToken;
        wsClient = new OkHttpClient.Builder()
                .pingInterval(3, TimeUnit.MINUTES)
                .build();
        Request request = new Request.Builder()
                .url(wsUrl)
                .build();
        webSocket = wsClient.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
                super.onClosed(webSocket, code, reason);
            }

            @Override
            public void onClosing(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
                super.onClosing(webSocket, code, reason);
            }

            @Override
            public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, @Nullable Response response) {
                super.onFailure(webSocket, t, response);
            }

            @Override
            public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
                super.onMessage(webSocket, text);
                sendNotification("中医院报检系统",text); // 发送通知
            }

            @Override
            public void onMessage(@NonNull WebSocket webSocket, @NonNull ByteString bytes) {
                super.onMessage(webSocket, bytes);
            }

            @Override
            public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
                super.onOpen(webSocket, response);
            }
        });


        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

    }

    private void createChannel(){
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String channelID = "sjksfjksdjfk";
        CharSequence name = "WS消息通道";
        int importance = NotificationManager.IMPORTANCE_HIGH;
        NotificationChannel channel = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO){
            channel = new NotificationChannel(channelID,name,NotificationManager.IMPORTANCE_HIGH);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{100,200,300,400,500,400,300,200,100});
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            notificationManager.createNotificationChannel(channel);
        }
    }

    /**
     * 发送通知
     * @param title 通知标题
     * @param content 通知内容
     */
    private void sendNotification(String title,String content){
        Context context = getApplicationContext();
        // 创建通道ID
        String channelId = "sjksfjksdjfk";

        try {
            JSONObject jsonObject = (JSONObject) new JSONTokener(content).nextValue();
            switch (jsonObject.getInt("code")){
                case 1001:{ // 工单审批通知
                    Intent intent = new Intent(context, WorkOrderDetailActivity.class);
                    JSONObject record = jsonObject.getJSONObject("record");
                    WorkOrderPO workOrderPO = new WorkOrderPO();
                    workOrderPO.setRid(record.getInt("rid"));
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'");
                    workOrderPO.setStartTime(dateFormat.parse(record.getString("startTime")));
                    workOrderPO.setEndTime(dateFormat.parse(record.getString("endTime")));
                    workOrderPO.setTitle(record.getString("title"));
                    workOrderPO.setContent(record.getString("content"));
                    workOrderPO.setCurrentStatus(record.getInt("currentStatus"));
                    workOrderPO.setCreateUser(record.getLong("createUser"));
                    if (record.has("imgUrl")){
                        workOrderPO.setImgUrl(record.getString("imgUrl"));
                    }

                    Bundle bundle = new Bundle();
                    bundle.putSerializable("entityData",workOrderPO);
                    intent.putExtra("data",bundle);


                    PendingIntent pendingIntent = PendingIntent.getActivity(context,0,intent,PendingIntent.FLAG_IMMUTABLE);
                    NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

                    Notification notification = new NotificationCompat.Builder(context,channelId)
                            .setContentTitle(title) // 设置标题
                            .setContentText(jsonObject.getString("notificationMessage")) // 设置内容
                            .setWhen(System.currentTimeMillis()) // 设置通知时间
                            .setDefaults(Notification.DEFAULT_SOUND|Notification.DEFAULT_VIBRATE) // 设置通知默认铃声和可见度
                            .setContentIntent(pendingIntent) // 设置点击跳转的Intent
                            .setSmallIcon(R.mipmap.ic_launcher) // 设置小图标
                            .setAutoCancel(false) // 设置是否自动关闭
                            .build();
                    notification.defaults = Notification.DEFAULT_ALL;
                    notificationManager.notify(123,notification); // 发送通知
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }


    }
}
