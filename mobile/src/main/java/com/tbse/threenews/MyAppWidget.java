package com.tbse.threenews;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.RemoteViews;

import com.squareup.picasso.Picasso;

public class MyAppWidget extends AppWidgetProvider {
    public MyAppWidget() {
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        final int N = appWidgetIds.length;

        // Perform this loop procedure for each App Widget that belongs to this provider
        for (int appWidgetId : appWidgetIds) {
            final Intent intent = new Intent(context, MainNewsActivity.class);
            final PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

            // Get the layout for the App Widget and attach an on-click listener
            // to the button
            final RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.news_story_fragment);

            // Tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final Bundle extras = intent.getExtras();
        int[] appIds;

        final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

        final RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
                R.layout.appwidget);

        if (extras == null) return;

        appIds = extras.getIntArray(context.getString(R.string.extra_ids));

        if (appIds == null) return;
        for (int a : appIds) {
            remoteViews.setTextViewText(R.id.headline, intent.getStringExtra(
                    context.getString(R.string.extra_headline)
            ));
            appWidgetManager.updateAppWidget(a, remoteViews);

            Picasso.with(context)
                    .load(intent.getStringExtra(context.getString(R.string.extra_image_url)))
                    .into(remoteViews, R.id.main_image, new int[] { a });
        }
    }

}
