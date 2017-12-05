package com.android.control.jpush;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.android.control.MainActivity;
import com.android.control.utils.RetrofitUtil;
import com.android.control.utils.SaveUtils;
import com.android.control.video.VideoService;
import com.google.gson.Gson;

import java.util.HashMap;

import cn.jpush.android.api.JPushInterface;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

/**
 * 接收Jpush推送过来的消息
 */
public class JPushReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            Bundle bundle = intent.getExtras();
            if (JPushInterface.ACTION_MESSAGE_RECEIVED.equals(intent.getAction())) {
                String message = bundle == null ? null : bundle.getString(JPushInterface.EXTRA_MESSAGE);
                MessageBean messageBean = new Gson().fromJson(message, MessageBean.class);
                switch (messageBean.type) {
                    case 1:
                        onLineUser(context, messageBean);
                        break;
                    case 2:
                        startSendVideoService(context, messageBean);
                        break;
                    case 3:
                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void onLineUser(Context context, MessageBean messageBean) {
        ApiService apiService = RetrofitUtil.retrofit.create(ApiService.class);
        String baseUrl = "http://" + messageBean.ipAddress + ":8081/control/online?type=2";
        HashMap<String, String> map = new HashMap<>();
        map.put("registrationID", JPushInterface.getRegistrationID(context));
        map.put("userName", SaveUtils.getUserName(context));
        String toJson = new Gson().toJson(map);
        Observable<String> stringObservable = apiService.onLine(baseUrl, toJson);
        stringObservable.subscribe(new Observer<String>() {
            @Override
            public void onSubscribe(Disposable d) {
                Log.i("tjhq--", "onSubscribe: ");
            }

            @Override
            public void onNext(String value) {

                Log.i("tjhq--", "onNext: " + value);

            }

            @Override
            public void onError(Throwable e) {
                Log.i("tjhq--", "onError: ");
            }

            @Override
            public void onComplete() {
                Log.i("tjhq--", "onComplete: ");
            }
        });
    }

    void startSendVideoService(Context context, MessageBean messageBean) {
        Intent i = new Intent(context, VideoService.class);
        context.stopService(i);
        i.putExtra(MainActivity.RTMPURL_MESSAGE, "rtmp://" + messageBean.ipAddress + "/live/" + JPushInterface.getRegistrationID(context));
        context.startService(i);
    }
}
