package com.android.control;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import cn.jpush.android.service.PushService;

/**
 * 唤醒服务广播监听
 * Created by dulong on 2017/12/8.
 */

public class TestBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("tjhq--", "onReceive: " + intent.getAction());
        context.startService(new Intent(context, PushService.class));

    }
}
