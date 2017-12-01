package com.android.control.base;

import android.app.Application;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;

public class UncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
    private Application mApplication;
    private Thread.UncaughtExceptionHandler defaultUncaughtExceptionHandler;

    public UncaughtExceptionHandler(Application application) {
        mApplication = application;
        defaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        File f = new File(mApplication.getExternalCacheDir(), "crash");
        if (!f.exists()) f.mkdirs();
        long l = System.currentTimeMillis();
        String s = String.format("%tY%<tm%<td%<tH%<tM%<tS%<tL.log", l);
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(new FileOutputStream(new File(f, s), true));
            pw.printf("%tF %<tT.%<tL%n", l);//DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").format(LocalDateTime.now());
            e.printStackTrace(pw);e.printStackTrace();
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        } finally {
            if (pw != null) {
                pw.close();
            }
            //defaultUncaughtExceptionHandler.uncaughtException(t, e);
        }
        System.exit(0);
    }
}