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

import static com.tbse.threenews.mysyncadapter.MyContentProvider.CONTENT_URI;
import static com.tbse.threenews.mysyncadapter.MyContentProvider.HEADLINE;
import static com.tbse.threenews.mysyncadapter.MyContentProvider.PROJECTION;

/**
 * Created by smitt345 on 9/23/16.
 */

public class NewsStoryFragment extends Fragment {

    private Handler contentObserverHandler;
    private ContentResolver contentResolver;
    private int story_id = 0;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        contentObserverHandler = new Handler(Looper.getMainLooper());
        contentResolver = getContext().getContentResolver();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        contentResolver.registerContentObserver(CONTENT_URI, false,
                new MyContentObserver(contentObserverHandler));
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.news_story_fragment, container, false);
    }

    public void setStoryId(int a) {
        Log.d("nano", "setting story id to " + a);
        story_id = a;
    }

    private class MyContentObserver extends ContentObserver {
        /**
         * Creates a content observer.
         *
         * @param handler The handler to run {@link #onChange} on, or null if none.
         */
        MyContentObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            Log.d("nano", "content changed for frag " + story_id);
            final Cursor c = contentResolver.query(CONTENT_URI, PROJECTION, null, null,
                    " limit 1 offset " + story_id);

            if (c != null && c.moveToFirst()) {
                Log.d("nano", "found " + c.getString(c.getColumnIndex(HEADLINE)));
                c.close();
            }
        }
    }

}
