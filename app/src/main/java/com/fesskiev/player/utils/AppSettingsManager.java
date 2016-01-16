package com.fesskiev.player.utils;


import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.text.TextUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;


public class AppSettingsManager {

    private static final String APP_SETTINGS_PREFERENCES = "com.fesskiev.player_settings_preferences";
    private static final String KEY_OAUTH_TOKEN = "com.fesskiev.player.SAVE_STATE_KEY_OAUTH_TOKEN";
    private static final String KEY_OAUTH_SECRET = "com.fesskiev.player.SAVE_STATE_KEY_OAUTH_SECRET";
    private static final String KEY_USER_ID = "com.fesskiev.player.SAVE_STATE_KEY_USER_ID";
    private static final String KEY_USER_FIRST_NAME = "com.fesskiev.player.KEY_USER_FIRST_NAME";
    private static final String KEY_USER_LAST_NAME = "com.fesskiev.player.KEY_USER_LAST_NAME";

    private SharedPreferences sharedPreferences;


    public AppSettingsManager(Context context) {
        sharedPreferences =
                context.getSharedPreferences(APP_SETTINGS_PREFERENCES, Context.MODE_PRIVATE);
    }


    public String getAuthToken() {
        return sharedPreferences.getString(KEY_OAUTH_TOKEN, "");
    }

    public void setAuthToken(String authToken){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_OAUTH_TOKEN, authToken);
        editor.apply();
    }

    public String getAuthSecret() {
        return sharedPreferences.getString(KEY_OAUTH_SECRET, "");
    }

    public void setAuthSecret(String authSecret){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_OAUTH_SECRET, authSecret);
        editor.apply();
    }

    public String getUserId() {
        return sharedPreferences.getString(KEY_USER_ID, "");
    }

    public void setUserId(String userId){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_USER_ID, userId);
        editor.apply();
    }

    public String getUserFirstName() {
        return sharedPreferences.getString(KEY_USER_FIRST_NAME, "");
    }

    public void setUserFirstName(String userId){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_USER_FIRST_NAME, userId);
        editor.apply();
    }

    public String getUserLastName() {
        return sharedPreferences.getString(KEY_USER_LAST_NAME, "");
    }

    public void setUserLastName(String userId){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_USER_LAST_NAME, userId);
        editor.apply();
    }


    public boolean isAuthTokenEmpty(){
        return TextUtils.isEmpty(getAuthToken());
    }

    private String getUserPhotoPath(){
        String externalStorage = Environment.getExternalStorageDirectory().toString();
        File folder = new File(externalStorage + "/MusicPlayer/UserPhoto/");
        if (!folder.exists()) {
            folder.mkdirs();
        }

        return new File(folder.getAbsolutePath(), "user_photo.png").getAbsolutePath();
    }

    public void saveUserPhoto(Bitmap bitmap){
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(getUserPhotoPath());
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public Bitmap getUserPhoto(){
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        return BitmapFactory.decodeFile(getUserPhotoPath(), options);
    }
}

