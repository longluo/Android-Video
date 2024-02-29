package me.longluo.video;

import android.content.Context;
import android.util.Log;

import java.io.File;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class Test {

    @org.junit.Test
    public void test() throws Exception {
        CL.setLogEnable(true);
        Context context = InstrumentationRegistry.getTargetContext();
        File file = new File("/storage/emulated/0/Tencent/QQfile_recv/VID_20180217_103218.mp4");

        VideoProcessor.processor(context)
                .input(file.getAbsolutePath())
                .output(context.getCacheDir() + "/" + "test.mp4")
                .outWidth(1137)
                .outHeight(640)
                .bitrate(59166668)
                .frameRate(15)
                .iFrameInterval(0)
                .progressListener(new VideoProgressListener() {
                    @Override
                    public void onProgress(float progress) {
                        Log.e("hwLog", "progress:" + progress);
                    }
                })
                .process();
        CL.setLogEnable(false);
    }
}
