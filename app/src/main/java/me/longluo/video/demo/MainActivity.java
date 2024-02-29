package me.longluo.video.demo;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.jaygoo.widget.OnRangeChangedListener;
import com.jaygoo.widget.RangeSeekBar;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import me.longluo.video.VideoEffects;
import me.longluo.video.VideoProcessor;
import me.longluo.video.VideoUtil;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity {

    private static final String POSITION = "position";

    private static final String FILEPATH = "filepath";

    private static final int REQUEST_TAKE_GALLERY_VIDEO = 100;

    private VideoView videoView;

    private RangeSeekBar rangeSeekBar;

    private Runnable runnable;

    private ProgressDialog progressDialog;

    private Uri selectedVideoUri;

    private int stopPosition;
    private TextView tvLeft, tvRight;
    private String filePath;
    private int duration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Timber.d("onCreate");

        final TextView uploadVideo = findViewById(R.id.uploadVideo);
        TextView cutVideo = findViewById(R.id.cropVideo);
        TextView scaleVideo = findViewById(R.id.scaleVideo);
        TextView mixAudio = findViewById(R.id.mixAudio);
        TextView increaseSpeed = findViewById(R.id.increaseSpeed);
        TextView decreaseSpeed = findViewById(R.id.decreaseSpeed);
        final TextView reverseVideo = findViewById(R.id.reverseVideo);

        tvLeft = findViewById(R.id.tvLeft);
        tvRight = findViewById(R.id.tvRight);

        videoView = findViewById(R.id.videoView);
        rangeSeekBar = findViewById(R.id.rangeSeekBar);
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle(null);
        progressDialog.setCancelable(false);
        progressDialog.setMessage("please wait......");
        rangeSeekBar.setEnabled(false);

        uploadVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= 23) {
                    getPermission();
                } else {
                    uploadVideo();
                }
            }
        });

        cutVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Timber.d("Cut Video");

                if (selectedVideoUri != null) {
                    executeCutVideo((int) (rangeSeekBar.getLeftSeekBar().getProgress() * 1000), (int) (rangeSeekBar.getRightSeekBar().getProgress() * 1000));
                } else {
                    Toast.makeText(getApplicationContext(), "Please upload a video", Toast.LENGTH_SHORT).show();
                }
            }
        });

        scaleVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedVideoUri != null) {
                    executeScaleVideo((int) (rangeSeekBar.getLeftSeekBar().getProgress() * 1000),
                            (int) (rangeSeekBar.getRightSeekBar().getProgress() * 1000));
                } else {
                    Toast.makeText(getApplicationContext(), "Please upload a video", Toast.LENGTH_SHORT).show();
                }
            }
        });

        mixAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedVideoUri != null) {
                    executeMixAudio((int) (rangeSeekBar.getLeftSeekBar().getProgress() * 1000), (int) (rangeSeekBar.getRightSeekBar().getProgress() * 1000));
                } else {
                    Toast.makeText(getApplicationContext(), "Please upload a video", Toast.LENGTH_SHORT).show();
                }
            }
        });

        increaseSpeed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedVideoUri != null) {
                    executeSpeedVideo((int) (rangeSeekBar.getLeftSeekBar().getProgress() * 1000), (int) (rangeSeekBar.getRightSeekBar().getProgress() * 1000),
                            2f);
                } else {
                    Toast.makeText(getApplicationContext(), "Please upload a video", Toast.LENGTH_SHORT).show();
                }
            }
        });

        decreaseSpeed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedVideoUri != null) {
                    executeSpeedVideo((int) (rangeSeekBar.getLeftSeekBar().getProgress() * 1000), (int) (rangeSeekBar.getRightSeekBar().getProgress() * 1000),
                            0.5f);
                } else {
                    Toast.makeText(getApplicationContext(), "Please upload a video", Toast.LENGTH_SHORT).show();
                }
            }
        });

        reverseVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedVideoUri != null) {
                    executeRevertVideo((int) (rangeSeekBar.getLeftSeekBar().getProgress() * 1000), (int) (rangeSeekBar.getRightSeekBar().getProgress() * 1000));
                } else {
                    Toast.makeText(getApplicationContext(), "Please upload a video", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void getPermission() {
        String[] params = null;

        String writeExternalStorage = Manifest.permission.WRITE_EXTERNAL_STORAGE;
        String readExternalStorage = Manifest.permission.READ_EXTERNAL_STORAGE;

        int hasWriteExternalStoragePermission = ActivityCompat.checkSelfPermission(this, writeExternalStorage);
        int hasReadExternalStoragePermission = ActivityCompat.checkSelfPermission(this, readExternalStorage);

        List<String> permissions = new ArrayList<String>();

        if (hasWriteExternalStoragePermission != PackageManager.PERMISSION_GRANTED) {
            permissions.add(writeExternalStorage);
        }

        if (hasReadExternalStoragePermission != PackageManager.PERMISSION_GRANTED) {
            permissions.add(readExternalStorage);
        }

        if (!permissions.isEmpty()) {
            params = permissions.toArray(new String[permissions.size()]);
        }

        if (params != null && params.length > 0) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    params,
                    100);
        } else {
            uploadVideo();
        }
    }

    /**
     * Handling response for permission request
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 100: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    uploadVideo();
                }
            }
            break;

            default:
                break;
        }
    }

    /**
     * Opening gallery for uploading video
     */
    private void uploadVideo() {
        try {
            Intent intent = new Intent();
            intent.setType("video/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Video"), REQUEST_TAKE_GALLERY_VIDEO);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopPosition = videoView.getCurrentPosition(); //stopPosition is an int
        videoView.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        videoView.seekTo(stopPosition);
        videoView.start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_TAKE_GALLERY_VIDEO) {
                selectedVideoUri = data.getData();
                ViewGroup.LayoutParams layoutParams = videoView.getLayoutParams();
                try {
                    Pair<Integer, Integer> size = VideoUtil.getVideoSize(new VideoProcessor.MediaSource(this, selectedVideoUri));
                    layoutParams.height = getResources().getDimensionPixelSize(R.dimen.video_height);
                    layoutParams.width = (int) (size.first * (layoutParams.height / (float) size.second));
                } catch (IOException e) {
                    e.printStackTrace();
                    layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
                    layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
                }

                videoView.setLayoutParams(layoutParams);

                videoView.setVideoURI(selectedVideoUri);
                videoView.start();

                videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {

                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        duration = mp.getDuration() / 1000;
                        tvLeft.setText("00:00:00");

                        tvRight.setText(getTime(mp.getDuration() / 1000));
                        mp.setLooping(true);
                        rangeSeekBar.setRange(0, duration);
                        rangeSeekBar.setProgress(0);
                        rangeSeekBar.setEnabled(true);
                        rangeSeekBar.requestLayout();

                        rangeSeekBar.setOnRangeChangedListener(new OnRangeChangedListener() {
                            @Override
                            public void onRangeChanged(RangeSeekBar view, float leftValue, float rightValue, boolean isFromUser) {
                                videoView.seekTo((int) leftValue * 1000);

                                tvLeft.setText(getTime((int) view.getLeftSeekBar().getProgress()));

                                tvRight.setText(getTime((int) view.getRightSeekBar().getProgress()));
                            }

                            @Override
                            public void onStartTrackingTouch(RangeSeekBar view, boolean isLeft) {

                            }

                            @Override
                            public void onStopTrackingTouch(RangeSeekBar view, boolean isLeft) {

                            }
                        });

                        final Handler handler = new Handler();
                        handler.postDelayed(runnable = new Runnable() {
                            @Override
                            public void run() {
                                if (videoView.getCurrentPosition() >= rangeSeekBar.getLeftSeekBar().getProgress() * 1000) {
                                    videoView.seekTo((int) rangeSeekBar.getRightSeekBar().getProgress() * 1000);
                                }
                                handler.postDelayed(runnable, 1000);
                            }
                        }, 1000);
                    }
                });
            }
        }
    }

    private String getTime(int seconds) {
        int hr = seconds / 3600;
        int rem = seconds % 3600;
        int mn = rem / 60;
        int sec = rem % 60;
        return String.format("%02d", hr) + ":" + String.format("%02d", mn) + ":" + String.format("%02d", sec);
    }

    private void executeCutVideo(final int startMs, final int endMs) {
        progressDialog.show();

        File moviesDir = getTempMovieDir();
        String filePrefix = "cut_video";
        String fileExtn = ".mp4";
        File dest = new File(moviesDir, filePrefix + fileExtn);
        int fileNo = 0;

        while (dest.exists()) {
            fileNo++;
            dest = new File(moviesDir, filePrefix + fileNo + fileExtn);
        }

        filePath = dest.getAbsolutePath();

        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean success = true;
                try {
                    VideoProcessor.cutVideo(getApplicationContext(), selectedVideoUri, filePath, startMs, endMs);
                } catch (Exception e) {
                    success = false;
                    e.printStackTrace();
                    postError();
                }
                if (success) {
                    startPreviewActivity(filePath);
                }
                progressDialog.dismiss();
            }
        }).start();
    }

    private void executeScaleVideo(final int startMs, final int endMs) {
        File moviesDir = getTempMovieDir();
        progressDialog.show();
        String filePrefix = "scale_video";
        String fileExtn = ".mp4";
        File dest = new File(moviesDir, filePrefix + fileExtn);
        int fileNo = 0;
        while (dest.exists()) {
            fileNo++;
            dest = new File(moviesDir, filePrefix + fileNo + fileExtn);
        }
        filePath = dest.getAbsolutePath();

        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean success = true;
                try {
                    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                    retriever.setDataSource(MainActivity.this, selectedVideoUri);
                    int originWidth = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
                    int originHeight = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
                    int bitrate = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE));

                    int outWidth = originWidth / 2;
                    int outHeight = originHeight / 2;
                    VideoProcessor.processor(getApplicationContext())
                            .input(selectedVideoUri)
                            .output(filePath)
                            .outWidth(outWidth)
                            .outHeight(outHeight)
                            .startTimeMs(startMs)
                            .endTimeMs(endMs)
                            .bitrate(bitrate / 2)
                            .process();
                } catch (Exception e) {
                    success = false;
                    e.printStackTrace();
                    postError();
                }
                if (success) {
                    startPreviewActivity(filePath);
                }
                progressDialog.dismiss();
            }
        }).start();
    }

    private void executeMixAudio(final int startMs, final int endMs) {
        File moviesDir = getTempMovieDir();
        progressDialog.show();
        String filePrefix = "scale_video";
        String fileExtn = ".mp4";
        final VideoProcessor.MediaSource selectVideo = new VideoProcessor.MediaSource(MainActivity.this, selectedVideoUri);
        File dest = new File(moviesDir, filePrefix + fileExtn);
        int fileNo = 0;
        while (dest.exists()) {
            fileNo++;
            dest = new File(moviesDir, filePrefix + fileNo + fileExtn);
        }
        filePath = dest.getAbsolutePath();

        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean success = true;
                try {
                    final String aacPath = new File(getCacheDir(), "test.aac").getAbsolutePath();
                    copyAssets("test.aac", aacPath);
                    VideoProcessor.mixAudioTrack(getApplicationContext(), selectVideo, new VideoProcessor.MediaSource(aacPath), filePath, startMs, endMs, 100, 100,
                            1, 1);
                } catch (Exception e) {
                    success = false;
                    e.printStackTrace();
                    postError();
                }
                if (success) {
                    startPreviewActivity(filePath);
                }
                progressDialog.dismiss();
            }
        }).start();
    }

    private void executeKichikuVideo() {
        File moviesDir = getTempMovieDir();
        progressDialog.show();
        String filePrefix = "kichiku_video";
        String fileExtn = ".mp4";
        File dest = new File(moviesDir, filePrefix + fileExtn);
        int fileNo = 0;
        while (dest.exists()) {
            fileNo++;
            dest = new File(moviesDir, filePrefix + fileNo + fileExtn);
        }
        filePath = dest.getAbsolutePath();

        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean success = true;
                try {
                    VideoEffects.doKichiku(getApplicationContext(), new VideoProcessor.MediaSource(MainActivity.this, selectedVideoUri), filePath, null, 2, 2000);
                } catch (Exception e) {
                    success = false;
                    e.printStackTrace();
                    postError();
                }
                if (success) {
                    startPreviewActivity(filePath);
                }
                progressDialog.dismiss();
            }
        }).start();
    }

    private void executeSpeedVideo(final int startMs, final int endMs, final float speed) {
        File moviesDir = getTempMovieDir();
        progressDialog.show();
        String filePrefix = "speed_video";
        String fileExtn = ".mp4";
        File dest = new File(moviesDir, filePrefix + fileExtn);
        int fileNo = 0;
        while (dest.exists()) {
            fileNo++;
            dest = new File(moviesDir, filePrefix + fileNo + fileExtn);
        }
        filePath = dest.getAbsolutePath();

        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean success = true;
                try {
                    long s = System.currentTimeMillis();
                    VideoProcessor.processor(getApplicationContext())
                            .input(selectedVideoUri)
                            .output(filePath)
                            .startTimeMs(startMs)
                            .endTimeMs(endMs)
                            .speed(speed)
                            .changeAudioSpeed(true)
                            .process();
                    long e = System.currentTimeMillis();
                    Timber.w("减速已完成，耗时:" + (e - s) / 1000f + "s");
                } catch (Exception e) {
                    success = false;
                    e.printStackTrace();
                    postError();
                }
                if (success) {
                    startPreviewActivity(filePath);
                }
                progressDialog.dismiss();
            }
        }).start();
    }

    private void executeRevertVideo(final int startMs, final int endMs) {
        File moviesDir = getTempMovieDir();
        progressDialog.show();
        String filePrefix = "revert_video";
        String fileExtn = ".mp4";
        File dest = new File(moviesDir, filePrefix + fileExtn);
        int fileNo = 0;
        while (dest.exists()) {
            fileNo++;
            dest = new File(moviesDir, filePrefix + fileNo + fileExtn);
        }
        filePath = dest.getAbsolutePath();

        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean success = true;
                try {
                    VideoProcessor.reverseVideo(getApplicationContext(), new VideoProcessor.MediaSource(MainActivity.this, selectedVideoUri), filePath, true, null);
                } catch (Exception e) {
                    success = false;
                    e.printStackTrace();
                    postError();
                }
                if (success) {
                    startPreviewActivity(filePath);
                }
                progressDialog.dismiss();
            }
        }).start();
    }

    private void startPreviewActivity(String videoPath) {
        String name = new File(videoPath).getName();
        int end = name.lastIndexOf('.');
        if (end > 0) {
            name = name.substring(0, end);
        }
        String strUri = VideoUtil.savaVideoToMediaStore(this, videoPath, name, "From VideoProcessor", "video/mp4");
        Uri uri = Uri.parse(strUri);
        Intent intent = new Intent(this, PreviewActivity.class);
        intent.putExtra(PreviewActivity.KEY_URI, uri);
        startActivity(intent);
    }

    private File getTempMovieDir() {
        File movie = new File(getCacheDir(), "movie");
        movie.mkdirs();
        return movie;
    }

    private void copyAssets(String assetsName, String path) throws IOException {
        AssetFileDescriptor assetFileDescriptor = getAssets().openFd(assetsName);
        FileChannel from = new FileInputStream(assetFileDescriptor.getFileDescriptor()).getChannel();
        FileChannel to = new FileOutputStream(path).getChannel();
        from.transferTo(assetFileDescriptor.getStartOffset(), assetFileDescriptor.getLength(), to);
    }

    private void postError() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), "process error!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
