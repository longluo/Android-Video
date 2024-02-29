package me.longluo.video.demo;

import android.app.Application;

import timber.log.Timber;

public class VideoApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        initSdk();
    }

    private void initSdk() {
        Timber.plant(new Timber.DebugTree());
    }
}
