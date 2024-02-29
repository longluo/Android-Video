package me.longluo.video;

import me.longluo.video.util.FrameDropper;
import timber.log.Timber;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class DropFrameTest {

    @org.junit.Test
    public void test() throws Exception {
        doTest(600, 60, 24);
    }

    private void doTest(int totalFrame, int srcFrameRate, int targetFrameRate) {
        FrameDropper dropper = new FrameDropper(srcFrameRate, targetFrameRate);
        Timber.i("totalFrame:" + totalFrame + ",srcFrameRate:" + srcFrameRate + ",targetFrameRate:" + targetFrameRate);
        for (int i = 0; i < totalFrame; i++) {
            boolean drop = dropper.checkDrop(i);
            Timber.i("第" + i + "帧,drop:" + drop);
        }

        dropper.printResult();
    }
}
