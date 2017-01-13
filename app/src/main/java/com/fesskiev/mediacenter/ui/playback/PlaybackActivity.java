package com.fesskiev.mediacenter.ui.playback;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.fesskiev.mediacenter.MediaApplication;
import com.fesskiev.mediacenter.R;
import com.fesskiev.mediacenter.analytics.AnalyticsActivity;
import com.fesskiev.mediacenter.data.model.AudioFile;
import com.fesskiev.mediacenter.players.AudioPlayer;
import com.fesskiev.mediacenter.services.PlaybackService;
import com.fesskiev.mediacenter.ui.audio.player.AudioPlayerActivity;
import com.fesskiev.mediacenter.utils.AnimationUtils;
import com.fesskiev.mediacenter.utils.AppSettingsManager;
import com.fesskiev.mediacenter.utils.AudioNotificationHelper;
import com.fesskiev.mediacenter.utils.BitmapHelper;
import com.fesskiev.mediacenter.utils.Utils;
import com.fesskiev.mediacenter.widgets.buttons.PlayPauseFloatingButton;
import com.fesskiev.mediacenter.widgets.nav.MediaNavigationView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;


public abstract class PlaybackActivity extends AnalyticsActivity {

    public abstract MediaNavigationView getMediaNavigationView();

    private AudioPlayer audioPlayer;
    private AudioFile currentTrack;

    private BottomSheetBehavior bottomSheetBehavior;
    private TrackListAdapter adapter;
    private PlayPauseFloatingButton playPauseButton;
    private TextView durationText;
    private TextView track;
    private TextView artist;
    private ImageView cover;
    private View emptyFolder;
    private View emptyTrack;
    private View peakView;
    private Bitmap lastCover;
    private int height;
    private boolean isShow = true;

    private boolean lastPlaying;
    private int lastPositionSeconds;
    private boolean lastEnableEQ;
    private boolean lastEnableReverb;

    @Override
    public void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        EventBus.getDefault().register(this);

        audioPlayer = MediaApplication.getInstance().getAudioPlayer();
        audioPlayer.getCurrentTrackAndTrackList();

        track = (TextView) findViewById(R.id.track);
        artist = (TextView) findViewById(R.id.artist);
        cover = (ImageView) findViewById(R.id.cover);
        durationText = (TextView) findViewById(R.id.duration);

        emptyTrack = findViewById(R.id.emptyTrackCard);
        emptyFolder = findViewById(R.id.emptyFolderCard);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.trackListControl);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TrackListAdapter();
        recyclerView.setAdapter(adapter);

        playPauseButton = (PlayPauseFloatingButton) findViewById(R.id.playPauseFAB);
        playPauseButton.setOnClickListener(v -> {
            if (checkTrackSelected()) {
                if (lastPlaying) {
                    audioPlayer.pause();
                } else {
                    audioPlayer.play();
                }
                togglePlayPause();
            }
        });
        playPauseButton.setPlay(false);

        View bottomSheet = findViewById(R.id.bottom_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_SETTLING);
        if (bottomSheetBehavior != null) {
            bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
                @Override
                public void onStateChanged(View bottomSheet, int newState) {
                    switch (newState) {
                        case BottomSheetBehavior.STATE_HIDDEN:
                            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                            break;
                    }
                }

                @Override
                public void onSlide(View bottomSheet, float slideOffset) {

                }
            });

            peakView = findViewById(R.id.basicNavPlayerContainer);
            peakView.setOnClickListener(v -> AudioPlayerActivity.startPlayerActivity(PlaybackActivity.this));
            peakView.post(() -> {
                int marginTop = getResources().getDimensionPixelSize(R.dimen.bottom_sheet_margin_top);
                height = peakView.getHeight() + marginTop;
                bottomSheetBehavior.setPeekHeight(height);
            });
        }

        showEmptyFolderCard();
        showEmptyTrackCard();


        registerNotificationReceiver();

    }

    private boolean checkTrackSelected() {
        if (currentTrack == null) {
            AnimationUtils.getInstance().errorAnimation(playPauseButton);
            return false;
        }
        return true;
    }

    private void togglePlayPause() {
        lastPlaying = !lastPlaying;
        playPauseButton.setPlay(lastPlaying);

        AudioNotificationHelper.getInstance(getApplicationContext())
                .updateNotification(currentTrack, lastCover, lastPositionSeconds, lastPlaying);

        adapter.notifyDataSetChanged();
    }

    private void registerNotificationReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(AudioNotificationHelper.ACTION_MEDIA_CONTROL_PLAY);
        filter.addAction(AudioNotificationHelper.ACTION_MEDIA_CONTROL_PAUSE);
        filter.addAction(AudioNotificationHelper.ACTION_MEDIA_CONTROL_NEXT);
        filter.addAction(AudioNotificationHelper.ACTION_MEDIA_CONTROL_PREVIOUS);
        registerReceiver(notificationReceiver, filter);

    }

    private void unregisterNotificationReceiver() {
        unregisterReceiver(notificationReceiver);
    }

    private BroadcastReceiver notificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d("test", "NOTIFICATION INTENT :" + action);
            switch (action) {
                case AudioNotificationHelper.ACTION_MEDIA_CONTROL_PLAY:
                    audioPlayer.play();
                    break;
                case AudioNotificationHelper.ACTION_MEDIA_CONTROL_PAUSE:
                    audioPlayer.pause();
                    break;
                case AudioNotificationHelper.ACTION_MEDIA_CONTROL_PREVIOUS:
                    audioPlayer.previous();
                    break;
                case AudioNotificationHelper.ACTION_MEDIA_CONTROL_NEXT:
                    audioPlayer.next();
                    break;
            }
        }
    };


    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        unregisterNotificationReceiver();

        AudioNotificationHelper.getInstance(getApplicationContext()).stopNotification();
        PlaybackService.stopPlaybackForegroundService(getApplicationContext());
    }

    @Override
    public String getActivityName() {
        return this.getLocalClassName();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPlaybackStateEvent(PlaybackService playbackState) {

        boolean playing = playbackState.isPlaying();
        if (lastPlaying != playing) {
            lastPlaying = playing;
            playPauseButton.setPlay(playing);

            AudioNotificationHelper.getInstance(getApplicationContext())
                    .updateNotification(currentTrack, lastCover, lastPositionSeconds, lastPlaying);

            adapter.notifyDataSetChanged();

        }

        int positionSeconds = playbackState.getPosition();
        if (lastPositionSeconds != positionSeconds) {
            lastPositionSeconds = positionSeconds;
            durationText.setText(Utils.getPositionSecondsString(lastPositionSeconds));
        }

        boolean enableEq = playbackState.isEnableEQ();
        if (lastEnableEQ != enableEq) {
            lastEnableEQ = enableEq;
            AppSettingsManager.getInstance().setEQEnable(enableEq);
            getMediaNavigationView().setEQEnable(lastEnableEQ);

            Log.d("eqtest", "EQ STATE CHANGE: " + lastEnableEQ);
        }

        boolean enableReverb = playbackState.isEnableReverb();
        if (lastEnableReverb != enableReverb) {
            lastEnableReverb = enableReverb;
            AppSettingsManager.getInstance().setReverbEnable(enableReverb);
            getMediaNavigationView().setReverbEnable(lastEnableReverb);

            Log.d("eqtest", "Reverb STATE CHANGE: " + lastEnableReverb);
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCurrentTrackEvent(AudioFile currentTrack) {
        this.currentTrack = currentTrack;

        Log.wtf("test", "PLAYBACK onCurrentTrackEvent: " + currentTrack.toString());

        setMusicFileInfo(currentTrack);
        hideEmptyTrackCard();

        adapter.notifyDataSetChanged();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCurrentTrackListEvent(List<AudioFile> currentTrackList) {
        Log.wtf("test", "PLAYBACK onCurrentTrackListEvent");

        adapter.refreshAdapter(currentTrackList);
        hideEmptyFolderCard();

        adapter.notifyDataSetChanged();
    }

    private void setMusicFileInfo(AudioFile audioFile) {
        track.setText(audioFile.title);
        artist.setText(audioFile.artist);

        audioPlayer.getCurrentAudioFolder()
                .first()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(audioFolder -> {
                    BitmapHelper.getInstance().loadArtwork(audioFile, audioFolder, cover,
                            new BitmapHelper.OnBitmapLoadListener() {
                                @Override
                                public void onLoaded(Bitmap bitmap) {

                                    lastCover = bitmap;

                                    AudioNotificationHelper.getInstance(getApplicationContext())
                                            .updateNotification(audioFile, lastCover, 0, lastPlaying);

                                    PlaybackService.startPlaybackForegroundService(getApplicationContext());
                                }

                                @Override
                                public void onFailed() {

                                }
                            });
                });


    }

    public void showPlayback() {
        if (!isShow) {
            bottomSheetBehavior.setPeekHeight(height);
            isShow = true;
        }
    }

    public void hidePlayback() {
        if (isShow) {
            bottomSheetBehavior.setPeekHeight(0);
            isShow = false;
        }
    }

    private class TrackListAdapter extends RecyclerView.Adapter<TrackListAdapter.ViewHolder> {

        private List<AudioFile> audioFiles;

        public TrackListAdapter() {
            audioFiles = new ArrayList<>();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {

            ImageView playEq;
            TextView title;
            TextView duration;
            TextView filePath;

            public ViewHolder(View v) {
                super(v);
                v.setOnClickListener(view -> {
                    List<AudioFile> audioFiles = adapter.getAudioFiles();
                    AudioFile audioFile = audioFiles.get(getAdapterPosition());
                    if (audioFile != null) {
                        audioPlayer.setCurrentAudioFileAndPlay(audioFile);
                    }
                });

                playEq = (ImageView) v.findViewById(R.id.playEq);
                title = (TextView) v.findViewById(R.id.title);
                duration = (TextView) v.findViewById(R.id.duration);
                filePath = (TextView) v.findViewById(R.id.filePath);
                filePath.setSelected(true);

            }
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_track_playback, parent, false);

            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            AudioFile audioFile = audioFiles.get(position);
            if (audioFile != null) {
                holder.title.setText(audioFile.title);
                holder.filePath.setText(audioFile.getFilePath());
                holder.duration.setText(Utils.getDurationString(audioFile.length));

                if (currentTrack != null) {
                    if (currentTrack.equals(audioFile) && lastPlaying) {
                        holder.playEq.setVisibility(View.VISIBLE);

                        AnimationDrawable animation = (AnimationDrawable) ContextCompat.
                                getDrawable(getApplicationContext(), R.drawable.ic_equalizer);
                        holder.playEq.setImageDrawable(animation);
                        if (animation != null) {
                            if (lastPlaying) {
                                animation.start();
                            } else {
                                animation.stop();
                            }
                        }
                    } else {
                        holder.playEq.setVisibility(View.INVISIBLE);
                    }
                }
            }
        }

        @Override
        public int getItemCount() {
            return audioFiles.size();
        }


        public List<AudioFile> getAudioFiles() {
            return audioFiles;
        }

        public void refreshAdapter(List<AudioFile> receiverAudioFiles) {
            audioFiles.clear();
            audioFiles.addAll(receiverAudioFiles);
            notifyDataSetChanged();
        }
    }

    private void showEmptyFolderCard() {
        emptyFolder.setVisibility(View.VISIBLE);
    }

    private void showEmptyTrackCard() {
        emptyTrack.setVisibility(View.VISIBLE);
    }

    private void hideEmptyFolderCard() {
        emptyFolder.setVisibility(View.GONE);
    }

    private void hideEmptyTrackCard() {
        emptyTrack.setVisibility(View.GONE);
    }
}