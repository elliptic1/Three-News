package com.tbse.threenews;

import android.content.Context;
import android.os.Bundle;
import android.support.multidex.MultiDexApplication;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import com.google.firebase.analytics.FirebaseAnalytics;

/**
 * Created by todd on 9/18/16.
 */

public class ThreeNewsApplication extends MultiDexApplication {

    private int deviceWidth;
    private int deviceHeight;
    public boolean shouldRun = true;
    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    public void onCreate() {
        super.onCreate();

        final WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        final DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(metrics);

        deviceWidth = metrics.widthPixels;
        deviceHeight = metrics.heightPixels;

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        final Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "news_app_open_id");
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "App Opened");
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "text");
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);

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
