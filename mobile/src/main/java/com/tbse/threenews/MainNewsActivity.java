package com.tbse.threenews;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.tbse.threenews.mysyncadapter.MySyncAdapter;
import com.tbse.threenews.mysyncadapter.NewsAlarmManager;

import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import hugo.weaving.DebugLog;

import static com.tbse.threenews.mysyncadapter.MyContentProvider.CONTENT_URI;
import static com.tbse.threenews.mysyncadapter.MyContentProvider.PROJECTION;
import static com.tbse.threenews.mysyncadapter.NewsAlarmManager.AUTHORITY;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class MainNewsActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor>,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final int AUTO_HIDE_DELAY_MILLIS = 1000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;

    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            delayedHide(AUTO_HIDE_DELAY_MILLIS);
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                dialog.show();
                if (!MySyncAdapter.shouldGetNews(view.getContext())) {
                    Toast.makeText(view.getContext(),
                            "Can't update news while on mobile data!", Toast.LENGTH_LONG).show();
                    dialog.dismiss();
                } else {
                    ContentResolver.requestSync(MySyncAdapter.createSyncAccount(view.getContext()),
                            AUTHORITY, MySyncAdapter.getSettingsBundle());
                }
            }
            return false;
        }
    };

    private final View.OnTouchListener mSettingsDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                getSupportFragmentManager().beginTransaction()
                        .replace(android.R.id.content, settingsFragment)
                        .commit();
            }
            return false;
        }
    };

    private ProgressDialog dialog;
    private SettingsFragment settingsFragment;
    ExecutorService exService = Executors.newFixedThreadPool(10);

    @Override
    protected void onResume() {
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    @DebugLog
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main_news);

        settingsFragment = new SettingsFragment();

        dialog = new ProgressDialog(this);
        dialog.setMessage("Getting the latest news...");
        dialog.setIndeterminate(true);
        getContentResolver().registerContentObserver(CONTENT_URI, false,
                new ContentObserver(new Handler(Looper.getMainLooper())) {
                    @Override
                    public void onChange(boolean selfChange) {
                        super.onChange(selfChange);
                        if (dialog != null && dialog.isShowing()) {
                            dialog.dismiss();
                        }
                    }
                });

        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = findViewById(R.id.fullscreen_content);

        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        findViewById(R.id.refresh_button).setOnTouchListener(mDelayHideTouchListener);
        findViewById(R.id.settings_button).setOnTouchListener(mSettingsDelayHideTouchListener);

        // Set margin for right side of settings button
        final ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams)
                findViewById(R.id.settings_button).getLayoutParams();
        layoutParams.setMargins(0, 0, getNavigationBarHeight(
                this, getResources().getConfiguration().orientation), 0);
        //

        final AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        final Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        final Intent intent = new Intent(this, NewsAlarmManager.class);
        intent.setAction("com.tbse.threenews.alarm");
        final PendingIntent alarmIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
        alarmManager.setRepeating(AlarmManager.RTC, calendar.getTimeInMillis(),
                1000 * 60, alarmIntent);
    }

    @Override
    @DebugLog
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);

        final NewsStoryFragment article_main =
                (NewsStoryFragment) getSupportFragmentManager().findFragmentById(R.id.article_main);
        final NewsStoryFragment article_top_right =
                (NewsStoryFragment) getSupportFragmentManager().findFragmentById(R.id.article_top_right);
        final NewsStoryFragment article_bot_right =
                (NewsStoryFragment) getSupportFragmentManager().findFragmentById(R.id.article_bot_right);

        article_main.setStoryId(0);
        article_top_right.setStoryId(1);
        article_bot_right.setStoryId(2);

        dialog.show();
        if (!MySyncAdapter.shouldGetNews(this)) {
            Toast.makeText(this,
                    "Can't update news while on mobile data!", Toast.LENGTH_LONG).show();
            dialog.dismiss();
        } else {
            ContentResolver.requestSync(MySyncAdapter.createSyncAccount(this),
                    AUTHORITY, MySyncAdapter.getSettingsBundle());
        }
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    @Override
    @DebugLog
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(MainNewsActivity.this, CONTENT_URI, PROJECTION, null, null, null);
    }

    @Override
    @DebugLog
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

    }

    @Override
    @DebugLog
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    private int getNavigationBarHeight(Context context, int orientation) {
        final Resources resources = context.getResources();

        final int id = resources.getIdentifier(
                orientation == Configuration.ORIENTATION_PORTRAIT ? "navigation_bar_height" : "navigation_bar_height_landscape", "dimen", "android");
        if (id > 0) {
            return resources.getDimensionPixelSize(id);
        }
        return 0;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, final String key) {
        if (key.equals("allow-mobile-data")) {
            return;
        }
        if (sharedPreferences.getBoolean(key, false)) {
            final Context context = this;
            final Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    MySyncAdapter.startRequestForSource(context, key);
                }
            };
            exService.submit(runnable);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && settingsFragment.isAdded()) {
            getSupportFragmentManager().beginTransaction().remove(settingsFragment).commit();
            delayedHide(AUTO_HIDE_DELAY_MILLIS);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
