package com.qiezitv;

import android.app.Application;

import com.dy.fastframework.application.SuperBaseApp;

public class BaseApp extends SuperBaseApp {
    public static final int FIND_LIVE_AC=1234;
    public static final int COMMENT_STEP=2345;
    @Override
    protected String setBaseUrl() {
        return "http://www.baidu.com/";
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public boolean closeDebugLog() {
        return false;
    }
}
