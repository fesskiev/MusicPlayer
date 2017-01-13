package com.fesskiev.mediacenter.widgets;


import android.content.Context;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.fesskiev.mediacenter.R;
import com.fesskiev.mediacenter.data.model.vk.Audio;
import com.fesskiev.mediacenter.utils.Utils;
import com.fesskiev.mediacenter.utils.download.DownloadGroupAudioFile;
import com.fesskiev.mediacenter.utils.download.DownloadManager;

import java.util.List;

public class GroupPostAudioView extends FrameLayout {

    private LinearLayout audioItems;

    public GroupPostAudioView(Context context) {
        super(context);
        init(context);
    }

    public GroupPostAudioView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public GroupPostAudioView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }


    private void init(Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.group_post_audio_layout, this, true);
    }

    public void createAudioItems(List<DownloadGroupAudioFile> downloadGroupAudioFiles) {
        audioItems = (LinearLayout) findViewById(R.id.audioFilesContainer);
        audioItems.removeAllViews();

        for (int i = 0; i < downloadGroupAudioFiles.size(); i++) {
            DownloadGroupAudioFile downloadGroupAudioFile = downloadGroupAudioFiles.get(i);
            audioItems.addView(makeAudioItem(downloadGroupAudioFile, audioItems));
        }
    }

    public void removeAudioItems() {
        if (audioItems != null) {
            audioItems.removeAllViews();
        }
    }

    private View makeAudioItem(final DownloadGroupAudioFile downloadGroupAudioFile, final ViewGroup viewGroup) {

        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.item_vk_audio, viewGroup, false);

        v.setOnClickListener(clickListener);
        v.setTag(downloadGroupAudioFile);

        TextView artist = (TextView) v.findViewById(R.id.itemArtist);
        TextView title = (TextView) v.findViewById(R.id.itemTitle);
        TextView duration = (TextView) v.findViewById(R.id.itemTime);

        final View downloadContainer = v.findViewById(R.id.downloadContainer);
        final View itemContainer = v.findViewById(R.id.itemContainer);

        final ProgressBar downloadProgress = (ProgressBar) v.findViewById(R.id.downloadProgressBar);
        final TextView progressValue = (TextView) v.findViewById(R.id.progressValue);
        final ImageView startPauseDownload = (ImageView) v.findViewById(R.id.startPauseDownloadButton);
        startPauseDownload.setOnClickListener(v12 -> {
            DownloadManager downloadManager = downloadGroupAudioFile.getDownloadManager();
            switch (downloadManager.getStatus()) {
                case DownloadManager.PAUSED:
                    downloadManager.resume();
                    break;
                case DownloadManager.DOWNLOADING:
                    downloadManager.pause();
                    break;
            }
        });
        final ImageView cancelDownload = (ImageView) v.findViewById(R.id.cancelDownloadButton);
        cancelDownload.setOnClickListener(v1 -> {
            DownloadManager downloadManager = downloadGroupAudioFile.getDownloadManager();
            downloadManager.cancel();
            if (downloadManager.removeFile()) {
                Utils.showCustomSnackbar(viewGroup,
                        getContext(),
                        getResources().getString(R.string.shackbar_delete_file),
                        Snackbar.LENGTH_LONG).show();
            }
        });

        downloadGroupAudioFile.setOnDownloadAudioListener(downloadManager -> {
                    if (downloadManager != null) {
                        downloadContainer.setVisibility(VISIBLE);
                        switch (downloadManager.getStatus()) {
                            case DownloadManager.DOWNLOADING:
                                downloadContainer.setVisibility(View.VISIBLE);
                                cancelDownload.setVisibility(View.GONE);
                                startPauseDownload.setImageResource(R.drawable.pause_icon);
                                downloadProgress.setProgress((int) downloadManager.getProgress());

                                progressValue.setText(String.format("%1$d %2$s",
                                        (int) downloadManager.getProgress(), "\u0025"));
                                break;
                            case DownloadManager.COMPLETE:
                                downloadContainer.setVisibility(View.GONE);
                                itemContainer.setBackgroundColor(ContextCompat.getColor(getContext(),
                                        R.color.primary_light));
                                break;
                            case DownloadManager.PAUSED:
                                startPauseDownload.setImageResource(R.drawable.download_icon);
                                cancelDownload.setVisibility(View.VISIBLE);
                                break;
                            case DownloadManager.CANCELLED:
                                downloadContainer.setVisibility(View.GONE);
                                break;
                            case DownloadManager.ERROR:
                                downloadContainer.setBackgroundColor(ContextCompat.getColor(getContext(),
                                        R.color.red));
                                cancelDownload.setVisibility(View.VISIBLE);
                                startPauseDownload.setVisibility(View.GONE);
                                break;
                        }

                    } else {
                        downloadContainer.setVisibility(GONE);
                    }
                });


        Audio audio = downloadGroupAudioFile.getAudio();
        if (audio != null) {
            artist.setText(Html.fromHtml(audio.getArtist()));
            title.setText(Html.fromHtml(audio.getTitle()));
            duration.setText(Utils.getTimeFromSecondsString(audio.getDuration()));
        }

        return v;
    }

    private OnClickListener clickListener = v -> {
        DownloadGroupAudioFile downloadGroupAudioFile = (DownloadGroupAudioFile) v.getTag();
        if (downloadGroupAudioFile != null) {
            downloadGroupAudioFile.startDownload();
        }
    };
}