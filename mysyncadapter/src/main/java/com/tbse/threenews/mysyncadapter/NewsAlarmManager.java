package com.tbse.threenews.mysyncadapter;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;

import hugo.weaving.DebugLog;

/**
 * Created by todd on 9/18/16.
 */

public class NewsAlarmManager extends BroadcastReceiver {
    public static final String ACCOUNT_TYPE = "type.threenews.tbse.com";
    public static final String ACCOUNT = "account.threenews.tbse.com";
    public static final String AUTHORITY = "com.tbse.threenews.provider";

    @Override
    @DebugLog
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("com.tbse.threenews.alarm")) {
            ContentResolver.requestSync(MySyncAdapter.createSyncAccount(context),
                    AUTHORITY, MySyncAdapter.getSettingsBundle());
        }
    }
}
