package me.longluo.video.demo;

import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

import me.longluo.video.VideoProcessor;
import me.longluo.video.VideoUtil;
import me.longluo.video.demo.util.PathUtils;


public class PreviewActivity extends AppCompatActivity {

    private VideoView videoView;

    private SeekBar seekBar;

    private int stopPosition;

    static final String KEY_URI = "uri";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        videoView = findViewById(R.id.videoView);
        seekBar = findViewById(R.id.seekBar);

        TextView tvInstruction = findViewById(R.id.tvInstruction);
        Uri uri = getIntent().getParcelableExtra(KEY_URI);

        String videoInfo = "";

        try {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(this, uri);
            int bitrate = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE));
            retriever.release();
            MediaExtractor extractor = new MediaExtractor();
            extractor.setDataSource(this, uri, null);
            MediaFormat format = extractor.getTrackFormat(VideoUtil.selectTrack(extractor, false));
            int frameRate = format.containsKey(MediaFormat.KEY_FRAME_RATE) ? format.getInteger(MediaFormat.KEY_FRAME_RATE) : -1;
            float aveFrameRate = VideoUtil.getAveFrameRate(new VideoProcessor.MediaSource(this, uri));
            int width = format.getInteger(MediaFormat.KEY_WIDTH);
            int height = format.getInteger(MediaFormat.KEY_HEIGHT);
            int rotation = format.containsKey(MediaFormat.KEY_ROTATION) ? format.getInteger(MediaFormat.KEY_ROTATION) : -1;
            long duration = format.containsKey(MediaFormat.KEY_DURATION) ? format.getLong(MediaFormat.KEY_DURATION) : -1;
            videoInfo = String.format(Locale.ENGLISH, "size:%dX%d,framerate:%d,aveFrameRate:%f,rotation:%d,bitrate:%d,duration:%.1fs", width, height, frameRate, aveFrameRate, rotation, bitrate,
                    duration / 1000f / 1000f);
            extractor.release();
        } catch (Exception e) {
            e.printStackTrace();
        }

        tvInstruction.setText(String.format(Locale.ENGLISH, "Video stored at \nUri %s\nPath:%s\n%s", uri, PathUtils.getPath(this, uri), videoInfo));
        videoView.setVideoURI(uri);
        videoView.start();

        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {

            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.setLooping(true);
                seekBar.setMax(videoView.getDuration());
                seekBar.postDelayed(onEverySecond, 1000);
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                if (fromUser) {
                    // this is when actually seekbar has been seeked to a new position
                    videoView.seekTo(progress);
                }
            }
        });
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

    private Runnable onEverySecond = new Runnable() {

        @Override
        public void run() {

            if (seekBar != null) {
                seekBar.setProgress(videoView.getCurrentPosition());
            }

            if (videoView.isPlaying()) {
                seekBar.postDelayed(onEverySecond, 1000);
            }
        }
    };

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle arrow click here
        if (item.getItemId() == android.R.id.home) {
            finish(); // close this activity and return to preview activity (if there is any)
        }

        return super.onOptionsItemSelected(item);
    }
}
