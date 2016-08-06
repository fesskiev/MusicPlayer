package com.fesskiev.player.utils;

import android.app.Activity;
import android.content.Context;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import com.fesskiev.player.R;
import com.vk.sdk.util.VKUtil;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;


public class Utils {

    private static final String TAG = Utils.class.getName();

    public static String getTimeFromMillisecondsString(long millis) {
        return String.format("%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(millis),
                TimeUnit.MILLISECONDS.toSeconds(millis) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
        );
    }

    public static String getDateStringFromSeconds(long seconds) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        return dateFormat.format(new Date(seconds * 1000));
    }

    public static String getTimeFromSecondsString(int seconds) {
        return getTimeFromMillisecondsString(seconds * 1000);
    }

    public static void getCertificateFingerprint(Context context) {
        String[] fingerprints =
                VKUtil.getCertificateFingerprint(context, context.getPackageName());
        Log.d(TAG, "fingerprint: " + Arrays.toString(fingerprints));
    }

    public static Snackbar showCustomSnackbar(View view, Context context, String text, int duration) {
        Snackbar snack = Snackbar.make(view, text, duration);
        ViewGroup group = (ViewGroup) snack.getView();
        group.setBackgroundColor(ContextCompat.getColor(context, R.color.primary_dark));
        return snack;
    }

    public static String getDurationString(int millis) {
        int seconds = millis / 1000;
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        seconds = seconds % 60;

        return twoDigitString(hours) + ":" + twoDigitString(minutes) + ":" + twoDigitString(seconds);
    }

    private static String twoDigitString(int number) {

        if (number == 0) {
            return "00";
        }

        if (number / 10 == 0) {
            return "0" + number;
        }

        return String.valueOf(number);
    }

    public static void hideKeyboard(Activity activity) {
        InputMethodManager inputMethodManager =
                (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(),
                InputMethodManager.HIDE_NOT_ALWAYS);
    }

    public static String replaceSymbols(String fileName) {
        return fileName.replaceAll("[|\\?*<\":>+/']", "");
    }

}
