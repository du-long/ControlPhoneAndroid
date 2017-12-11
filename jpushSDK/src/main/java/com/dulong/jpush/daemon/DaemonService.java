package com.dulong.jpush.daemon;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.dulong.jpush.utils.Test;

/**
 * Created by dulong on 2017/7/27.
 */

public class DaemonService extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("tjhq--", "onReceive: DaemonService===" + Test.getCurProcessName(getApplicationContext()));
        return Service.START_NOT_STICKY;
    }
}
