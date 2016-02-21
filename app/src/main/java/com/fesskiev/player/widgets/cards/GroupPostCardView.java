package com.fesskiev.player.widgets.cards;


import android.content.Context;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.fesskiev.player.R;
import com.fesskiev.player.model.vk.GroupPost;
import com.fesskiev.player.model.vk.VKMusicFile;
import com.fesskiev.player.utils.Utils;
import com.squareup.picasso.Picasso;

import java.util.List;

public class GroupPostCardView extends CardView {

    private ViewGroup audioItems;

    public GroupPostCardView(Context context) {
        super(context);
        init(context);
    }

    public GroupPostCardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public GroupPostCardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }


    private void init(Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.card_post_group_layout, this, true);
    }

    private void createAudioItems(List<VKMusicFile> musicFiles) {
        audioItems = (ViewGroup) findViewById(R.id.audioFilesContainer);
        audioItems.removeAllViews();

        for (int i = 0; i < musicFiles.size(); i++) {
            VKMusicFile musicFile = musicFiles.get(i);
            audioItems.addView(makeAudioItem(musicFile, audioItems));
        }

    }


    private View makeAudioItem(VKMusicFile musicFile, ViewGroup viewGroup) {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        View v = inflater.inflate(R.layout.item_audio, viewGroup, false);
        v.setOnClickListener(clickListener);
        v.setTag(musicFile);


        TextView artist = (TextView) v.findViewById(R.id.itemArtist);
        TextView title = (TextView) v.findViewById(R.id.itemTitle);
        TextView duration = (TextView) v.findViewById(R.id.itemTime);
        ImageView downloadCompleteIcon = (ImageView) v.findViewById(R.id.itemDownloadComplete);
        ProgressBar downloadProgress = (ProgressBar) v.findViewById(R.id.downloadProgressBar);
        TextView progressValue = (TextView) v.findViewById(R.id.progressValue);
        View downloadContainer = v.findViewById(R.id.downloadContainer);

        if (musicFile != null) {
            downloadContainer.setVisibility(GONE);
            artist.setText(musicFile.artist);
            title.setText(musicFile.title);
            duration.setText(Utils.getTimeFromSecondsString(musicFile.duration));
        }

        return v;
    }

    private OnClickListener clickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            VKMusicFile musicFile = (VKMusicFile) v.getTag();
            if (musicFile != null) {
                Log.e("click", "click: " + musicFile.toString());
            }
        }
    };

    private void createGroupInfo(GroupPost groupPost) {
        TextView postText = (TextView) findViewById(R.id.postText);
        ImageView postCover = (ImageView) findViewById(R.id.postCover);
        TextView likes = (TextView) findViewById(R.id.likePost);
        TextView shares = (TextView) findViewById(R.id.sharePost);

        postText.setText(groupPost.text);
        likes.setText(String.valueOf(groupPost.likes));
        shares.setText(String.valueOf(groupPost.reposts));
        Picasso.with(getContext()).
                load(groupPost.photo).
                into(postCover);
    }


    public void setGroupPost(GroupPost groupPost) {
        Log.e("click", "audio items null: " + (audioItems == null));
        createGroupInfo(groupPost);
        createAudioItems(groupPost.musicFiles);
    }
}
