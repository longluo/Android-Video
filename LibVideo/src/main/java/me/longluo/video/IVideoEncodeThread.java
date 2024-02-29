package me.longluo.video;

import android.view.Surface;

import java.util.concurrent.CountDownLatch;


public interface IVideoEncodeThread {
    Surface getSurface();

    CountDownLatch getEglContextLatch();
}
