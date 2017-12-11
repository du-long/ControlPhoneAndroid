package com.dulong.jpush.daemon;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.dulong.jpush.utils.Test;

/**
 * Created by dulong on 2017/7/27.
 */

public class DaemonReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("tjhq--", "onReceive: DaemonReceiver===" + Test.getCurProcessName(context));
    }


}
