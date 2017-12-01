package com.android.control.base;

import com.dulong.jpush.JpushApplication;

/**
 * Created by dulong on 2017/7/27.
 */

public class BaseApplication extends JpushApplication {
    @Override
    public void onCreate() {
        super.onCreate();
        new UncaughtExceptionHandler(this);
    }

}
