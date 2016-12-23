package com.fesskiev.player.ui.equalizer;


import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.fesskiev.player.R;
import com.fesskiev.player.data.model.EQState;
import com.fesskiev.player.services.PlaybackService;
import com.fesskiev.player.utils.AppSettingsManager;
import com.fesskiev.player.widgets.eq.BandControlView;

import org.greenrobot.eventbus.EventBus;


public class EqualizerFragment extends Fragment implements BandControlView.OnBandLevelListener {

    private Context context;
    private EQState state;
    private AppSettingsManager settingsManager;

    public static EqualizerFragment newInstance() {
        return new EqualizerFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getContext().getApplicationContext();
        settingsManager = AppSettingsManager.getInstance(context);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_equalizer, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.saveEQStateButton).setOnClickListener(v -> {

            settingsManager.setEQState(state);
            EventBus.getDefault().post(state);

            getActivity().finish();
        });

        SwitchCompat switchEQState = (SwitchCompat) view.findViewById(R.id.stateEqualizer);
        switchEQState.setOnCheckedChangeListener((compoundButton, checked) ->
                PlaybackService.changeEQEnable(getContext(), checked));
        switchEQState.setChecked(settingsManager.isEQOn());


        BandControlView[] bandControlViews = new BandControlView[]{
                (BandControlView) view.findViewById(R.id.bandControlLow),
                (BandControlView) view.findViewById(R.id.bandControlMid),
                (BandControlView) view.findViewById(R.id.bandControlHigh)
        };

        for (BandControlView bandControlView : bandControlViews) {
            bandControlView.setOnBandLevelListener(this);
        }

        if (settingsManager.isEQOn()) {
            PlaybackService.changeEQEnable(context, true);
        } else {
            PlaybackService.changeEQEnable(context, false);
        }

        setEQState(bandControlViews);
    }

    private void setEQState(BandControlView[] bandControlViews) {
        state = settingsManager.getEQState();
        if (state != null) {
            Log.wtf("test", "state: " + state.toString());

            for (int i = 0; i < bandControlViews.length; i++) {
                switch (i) {
                    case 0:
                        bandControlViews[i].setLevel(state.getLowBand());
                        break;
                    case 1:
                        bandControlViews[i].setLevel(state.getMidBand());
                        break;
                    case 2:
                        bandControlViews[i].setLevel(state.getHighBand());
                        break;
                }
            }
        } else {
            state = new EQState();
        }
    }

    @Override
    public void onBandLevelChanged(int band, int level) {
        Log.d("test", " band, " + band + " level: " + level);

        PlaybackService.changeEQBandLevel(context, band, level);

        switch (band) {
            case 0:
                state.setLowBand(level);
                break;
            case 1:
                state.setMidBand(level);
                break;
            case 2:
                state.setHighBand(level);
                break;
        }
    }
}
