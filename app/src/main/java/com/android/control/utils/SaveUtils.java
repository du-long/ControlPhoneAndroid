package com.android.control.utils;

import android.app.Activity;
import android.content.Context;

/**
 * Created by dulong on 2017/12/1.
 */

public class SaveUtils {
    private SaveUtils() {
    }

    public static boolean saveUserName(Context context, String userName) {
        return context.getSharedPreferences("Config", Activity.MODE_PRIVATE).edit().putString("username", userName).commit();
    }

    public static String getUserName(Context context) {
        return context.getSharedPreferences("Config", Activity.MODE_PRIVATE).getString("username", "");
    }

    public static boolean saveIsHint(Context context, boolean isHint) {
        return context.getSharedPreferences("Config", Activity.MODE_PRIVATE).edit().putBoolean("isHint", isHint).commit();
    }

    public static boolean getIsHint(Context context) {
        return context.getSharedPreferences("Config", Activity.MODE_PRIVATE).getBoolean("isHint", true);
    }
}
