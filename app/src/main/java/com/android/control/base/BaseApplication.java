package com.android.control.base;

import android.widget.Toast;

import com.android.control.utils.SaveUtils;
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

    @Override
    protected void startServiceListener() {
        if (SaveUtils.getIsHint(this))
            Toast.makeText(this, "守护成功", Toast.LENGTH_SHORT).show();
    }

}
