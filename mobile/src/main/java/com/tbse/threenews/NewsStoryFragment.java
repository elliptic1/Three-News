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
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import static com.tbse.threenews.R.id.story_box;
import static com.tbse.threenews.mysyncadapter.MyContentProvider.CONTENT_URI;
import static com.tbse.threenews.mysyncadapter.MyContentProvider.HEADLINE;
import static com.tbse.threenews.mysyncadapter.MyContentProvider.IMG;
import static com.tbse.threenews.mysyncadapter.MyContentProvider.PROJECTION;

/**
 * Created by smitt345 on 9/23/16.
 */

public class NewsStoryFragment extends Fragment {

    private RelativeLayout storyBox;
    private ImageView storyImage;
    private Handler contentObserverHandler;
    private ContentResolver contentResolver;
    private int story_id = 0;
    private int deviceWidth;
    private int deviceHeight;

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
        storyBox = (RelativeLayout) view.findViewById(story_box);
        contentResolver.registerContentObserver(CONTENT_URI, false,
                new MyContentObserver(view, contentObserverHandler));
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

        private View view;

        /**
         * Creates a content observer.
         *
         * @param handler The handler to run {@link #onChange} on, or null if none.
         */
        MyContentObserver(View view, Handler handler) {
            super(handler);
            this.view = view;
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            final Cursor c = contentResolver.query(CONTENT_URI, PROJECTION, null, null, null);
            if (c != null && c.moveToPosition(story_id)) {
                final String headline = c.getString(c.getColumnIndex(HEADLINE));
                final String img = c.getString(c.getColumnIndex(IMG));
                c.close();

                storyImage.setContentDescription(headline);

                Picasso.with(view.getContext()).load(img)
                        .placeholder(R.drawable.loading)
                        .resize(deviceWidth/2, deviceHeight)
                        .centerCrop()
                        .into(storyImage);

                final TextView headlineTV = (TextView) view.findViewById(R.id.headline);
                headlineTV.setText(headline);
            }
        }
    }

}