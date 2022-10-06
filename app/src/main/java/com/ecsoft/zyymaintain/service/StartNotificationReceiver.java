package com.ecsoft.zyymaintain.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ShareCompat;

import com.ecsoft.zyymaintain.MainActivity;
import com.ecsoft.zyymaintain.database.DbSettingsService;

public class StartNotificationReceiver extends BroadcastReceiver {
    /*要接收的intent源*/
    static final String ACTION = "android.intent.action.BOOT_COMPLETED";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e("应用接收了广播","onReceive:"+intent.getAction());
        if (ACTION.equals(intent.getAction())){ // 如果广播的Action是开机启动的话
            Intent intentService = new Intent(context, NotificationService.class);
            Bundle bundle = new Bundle();
            DbSettingsService dbSettingsService = new DbSettingsService(context);
            bundle.putString("userName",dbSettingsService.getSettings("tokenUser"));
            bundle.putString("userToken",dbSettingsService.getSettings("loginToken"));
            bundle.putString("userId",dbSettingsService.getSettings("uid"));
            intent.putExtra("data",bundle);
            context.startForegroundService(intentService);
        }
    }
}
