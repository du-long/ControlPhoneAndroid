package com.dulong.jpush;

import android.content.Context;
import android.util.Log;

import com.dulong.jpush.daemon.DaemonReceiver;
import com.dulong.jpush.daemon.DaemonService;
import com.dulong.jpush.daemon.MainReceiver;
import com.marswin89.marsdaemon.DaemonApplication;
import com.marswin89.marsdaemon.DaemonConfigurations;

import cn.jpush.android.api.JPushInterface;
import cn.jpush.android.service.PushService;

/**
 * For developer startup JPush SDK
 * <p>
 * 一般建议在自定义 Application 类里初始化。也可以在主 Activity 里。
 */
public abstract class JpushApplication extends DaemonApplication {
    private static final String TAG = "JIGUANG-Example";

    @Override
    public void onCreate() {
        Logger.d(TAG, "[JpushApplication] onCreate");
        super.onCreate();

        JPushInterface.setDebugMode(false);    // 设置开启日志,发布时请关闭日志
        JPushInterface.init(this);            // 初始化 JPush
    }
//    @Override
//    protected void attachBaseContext(Context base) {
//        super.attachBaseContext(base);
//        mDaemonClient = new DaemonClient(createDaemonConfigurations());
//        mDaemonClient.onAttachBaseContext(base);
//    }


    @Override
    public void attachBaseContextByDaemon(Context base) {
        super.attachBaseContextByDaemon(base);
    }

    @Override
    protected DaemonConfigurations getDaemonConfigurations() {
        DaemonConfigurations.DaemonConfiguration configuration1 = new DaemonConfigurations.DaemonConfiguration(
                "com.android.control:main",
                PushService.class.getCanonicalName(),
                MainReceiver.class.getCanonicalName());

        DaemonConfigurations.DaemonConfiguration configuration2 = new DaemonConfigurations.DaemonConfiguration(
                "com.android.control:daemon",
                DaemonService.class.getCanonicalName(),
                DaemonReceiver.class.getCanonicalName());

        DaemonConfigurations.DaemonListener listener = new MyDaemonListener();
        //return new DaemonConfigurations(configuration1, configuration2);//listener can be null
        return new DaemonConfigurations(configuration1, configuration2, listener);
    }

    class MyDaemonListener implements DaemonConfigurations.DaemonListener {
        @Override
        public void onPersistentStart(Context context) {
            Log.i("tjhq--", "onPersistentStart: ");
            startServiceListener();
        }

        @Override
        public void onDaemonAssistantStart(Context context) {
            Log.i("tjhq--", "onDaemonAssistantStart: ");
        }

        @Override
        public void onWatchDaemonDaed() {
            Log.i("tjhq--", "onWatchDaemonDaed: ");
        }
    }

    /**
     * 被守护的进程启动监听
     */
    protected abstract void startServiceListener();
}
