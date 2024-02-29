package me.longluo.video;

import android.content.Context;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

import me.longluo.video.util.VideoProgressListener;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class AacTest {
//    @Test
//    public void test() throws Exception {
//        File videoFile = new File("/mnt/sdcard/DCIM/Camera/C360VID_20180420_151736.mp4");
//        File aacFile = new File("/mnt/sdcard/test_short.aac");
//        File outFile = new File("/mnt/sdcard/test.mp4");
//        outFile.delete();
//        long s = System.currentTimeMillis();
//        AudioUtil.replaceAudioTrack(videoFile.getAbsolutePath(), aacFile.getAbsolutePath(), outFile.getAbsolutePath(),true);
//        long e = System.currentTimeMillis();
//        Log.e("hwLog","time:"+(e-s)+"ms");
//    }

    @Test
    public void test2() throws Exception {
        Context context = InstrumentationRegistry.getTargetContext();

        File videoFile = new File("/mnt/sdcard/DCIM/Camera/VID_20180921_121547.mp4");
        File outFile = new File("/mnt/sdcard/amr.mp4");

        long s = System.currentTimeMillis();

        VideoProcessor.processor(context)
                .input(videoFile.getAbsolutePath())
                .output(outFile.getAbsolutePath())
                .progressListener(new VideoProgressListener() {
                    @Override
                    public void onProgress(float progress) {

                    }
                })
                .process();

        long e = System.currentTimeMillis();
        Log.e("hwLog", "time:" + (e - s) + "ms");
    }

    //    @Test
    public void testResample() throws Exception {
        File aacFile = new File("/mnt/sdcard/6c.aac");
        MediaExtractor extractor = new MediaExtractor();
        extractor.setDataSource(aacFile.getAbsolutePath());
        int trackIndex = VideoUtil.selectTrack(extractor, true);
        MediaFormat trackFormat = extractor.getTrackFormat(trackIndex);
        int sampleRate = trackFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        int channelCount = trackFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);

        File cacheDir = new File("/mnt/sdcard/test");
        cacheDir.mkdirs();
        File pcmFile = new File(cacheDir, "t.pcm");
        File pcmFile2 = new File(cacheDir, "t2.pcm");
        File pcmFile3 = new File(cacheDir, "t3.pcm");

        pcmFile.createNewFile();

        AudioUtil.decodeToPCM(aacFile.getAbsolutePath(), pcmFile.getAbsolutePath(), null, null);
        AudioUtil.stereoToMonoSimple(pcmFile.getAbsolutePath(), pcmFile2.getAbsolutePath(), channelCount);

        AudioUtil.copyFile(pcmFile2.getAbsolutePath(), "/mnt/sdcard/mo.pcm");
        AudioUtil.reSamplePcm(pcmFile2.getAbsolutePath(), pcmFile3.getAbsolutePath(), sampleRate, 44100, 1);
        AudioUtil.copyFile(pcmFile3.getAbsolutePath(), "/mnt/sdcard/re.pcm");
    }

    public static File checkAndFillPcm(int aacDuration, File aacPcmFile, int videoDuration) {
        if (aacDuration >= videoDuration) {
            return aacPcmFile;
        }
        File cacheFile = new File(aacPcmFile.getAbsolutePath() + ".concat");
        FileInputStream is = null;
        FileOutputStream os = null;
        FileChannel from = null;
        FileChannel to = null;
        try {
            //计算填充次数
            float repeat = videoDuration / (float) aacDuration;
            int repeatInt = (int) repeat;
            //拼接repeatInt次
            is = new FileInputStream(aacPcmFile);
            os = new FileOutputStream(cacheFile);
            from = is.getChannel();
            to = os.getChannel();
            for (int i = 0; i < repeatInt; i++) {
                from.transferTo(0, from.size(), to);
                from.position(0);
            }
            //剩下的部分
            float remain = repeat - repeatInt;
            int remainSize = (int) (aacPcmFile.length() * remain);
            if (remainSize > 1024) {
                from.transferTo(0, remainSize, to);
            }
            from.close();
            to.close();
            aacPcmFile.delete();
            cacheFile.renameTo(aacPcmFile);
            return aacPcmFile;
        } catch (Exception e) {
            e.printStackTrace();
            cacheFile.delete();
            return aacPcmFile;
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
                if (os != null) {
                    os.close();
                }
                if (from != null) {
                    from.close();
                }
                if (to != null) {
                    to.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
