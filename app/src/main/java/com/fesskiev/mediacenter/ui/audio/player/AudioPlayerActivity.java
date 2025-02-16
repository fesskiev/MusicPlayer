package com.fesskiev.mediacenter.ui.audio.player;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Animatable;
import android.os.Bundle;
import android.support.design.widget.BaseTransientBottomBar;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.fesskiev.mediacenter.MediaApplication;
import com.fesskiev.mediacenter.R;
import com.fesskiev.mediacenter.analytics.AnalyticsActivity;
import com.fesskiev.mediacenter.data.model.AudioFile;
import com.fesskiev.mediacenter.players.AudioPlayer;
import com.fesskiev.mediacenter.services.PlaybackService;
import com.fesskiev.mediacenter.ui.audio.tracklist.PlayerTrackListActivity;
import com.fesskiev.mediacenter.ui.cue.CueActivity;
import com.fesskiev.mediacenter.ui.cut.CutMediaActivity;
import com.fesskiev.mediacenter.ui.effects.EffectsActivity;
import com.fesskiev.mediacenter.utils.AppGuide;
import com.fesskiev.mediacenter.utils.AppSettingsManager;
import com.fesskiev.mediacenter.utils.BitmapHelper;
import com.fesskiev.mediacenter.utils.RxUtils;
import com.fesskiev.mediacenter.utils.Utils;
import com.fesskiev.mediacenter.utils.ffmpeg.FFmpegHelper;
import com.fesskiev.mediacenter.widgets.buttons.MuteSoloButton;
import com.fesskiev.mediacenter.widgets.buttons.RepeatButton;
import com.fesskiev.mediacenter.widgets.cards.DescriptionCardView;
import com.fesskiev.mediacenter.widgets.controls.AudioControlView;
import com.fesskiev.mediacenter.widgets.dialogs.LoopingDialog;
import com.fesskiev.mediacenter.widgets.utils.DisabledScrollView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class AudioPlayerActivity extends AnalyticsActivity {

    private AudioPlayer audioPlayer;
    private AppSettingsManager settingsManager;
    private Disposable subscription;

    private AppGuide appGuide;

    private AudioControlView controlView;
    private DescriptionCardView cardDescription;
    private MuteSoloButton muteSoloButton;
    private RepeatButton repeatButton;
    private TextView trackTimeCount;
    private TextView trackTimeTotal;
    private TextView artist;
    private TextView volumeLevel;
    private TextView title;
    private TextView genre;
    private TextView album;
    private TextView trackDescription;
    private ImageView prevTrack;
    private ImageView nextTrack;
    private ImageView backdrop;
    private ImageView equalizer;
    private ImageView trackList;
    private ImageView timer;


    private boolean lastConvertStart;
    private boolean lastLoadSuccess;
    private boolean lastLoadError;
    private boolean lastPlaying;
    private boolean lastLooping;
    private int lastPositionSeconds = -1;
    private int lastDurationSeconds = -1;
    private float lastVolume = -1f;

    public static void startPlayerActivity(Activity activity) {
        activity.startActivity(new Intent(activity, AudioPlayerActivity.class));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_player);

        settingsManager = AppSettingsManager.getInstance();
        audioPlayer = MediaApplication.getInstance().getAudioPlayer();

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        timer = findViewById(R.id.timerImage);
        backdrop = findViewById(R.id.backdrop);
        muteSoloButton = findViewById(R.id.muteSoloButton);
        trackTimeTotal = findViewById(R.id.trackTimeTotal);
        trackTimeCount = findViewById(R.id.trackTimeCount);
        artist = findViewById(R.id.trackArtist);
        title = findViewById(R.id.trackTitle);
        trackDescription = findViewById(R.id.trackDescription);
        genre = findViewById(R.id.genre);
        album = findViewById(R.id.album);
        volumeLevel = findViewById(R.id.volumeLevel);

        prevTrack = findViewById(R.id.previousTrack);
        prevTrack.setOnClickListener(v -> {
            ((Animatable) prevTrack.getDrawable()).start();
            previous();
        });

        nextTrack = findViewById(R.id.nextTrack);
        nextTrack.setOnClickListener(v -> {
            ((Animatable) nextTrack.getDrawable()).start();
            next();
        });

        equalizer = findViewById(R.id.equalizer);
        equalizer.setOnClickListener(v -> startEqualizerActivity());
        trackList = findViewById(R.id.trackList);
        trackList.setOnClickListener(v -> openTrackList());

        cardDescription = findViewById(R.id.cardDescription);
        cardDescription.setOnCardAnimationListener(new DescriptionCardView.OnCardAnimationListener() {
            @Override
            public void animationStart() {

            }

            @Override
            public void animationEnd() {

            }
        });

        muteSoloButton.setOnMuteSoloListener(mute -> {
            if (mute) {
                lastVolume = 0;
                disableChangeVolume();
            } else {
                enableChangeVolume();
            }
            setVolumeLevel(lastVolume);
            PlaybackService.volumePlayback(getApplicationContext(), 0);
        });

        repeatButton = findViewById(R.id.repeatButton);
        repeatButton.setOnRepeatStateChangedListener(new RepeatButton.OnRepeatStateChangedListener() {
            @Override
            public void onRepeatStateChanged(boolean repeat) {
                PlaybackService.changeLoopingState(getApplicationContext(), repeat);
            }

            @Override
            public void onLoopingBetweenClick() {
                makeLoopingDialog();
            }
        });

        final DisabledScrollView scrollView = findViewById(R.id.scrollView);

        controlView = findViewById(R.id.audioControl);
        controlView.setOnAudioControlListener(new AudioControlView.OnAudioControlListener() {
            @Override
            public void onPlayStateChanged() {
                if (!lastConvertStart) {
                    if (lastPlaying) {
                        pause();
                    } else {
                        play();
                    }
                    lastPlaying = !lastPlaying;
                    controlView.setPlay(lastPlaying);
                }
            }

            @Override
            public void onVolumeStateChanged(int volume, boolean change) {
                if (change) {
                    PlaybackService.volumePlayback(getApplicationContext(), volume);
                }
                scrollView.setEnableScrolling(!change);
            }

            @Override
            public void onSeekStateChanged(int seek, boolean change) {
                if (change) {
                    PlaybackService.seekPlayback(getApplicationContext(), seek);
                }
                scrollView.setEnableScrolling(!change);
            }
        });

        controlView.setPlay(lastPlaying);
        setAudioTrackValues(audioPlayer.getCurrentTrack());

        EventBus.getDefault().register(this);

        PlaybackService.requestPlaybackStateIfNeed(getApplicationContext());

        checkFirstOrLastTrack();
    }


    @Override
    protected void onStart() {
        super.onStart();
        controlView.postDelayed(this::makeGuideIfNeed, 1000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (appGuide != null) {
            appGuide.clear();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        RxUtils.unsubscribe(subscription);
    }

    private void makeGuideIfNeed() {
        if (settingsManager.isNeedAudioPlayerActivityGuide()) {
            appGuide = new AppGuide(this, 4);
            appGuide.OnAppGuideListener(new AppGuide.OnAppGuideListener() {
                @Override
                public void next(int count) {
                    switch (count) {
                        case 1:
                            appGuide.makeGuide(repeatButton,
                                    getString(R.string.app_guide_looping_title),
                                    getString(R.string.app_guide_looping_desc));
                            break;
                        case 2:
                            appGuide.makeGuide(prevTrack,
                                    getString(R.string.app_guide_audio_prev_title),
                                    getString(R.string.app_guide_audio_prev_desc));
                            break;
                        case 3:
                            appGuide.makeGuide(nextTrack,
                                    getString(R.string.app_guide_audio_next_title),
                                    getString(R.string.app_guide_audio_next_desc));
                            break;
                    }
                }

                @Override
                public void watched() {
                    settingsManager.setNeedAudioPlayerActivityGuide(false);
                }
            });
            appGuide.makeGuide(findViewById(R.id.trackList),
                    getString(R.string.app_guide_track_list_title),
                    getString(R.string.app_guide_track_list_desc));
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && settingsManager.isFullScreenMode()) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    private void makeLoopingDialog() {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.addToBackStack(null);
        LoopingDialog dialog = LoopingDialog.newInstance(lastDurationSeconds);
        dialog.show(transaction, LoopingDialog.class.getName());
        dialog.setLoopingBetweenListener(this::setLoopBetween);
    }

    private void setLoopBetween(int start, int end) {
        PlaybackService.startLooping(getApplicationContext(), start, end);
        repeatButton.setLoopBetweenTime(start, end);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean isProUser = settingsManager.isUserPro();
        if (isProUser) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.menu_player, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_cue:
                startCueActivity();
                return true;
            case R.id.menu_cut:
                startCutActivity();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void openTrackList() {
        List<AudioFile> audioFiles = audioPlayer.getCurrentTrackList();
        if (audioFiles != null) {
            startActivity(new Intent(AudioPlayerActivity.this, PlayerTrackListActivity.class));
        }
    }

    private void startEqualizerActivity() {
        startActivity(new Intent(AudioPlayerActivity.this, EffectsActivity.class));
    }

    private void startCueActivity() {
        startActivity(new Intent(AudioPlayerActivity.this, CueActivity.class));
    }

    private void startCutActivity() {
        CutMediaActivity.startCutMediaActivity(AudioPlayerActivity.this, CutMediaActivity.CUT_AUDIO);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCurrentTrackEvent(AudioFile currentTrack) {
        lastLoadSuccess = false;
        controlView.startLoading();

        setAudioTrackValues(currentTrack);

        checkFirstOrLastTrack();
    }

    private void checkFirstOrLastTrack() {
        if (audioPlayer.first()) {
            disablePreviousTrackButton();
        } else {
            enablePreviousTrackButton();
        }

        if (audioPlayer.last()) {
            disableNextTrackButton();
        } else {
            enableNextTrackButton();
        }
    }

    public void enablePreviousTrackButton() {
        prevTrack.setAlpha(1f);
        prevTrack.setEnabled(true);
        prevTrack.setClickable(true);
    }

    public void disablePreviousTrackButton() {
        prevTrack.setAlpha(0.5f);
        prevTrack.setEnabled(false);
        prevTrack.setClickable(false);
    }

    public void enableNextTrackButton() {
        nextTrack.setAlpha(1f);
        nextTrack.setEnabled(true);
        nextTrack.setClickable(true);
    }

    public void disableNextTrackButton() {
        nextTrack.setAlpha(0.5f);
        nextTrack.setEnabled(false);
        nextTrack.setClickable(false);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPlaybackStateEvent(PlaybackService playbackState) {

        boolean playing = playbackState.isPlaying();
        if (lastPlaying != playing) {
            lastPlaying = playing;
            controlView.setPlay(playing);
        }

        boolean isConvertStart = playbackState.isConvertStart();
        if (lastConvertStart != isConvertStart) {
            lastConvertStart = isConvertStart;
            controlView.startLoading();
        }

        boolean isLoadSuccess = playbackState.isLoadSuccess();
        if (lastLoadSuccess != isLoadSuccess) {
            lastLoadSuccess = isLoadSuccess;
            if (!lastLoadSuccess) {
                controlView.startLoading();
            } else {
                controlView.finishLoading();
            }
        }

        boolean isLoadError = playbackState.isLoadError();
        if (lastLoadError != isLoadError) {
            lastLoadError = isLoadError;
            if (lastLoadError && !FFmpegHelper.isAudioFileFLAC(audioPlayer.getCurrentTrack())) {
                showErrorAndClose();
            }
        }

        int positionSeconds = playbackState.getPosition();

        if (lastPositionSeconds != positionSeconds) {
            lastPositionSeconds = positionSeconds;
            controlView.setSeekValue((int) playbackState.getPositionPercent());
            trackTimeCount.setText(Utils.getPositionSecondsString(lastPositionSeconds));
        }

        int durationSeconds = playbackState.getDuration();

        if (lastDurationSeconds != durationSeconds) {
            lastDurationSeconds = durationSeconds;
            trackTimeTotal.setText(Utils.getPositionSecondsString(lastDurationSeconds));
        }

        boolean looping = playbackState.isLooping();
        if (lastLooping != looping) {
            lastLooping = looping;
            repeatButton.changeState(lastLooping);
        }

        float volume = playbackState.getVolume();
        if (lastVolume != volume) {
            lastVolume = volume;
            setVolumeLevel(lastVolume);
        }
    }

    private void showErrorAndClose() {
        Utils.showCustomSnackbar(findViewById(R.id.audioPlayerRoot), getApplicationContext(),
                getString(R.string.snackbar_loading_error),
                Snackbar.LENGTH_LONG).addCallback(new BaseTransientBottomBar.BaseCallback<Snackbar>() {
            @Override
            public void onDismissed(Snackbar snackbar, int event) {
                super.onDismissed(snackbar, event);
                finish();
            }
        }).show();
    }

    protected void setAudioTrackValues(AudioFile audioFile) {
        if (audioFile != null) {
            setTrackInformation(audioFile);
            setBackdropImage(audioFile);
        }
    }

    @Override
    public String getActivityName() {
        return this.getLocalClassName();
    }


    private void setBackdropImage(AudioFile audioFile) {
        BitmapHelper bitmapHelper = BitmapHelper.getInstance();
        bitmapHelper.loadAudioPlayerArtwork(audioFile, backdrop);

        subscription = bitmapHelper.getAudioFilePalette(audioFile)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::setPalette);
    }

    private void setPalette(BitmapHelper.PaletteColor color) {
        int muted = color.getMuted();
        trackTimeCount.setTextColor(muted);
        trackTimeTotal.setTextColor(muted);
        artist.setTextColor(muted);
        volumeLevel.setTextColor(muted);
        title.setTextColor(muted);
        genre.setTextColor(muted);
        album.setTextColor(muted);
        trackDescription.setTextColor(muted);
        prevTrack.setColorFilter(muted);
        nextTrack.setColorFilter(muted);
        equalizer.setColorFilter(muted);
        trackList.setColorFilter(muted);
        muteSoloButton.setColorFilter(muted);
        repeatButton.setColorFilter(muted);
        timer.setColorFilter(muted);
        controlView.setColorFilter(muted, color.getVibrantDark());
    }

    public void play() {
        audioPlayer.play();
    }


    public void pause() {
        audioPlayer.pause();
    }


    public void next() {
        if (!lastLooping && !lastConvertStart) {
            audioPlayer.next();
            cardDescription.next();
            resetIndicators();

        }
    }

    public void previous() {
        if (!lastLooping && !lastConvertStart) {
            audioPlayer.previous();
            cardDescription.previous();
            resetIndicators();
        }
    }

    private void setVolumeLevel(float volume) {
        volumeLevel.setText(String.format("%.0f", volume));
        controlView.setVolumeValue(volume);
        if (volume >= 60) {
            muteSoloButton.setHighSoloState();
        } else if (volume >= 30) {
            muteSoloButton.setMediumSoloState();
        } else {
            muteSoloButton.setLowSoloState();
        }
    }

    private void resetIndicators() {
        trackTimeTotal.setText(getString(R.string.infinity_symbol));
        trackTimeCount.setText(getString(R.string.infinity_symbol));
        controlView.setSeekValue(0);
    }


    private void setTrackInformation(AudioFile audioFile) {
        artist.setText(audioFile.artist);
        title.setText(audioFile.title);
        album.setText(audioFile.album);
        genre.setText(audioFile.genre);

        StringBuilder sb = new StringBuilder();
        sb.append(audioFile.sampleRate);
        sb.append("::");
        sb.append(audioFile.bitrate);
        sb.append("::");
        sb.append(audioFile.getFilePath());

        /**
         *  http://stackoverflow.com/questions/3332924/textview-marquee-not-working?noredirect=1&lq=1
         */
        trackDescription.setSelected(true);
        trackDescription.setText(sb.toString());
    }

    private void disableChangeVolume() {
        controlView.setEnableChangeVolume(false);
    }

    private void enableChangeVolume() {
        controlView.setEnableChangeVolume(true);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
