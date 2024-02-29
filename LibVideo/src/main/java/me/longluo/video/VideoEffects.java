package me.longluo.video;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaMetadataRetriever;

import androidx.annotation.Nullable;

import java.io.File;
import java.util.List;

import timber.log.Timber;


public class VideoEffects {

    /**
     * 鬼畜效果，先按speed倍率对视频进行加速，然后按splitTimeMs分割视频，并对每一个片段做正放+倒放
     */
    public static void doKichiku(Context context, VideoProcessor.MediaSource inputVideo, String outputVideo, @Nullable Integer outBitrate, float speed, int splitTimeMs) throws Exception {
        long s = System.currentTimeMillis();
        File cacheDir = new File(context.getCacheDir(), "kichiku_" + System.currentTimeMillis());
        cacheDir.mkdir();
        if (outBitrate == null) {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            inputVideo.setDataSource(retriever);
            outBitrate = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE));
            retriever.release();
        }
        int bitrate = VideoUtil.getBitrateForAllKeyFrameVideo(inputVideo);
        List<File> fileList;
        try {
            Timber.w("切割视频+");
            fileList = VideoUtil.splitVideo(context, inputVideo, cacheDir.getAbsolutePath(), splitTimeMs, 500, bitrate, speed, 0);
        } catch (MediaCodec.CodecException e) {
            Timber.e(e);
            /** Nexus5上-1代表全关键帧*/
            fileList = VideoUtil.splitVideo(context, inputVideo, cacheDir.getAbsolutePath(), splitTimeMs, 500, bitrate, speed, -1);
        }
        Timber.w("切割视频-");
        Timber.w("合并视频+");
        VideoUtil.combineVideos(fileList, outputVideo, outBitrate, VideoProcessor.DEFAULT_I_FRAME_INTERVAL);
        Timber.w("合并视频-");
        long e = System.currentTimeMillis();
        Timber.e("鬼畜已完成,耗时:" + (e - s) / 1000f + "s");
    }
}
