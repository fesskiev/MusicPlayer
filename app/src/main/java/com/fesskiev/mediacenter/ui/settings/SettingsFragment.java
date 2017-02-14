package com.fesskiev.mediacenter.ui.settings;


import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.drawable.Animatable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;

import com.fesskiev.mediacenter.R;
import com.fesskiev.mediacenter.services.FileSystemService;
import com.fesskiev.mediacenter.utils.AppSettingsManager;
import com.fesskiev.mediacenter.widgets.settings.MediaContentUpdateTimeView;


public class SettingsFragment extends Fragment implements CompoundButton.OnCheckedChangeListener {


    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }

    private static final int JOB_ID = 31;

    private AppSettingsManager appSettingsManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        appSettingsManager = AppSettingsManager.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SwitchCompat[] switches = new SwitchCompat[]{
                (SwitchCompat) view.findViewById(R.id.play_headset_plug_in_switch),
                (SwitchCompat) view.findViewById(R.id.download_wifi_switch)
        };

        for (SwitchCompat switchCompat : switches) {
            switchCompat.setOnCheckedChangeListener(this);
        }

        MediaContentUpdateTimeView contentUpdateTimeView
                = (MediaContentUpdateTimeView) view.findViewById(R.id.mediaContentUpdateTime);
        contentUpdateTimeView.setOnMediaContentTimeUpdateListener(new MediaContentUpdateTimeView
                .OnMediaContentTimeUpdateListener() {
            @Override
            public void onUpdateByTime(int time) {
                startBackgroundJob(time);
            }

            @Override
            public void onCancelUpdateByTime() {
                stopBackgroundJob();
            }
        });

        ImageView timerView = (ImageView) view.findViewById(R.id.timerView);
        view.findViewById(R.id.searchFilesTitleContainer).setOnClickListener(v -> {
            ((Animatable) timerView.getDrawable()).start();
            contentUpdateTimeView.toggleWithAnimate();
        });

        setSettingsState(switches);
    }

    private void setSettingsState(SwitchCompat[] switches) {
        for (SwitchCompat switchCompat : switches) {
            switch (switchCompat.getId()) {
                case R.id.play_headset_plug_in_switch:
                    switchCompat.setChecked(appSettingsManager.isPlayPlugInHeadset());
                    break;
                case R.id.download_wifi_switch:
                    switchCompat.setChecked(appSettingsManager.isDownloadWiFiOnly());
                    break;
            }
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton switchCompat, boolean isChecked) {
        switch (switchCompat.getId()) {
            case R.id.play_headset_plug_in_switch:
                appSettingsManager.setPlayPlugInHeadset(isChecked);
                break;
            case R.id.download_wifi_switch:
                appSettingsManager.setDownloadWiFiOnly(isChecked);
                break;
        }
    }

    private void startBackgroundJob(int periodic) {
        Log.d("job", "startBackgroundJob: " + periodic);

        JobInfo.Builder builder = new JobInfo.Builder(JOB_ID, new ComponentName(getActivity(), FileSystemService.class));
        builder.setPersisted(true);
        builder.setOverrideDeadline(5000);
        builder.setMinimumLatency(15 * 1000 * 60);
        builder.setRequiresDeviceIdle(true);
        builder.setRequiresCharging(false);
        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);

        JobScheduler jobScheduler = (JobScheduler) getActivity().getSystemService(Context.JOB_SCHEDULER_SERVICE);
        int jobValue = jobScheduler.schedule(builder.build());
        if (jobValue == JobScheduler.RESULT_FAILURE) {
            Log.w("job", "JobScheduler launch the task failure");
        } else {
            Log.w("job", "JobScheduler launch the task success: " + jobValue);
        }
    }

    private void stopBackgroundJob() {
        Log.d("job", "stopBackgroundJob");

        JobScheduler jobScheduler = (JobScheduler) getActivity().getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.cancel(JOB_ID);
    }
}
