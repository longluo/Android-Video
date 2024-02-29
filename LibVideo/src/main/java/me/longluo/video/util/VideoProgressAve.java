package me.longluo.video.util;

import timber.log.Timber;

public class VideoProgressAve {
    private VideoProgressListener mListener;
    private float mEncodeProgress;
    private float mAudioProgress;
    private int mStartTimeMs;
    private int mEndTimeMs;
    private Float mSpeed;

    public VideoProgressAve(VideoProgressListener listener) {
        mListener = listener;
    }

    public void setEncodeTimeStamp(long timeStampUs) {
        if (mListener == null) {
            return;
        }
        if(mSpeed!=null){
            timeStampUs = (long) (timeStampUs*mSpeed);
        }
        mEncodeProgress = (timeStampUs/1000f)/(mEndTimeMs - mStartTimeMs);
        mEncodeProgress = mEncodeProgress <0?0:mEncodeProgress;
        mEncodeProgress = mEncodeProgress>1?1:mEncodeProgress;
        mListener.onProgress((mEncodeProgress + mAudioProgress) / 2);
        Timber.i("mEncodeProgress:"+mEncodeProgress);
    }

    public void setAudioProgress(float audioProgress) {
        mAudioProgress = audioProgress;
        if (mListener != null) {
            mListener.onProgress((mEncodeProgress + mAudioProgress) / 2);
        }

        Timber.i("mAudioProgress:"+mAudioProgress);
    }

    public void setStartTimeMs(int startTimeMs) {
        mStartTimeMs = startTimeMs;
    }

    public void setEndTimeMs(int endTimeMs) {
        mEndTimeMs = endTimeMs;
    }

    public void setSpeed(Float speed) {
        mSpeed = speed;
    }
}
