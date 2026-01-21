package com.google.easyapp;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.view.Gravity;

import com.google.easyapp.utils.ToastUtils;

public class APPUtils {
    private static Application sApplication;
    public static final Handler UI_HANDLER = new Handler(Looper.getMainLooper());

    /**
     * 初始化工具类
     */
    public static void init(@NonNull final Application app) {
        APPUtils.sApplication = app;
        ToastUtils.setGravity(Gravity.CENTER, 0, 0);
    }

    /**
     * 获取 Application
     */
    public static Application getApp() {
        if (sApplication != null) return sApplication;
        throw new NullPointerException("u should init first");
    }

}
