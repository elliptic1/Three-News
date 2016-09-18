package com.tbse.threenews.mysyncadapter;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import static android.content.Context.ACCOUNT_SERVICE;

/**
 * Created by todd on 9/18/16.
 */

public class NewsAlarmManager extends BroadcastReceiver {
    // An account type, in the form of a domain name
    public static final String ACCOUNT_TYPE = "type.threenews.tbse.com";
    // The account name
    public static final String ACCOUNT = "account.threenews.tbse.com";
    public static final String AUTHORITY = "com.tbse.threenews.provider";

    Account account;
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("nano", "onReceive");
        account = CreateSyncAccount(context);
        if (intent.getAction().equals("com.tbse.threenews.alarm")) {
            Log.d("nano", "got action");
            final Bundle settingsBundle = new Bundle();
            settingsBundle.putBoolean(
                    ContentResolver.SYNC_EXTRAS_MANUAL, true);
            ContentResolver.requestSync(account, AUTHORITY, settingsBundle);
        }
    }
    public static Account CreateSyncAccount(Context context) {
        final Account newAccount = new Account(ACCOUNT, ACCOUNT_TYPE);
        final AccountManager accountManager = (AccountManager) context.getSystemService(ACCOUNT_SERVICE);
        if (accountManager.addAccountExplicitly(newAccount, null, null)) {
            Log.d("nano", "account added");
        }
        return newAccount;
    }
}
