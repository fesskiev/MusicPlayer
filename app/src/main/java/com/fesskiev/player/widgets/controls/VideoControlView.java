package com.fesskiev.player.widgets.controls;


import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.fesskiev.player.R;
import com.fesskiev.player.widgets.buttons.PlayPauseButton;

public class VideoControlView extends FrameLayout implements View.OnClickListener {


    public interface OnVideoPlayerControlListener {

        void playPauseButtonClick(boolean isPlaying);

        void seekVideo(int progress);

        void nextVideo();

        void previousVideo();
    }

    private OnVideoPlayerControlListener listener;
    private PlayPauseButton playPauseButton;
    private SeekBar seekVideo;
    private TextView videoTimeCount;
    private TextView videoTimeTotal;
    private boolean isPlaying;

    public VideoControlView(Context context) {
        super(context);
        init(context);
    }

    public VideoControlView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public VideoControlView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.video_player_control, this, true);

        videoTimeCount = (TextView) findViewById(R.id.videoTimeCount);
        videoTimeTotal = (TextView) findViewById(R.id.videoTimeTotal);
        findViewById(R.id.nextVideo).setOnClickListener(this);
        findViewById(R.id.previousVideo).setOnClickListener(this);
        playPauseButton = (PlayPauseButton) view.findViewById(R.id.playPauseButton);
        playPauseButton.setColor(ContextCompat.getColor(context, R.color.primary));
        playPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPauseButton.setPlay(isPlaying);
                isPlaying = !isPlaying;
                if (listener != null) {
                    listener.playPauseButtonClick(isPlaying);
                }

            }
        });

        seekVideo = (SeekBar) findViewById(R.id.seekVideo);
        seekVideo.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progress;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                this.progress = progress;

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {


            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (listener != null) {
                    listener.seekVideo(progress);
                }
            }
        });

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.nextVideo:
                if (listener != null) {
                    listener.nextVideo();
                }
                break;
            case R.id.previousVideo:
                if (listener != null) {
                    listener.previousVideo();
                }
                break;
        }
    }

    public void setOnVideoPlayerControlListener(OnVideoPlayerControlListener l) {
        this.listener = l;
    }

    public void setVideoTimeTotal(String time) {
        videoTimeTotal.setText(time);
    }

    public void setVideoTimeCount(String time) {
        videoTimeCount.setText(time);
    }

    public void setProgress(int progress) {
        seekVideo.setProgress(progress);
    }


    public void resetIndicators() {
        videoTimeTotal.setText("0:00");
        videoTimeCount.setText("0:00");
        seekVideo.setProgress(0);
    }
}
