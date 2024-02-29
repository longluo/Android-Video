package me.longluo.video;

import static me.longluo.video.VideoProcessor.TIMEOUT_USEC;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.view.Surface;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import me.longluo.video.util.FrameDropper;
import me.longluo.video.util.InputSurface;
import me.longluo.video.util.OutputSurface;
import timber.log.Timber;

public class VideoDecodeThread extends Thread {

    private MediaExtractor mExtractor;

    private MediaCodec mDecoder;

    private Integer mStartTimeMs;
    private Integer mEndTimeMs;

    private Float mSpeed;
    private AtomicBoolean mDecodeDone;
    private Exception mException;
    private int mVideoIndex;
    private IVideoEncodeThread mVideoEncodeThread;
    private InputSurface mInputSurface;
    private OutputSurface mOutputSurface;
    private Integer mDstFrameRate;
    private Integer mSrcFrameRate;
    private boolean mDropFrames;
    private FrameDropper mFrameDropper;

    public VideoDecodeThread(IVideoEncodeThread videoEncodeThread, MediaExtractor extractor,
                             @Nullable Integer startTimeMs, @Nullable Integer endTimeMs,
                             @Nullable Integer srcFrameRate, @Nullable Integer dstFrameRate, @Nullable Float speed,
                             boolean dropFrames,
                             int videoIndex, AtomicBoolean decodeDone

    ) {
        super("VideoProcessDecodeThread");

        Timber.d("VideoDecodeThread startTimeMs = %s, mEndTimeMs = %s, mSpeed = %s, srcFrameRate = %s, dstFrameRate = %s", mStartTimeMs,
                mEndTimeMs, mSpeed, srcFrameRate, dstFrameRate);

        mExtractor = extractor;
        mStartTimeMs = startTimeMs;
        mEndTimeMs = endTimeMs;
        mSpeed = speed;
        mVideoIndex = videoIndex;
        mDecodeDone = decodeDone;
        mVideoEncodeThread = videoEncodeThread;
        mDstFrameRate = dstFrameRate;
        mSrcFrameRate = srcFrameRate;
        mDropFrames = dropFrames;
    }

    @Override
    public void run() {
        super.run();

        try {
            doDecode();
        } catch (Exception e) {
            mException = e;
            Timber.e(e);
        } finally {
            if (mInputSurface != null) {
                mInputSurface.release();
            }

            if (mOutputSurface != null) {
                mOutputSurface.release();
            }

            try {
                if (mDecoder != null) {
                    mDecoder.stop();
                    mDecoder.release();
                }
            } catch (Exception e) {
                mException = mException == null ? e : mException;
                Timber.e(e);
            }
        }
    }

    private void doDecode() throws IOException {
        Timber.d("doDecode");

        CountDownLatch eglContextLatch = mVideoEncodeThread.getEglContextLatch();

        try {
            boolean await = eglContextLatch.await(5, TimeUnit.SECONDS);
            if (!await) {
                mException = new TimeoutException("wait eglContext timeout!");
                return;
            }
        } catch (InterruptedException e) {
            Timber.e(e);
            mException = e;
            return;
        }

        Surface encodeSurface = mVideoEncodeThread.getSurface();
        mInputSurface = new InputSurface(encodeSurface);
        mInputSurface.makeCurrent();

        MediaFormat inputFormat = mExtractor.getTrackFormat(mVideoIndex);

        // 初始化解码器
        mDecoder = MediaCodec.createDecoderByType(inputFormat.getString(MediaFormat.KEY_MIME));
        mOutputSurface = new OutputSurface();
        mDecoder.configure(inputFormat, mOutputSurface.getSurface(), null, 0);
        mDecoder.start();

        // 丢帧判断
        int frameIndex = 0;

        if (mDropFrames && mSrcFrameRate != null && mDstFrameRate != null) {
            if (mSpeed != null) {
                mSrcFrameRate = (int) (mSrcFrameRate * mSpeed);
            }

            if (mSrcFrameRate > mDstFrameRate) {
                mFrameDropper = new FrameDropper(mSrcFrameRate, mDstFrameRate);
                Timber.w("帧率过高，需要丢帧:" + mSrcFrameRate + "->" + mDstFrameRate);
            }
        }

        // 开始解码
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

        boolean decoderDone = false;
        boolean inputDone = false;

        long videoStartTimeUs = -1;
        int decodeTryAgainCount = 0;

        while (!decoderDone) {
            //还有帧数据，输入解码器
            if (!inputDone) {
                boolean eof = false;
                int index = mExtractor.getSampleTrackIndex();
                if (index == mVideoIndex) {
                    int inputBufIndex = mDecoder.dequeueInputBuffer(TIMEOUT_USEC);
                    if (inputBufIndex >= 0) {
                        ByteBuffer inputBuf = mDecoder.getInputBuffer(inputBufIndex);
                        int chunkSize = mExtractor.readSampleData(inputBuf, 0);
                        if (chunkSize < 0) {
                            mDecoder.queueInputBuffer(inputBufIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            decoderDone = true;
                        } else {
                            long sampleTime = mExtractor.getSampleTime();
                            mDecoder.queueInputBuffer(inputBufIndex, 0, chunkSize, sampleTime, 0);
                            mExtractor.advance();
                        }
                    }
                } else if (index == -1) {
                    eof = true;
                }

                if (eof) {
                    // 解码输入结束
                    Timber.i("inputDone");

                    int inputBufIndex = mDecoder.dequeueInputBuffer(TIMEOUT_USEC);
                    if (inputBufIndex >= 0) {
                        mDecoder.queueInputBuffer(inputBufIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        inputDone = true;
                    }
                }
            }

            boolean decoderOutputAvailable = !decoderDone;
            if (decoderDone) {
                Timber.i("decoderOutputAvailable:" + decoderOutputAvailable);
            }

            while (decoderOutputAvailable) {
                int outputBufferIndex = mDecoder.dequeueOutputBuffer(info, TIMEOUT_USEC);

                Timber.i("outputBufferIndex = " + outputBufferIndex);

                if (inputDone && outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    decodeTryAgainCount++;
                    if (decodeTryAgainCount > 10) {
                        //小米2上出现BUFFER_FLAG_END_OF_STREAM之后一直tryAgain的问题
                        Timber.e("INFO_TRY_AGAIN_LATER 10 times,force End!");
                        decoderDone = true;
                        break;
                    }
                } else {
                    decodeTryAgainCount = 0;
                }
                if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    break;
                } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat newFormat = mDecoder.getOutputFormat();
                    Timber.i("decode newFormat = " + newFormat);
                } else if (outputBufferIndex < 0) {
                    //ignore
                    Timber.e("unexpected result from decoder.dequeueOutputBuffer: " + outputBufferIndex);
                } else {
                    boolean doRender = true;
                    // 解码数据可用
                    if (mEndTimeMs != null && info.presentationTimeUs >= mEndTimeMs * 1000) {
                        inputDone = true;
                        decoderDone = true;
                        doRender = false;
                        info.flags |= MediaCodec.BUFFER_FLAG_END_OF_STREAM;
                    }

                    if (mStartTimeMs != null && info.presentationTimeUs < mStartTimeMs * 1000) {
                        doRender = false;
                        Timber.e("drop frame startTime = " + mStartTimeMs + " present time = " + info.presentationTimeUs / 1000);
                    }

                    if (info.flags == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                        decoderDone = true;
                        mDecoder.releaseOutputBuffer(outputBufferIndex, false);
                        Timber.i("decoderDone");
                        break;
                    }

                    // 检查是否需要丢帧
                    if (mFrameDropper != null && mFrameDropper.checkDrop(frameIndex)) {
                        Timber.w("帧率过高，丢帧:" + frameIndex);
                        doRender = false;
                    }

                    frameIndex++;

                    mDecoder.releaseOutputBuffer(outputBufferIndex, doRender);

                    if (doRender) {
                        boolean errorWait = false;
                        try {
                            mOutputSurface.awaitNewImage();
                        } catch (Exception e) {
                            errorWait = true;
                            Timber.e(e.getMessage());
                        }

                        if (!errorWait) {
                            if (videoStartTimeUs == -1) {
                                videoStartTimeUs = info.presentationTimeUs;
                                Timber.i("videoStartTime:" + videoStartTimeUs / 1000);
                            }

                            mOutputSurface.drawImage(false);
                            long presentationTimeNs = (info.presentationTimeUs - videoStartTimeUs) * 1000;
                            if (mSpeed != null) {
                                presentationTimeNs /= mSpeed;
                            }

                            Timber.i("drawImage,setPresentationTimeMs:" + presentationTimeNs / 1000 / 1000);

                            mInputSurface.setPresentationTime(presentationTimeNs);
                            mInputSurface.swapBuffers();
                            break;
                        }
                    }
                }
            }
        }

        Timber.d("Video Decode Done!");

        mDecodeDone.set(true);
    }

    public Exception getException() {
        return mException;
    }
}