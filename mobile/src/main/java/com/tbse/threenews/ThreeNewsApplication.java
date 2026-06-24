package com.tbse.threenews;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import androidx.multidex.MultiDexApplication;

/**
 * Created by todd on 9/18/16.
 */

public class ThreeNewsApplication extends MultiDexApplication {

    private int deviceWidth;
    private int deviceHeight;
    public boolean shouldRun = true;

    @Override
    public void onCreate() {
        super.onCreate();

        final WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        final DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(metrics);

        deviceWidth = metrics.widthPixels;
        deviceHeight = metrics.heightPixels;
    }


    public int getDeviceWidth() {
        return deviceWidth;
    }

    public int getDeviceHeight() {
        return deviceHeight;
    }

    public boolean shouldRun() {
        return shouldRun;
    }

    public void setShouldRun(boolean b) {
        shouldRun = b;
    }

}
