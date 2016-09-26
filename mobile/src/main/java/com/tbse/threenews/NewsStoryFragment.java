package com.tbse.threenews;

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.tbse.threenews.mysyncadapter.MyTransform;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.tbse.threenews.R.id.headline;
import static com.tbse.threenews.mysyncadapter.MyContentProvider.CONTENT_URI;
import static com.tbse.threenews.mysyncadapter.MyContentProvider.DATE;
import static com.tbse.threenews.mysyncadapter.MyContentProvider.HEADLINE;
import static com.tbse.threenews.mysyncadapter.MyContentProvider.IMG;
import static com.tbse.threenews.mysyncadapter.MyContentProvider.PROJECTION;
import static com.tbse.threenews.mysyncadapter.MyContentProvider.SOURCE;
import static com.tbse.threenews.mysyncadapter.MyContentProvider._ID;
import static com.tbse.threenews.mysyncadapter.MySyncAdapter.sourceToName;

/**
 * Created by smitt345 on 9/23/16 for the Android Nanodegree capstone project.
 */

public class NewsStoryFragment extends Fragment {

    private boolean isStoryReady;
    private int deviceWidth;
    private int deviceHeight;
    private int story_id;
    private ContentResolver contentResolver;
    private Handler contentObserverHandler;
    private ImageView storyImage;
    private TextView headlineTV;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        contentObserverHandler = new Handler(Looper.getMainLooper());
        contentResolver = getContext().getContentResolver();
        final ThreeNewsApplication app
                = (ThreeNewsApplication) getContext().getApplicationContext();
        deviceHeight = app.getDeviceHeight();
        deviceWidth = app.getDeviceWidth();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        storyImage = (ImageView) view.findViewById(R.id.story_image);
        headlineTV = (TextView) view.findViewById(headline);
        contentResolver.registerContentObserver(CONTENT_URI, false,
                new MyContentObserver(contentObserverHandler));
        contentObserverHandler.postDelayed(new ShowStoryRunnable(view), 3000);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.news_story_fragment, container, false);
    }

    public void setStoryId(int a) {
        story_id = a;
    }

    private class MyContentObserver extends ContentObserver {

        private ExecutorService executorService;

        /**
         * Creates a content observer.
         *
         * @param handler The handler to run {@link #onChange} on, or null if none.
         */
        MyContentObserver(Handler handler) {
            super(handler);
            isStoryReady = false;
            executorService = Executors.newFixedThreadPool(20);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            final Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    Log.d("nano", "QQQQQQQQQQ");
                    final Cursor c = contentResolver.query(
                            CONTENT_URI, PROJECTION, null, null, DATE + " DESC");
                    if (c != null && c.moveToPosition(story_id) && !isStoryReady) {
                        Log.d("nano", "story " + story_id + " ready");
                        isStoryReady = true;
                    } else {
                        Log.d("nano", "story " + story_id + " not ready");
                    }

                    //

                    Log.d("nano", "db:");
                    if (c != null && c.moveToFirst()) {
                        int id = c.getInt(c.getColumnIndex(_ID));
                        String headline = c.getString(c.getColumnIndex(HEADLINE));
                        String source = c.getString(c.getColumnIndex(SOURCE));
                        int date = c.getInt(c.getColumnIndex(DATE));
                        Log.d("nano", id + " - " + date + " - " + source + " - " + headline);
                        while (c.moveToNext()) {
                            id = c.getInt(c.getColumnIndex(_ID));
                            headline = c.getString(c.getColumnIndex(HEADLINE));
                            source = c.getString(c.getColumnIndex(SOURCE));
                            date = c.getInt(c.getColumnIndex(DATE));
                            Log.d("nano", id + " - " + date + " - " + source + " - " + headline);
                        }
                    }
                    Log.d("nano", "--------");

                    //
                    if (c != null) {
                        c.close();
                        Log.d("nano", "-------QQQQQQQQQQ");
                    }
                }
            };
            executorService.submit(runnable);

        }
    }

    private class ShowStoryRunnable implements Runnable {
        private View view;

        ShowStoryRunnable(View view) {
            this.view = view;
        }

        public void run() {
            Log.d("nano", "checking for story " + story_id + " ready? " + isStoryReady);
            if (getContext() != null && isStoryReady) {
                final Cursor c = contentResolver.query(
                        CONTENT_URI, PROJECTION, null, null, DATE + " DESC");
                if (c != null && c.moveToPosition(story_id)) {
                    final String img = c.getString(c.getColumnIndex(IMG));
                    final String source = c.getString(c.getColumnIndex(SOURCE));
                    final String headline = c.getString(c.getColumnIndex(HEADLINE));
                    c.close();

                    if (headline.equals(headlineTV.getText().toString())) {
                        Log.d("nano", "headline didn't change, skipping");
                        isStoryReady = false;
                        return;
                    }

                    storyImage.setContentDescription(headline);

                    final MyTransform myTransform;
                    if (story_id == 0) {
                        myTransform = new MyTransform(deviceWidth * 0.618f, 1.0f * deviceHeight);
                    } else {
                        myTransform = new MyTransform(deviceWidth * 0.382f, deviceHeight / 2.0f);
                    }

                    Picasso.with(view.getContext())
                            .load(img)
                            .placeholder(R.drawable.loading)
                            .transform(myTransform)
                            .into(storyImage);

                    headlineTV.setText(sourceToName.get(source) + ": " + headline);
                } else {
                    isStoryReady = false;
                    Log.d("nano", "story " + story_id + " setting not ready");
                }
                if (c != null) {
                    c.close();
                }
            }
            contentObserverHandler.postDelayed(this, new Random().nextInt(20000) + 10000);
        }
    }

}
