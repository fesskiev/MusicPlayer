package com.fesskiev.player.model;


import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class AudioPlayer {

    public static final String ACTION_CHANGE_CURRENT_AUDIO_FOLDER
            = "com.fesskiev.player.ACTION_CHANGE_CURRENT_AUDIO_FOLDER";
    public static final String ACTION_CHANGE_CURRENT_AUDIO_FILE
            = "com.fesskiev.player.ACTION_CHANGE_CURRENT_AUDIO_FILE";

    private Context context;
    public List<AudioFolder> audioFolders;
    public AudioFolder currentAudioFolder;
    public AudioFile currentAudioFile;
    public int position;
    public int volume;
    public boolean isPlaying;

    public AudioPlayer(Context context) {
        this.context = context;
        this.audioFolders = new ArrayList<>();
        this.volume = 100;
    }


    public void setCurrentAudioFile(AudioFile audioFile) {
        this.currentAudioFile = audioFile;
        sendBroadcastChangeAudioFile();
    }

    public void next() {
        if (!lastPosition()) {
            incrementPosition();
        }
        setCurrentAudioFile(currentAudioFolder.audioFilesDescription.get(position));
    }

    public void previous() {
        if (position > 0) {
            decrementPosition();
        }
        setCurrentAudioFile(currentAudioFolder.audioFilesDescription.get(position));
    }

    private boolean lastPosition() {
        return position == currentAudioFolder.audioFilesDescription.size() - 1;
    }

    private void incrementPosition() {
        position++;
    }

    private void decrementPosition() {
        position--;
    }

    public void sendBroadcastChangeAudioFolder() {
        Intent intent = new Intent();
        intent.setAction(ACTION_CHANGE_CURRENT_AUDIO_FOLDER);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public void sendBroadcastChangeAudioFile() {
        Intent intent = new Intent();
        intent.setAction(ACTION_CHANGE_CURRENT_AUDIO_FILE);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
}