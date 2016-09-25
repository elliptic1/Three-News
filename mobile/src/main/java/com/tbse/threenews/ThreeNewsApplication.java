package com.tbse.threenews;

import android.content.Context;
import android.support.multidex.MultiDexApplication;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

/**
 * Created by todd on 9/18/16.
 */

public class ThreeNewsApplication extends MultiDexApplication {

    private int deviceWidth = 100;
    private int deviceHeight = 100;

    @Override
    public void onCreate() {
        super.onCreate();

        final WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        final DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(metrics);

        deviceWidth = metrics.widthPixels;
        deviceHeight = metrics.heightPixels;

        Log.d("nano", "width = " + metrics.widthPixels);
        Log.d("nano", "height = " + metrics.heightPixels);

    }

    public int getDeviceWidth() {
        return deviceWidth;
    }

    public int getDeviceHeight() {
        return deviceHeight;
    }
}
