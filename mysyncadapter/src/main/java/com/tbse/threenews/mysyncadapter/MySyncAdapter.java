package com.tbse.threenews.mysyncadapter;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import hugo.weaving.DebugLog;

import static android.content.Context.ACCOUNT_SERVICE;
import static com.tbse.threenews.mysyncadapter.MyContentProvider.CONTENT_URI;
import static com.tbse.threenews.mysyncadapter.MyContentProvider.DATE;
import static com.tbse.threenews.mysyncadapter.MyContentProvider.HEADLINE;
import static com.tbse.threenews.mysyncadapter.MyContentProvider.IMG;
import static com.tbse.threenews.mysyncadapter.MyContentProvider.LINK;
import static com.tbse.threenews.mysyncadapter.MyContentProvider.PRIORITY;
import static com.tbse.threenews.mysyncadapter.NewsAlarmManager.ACCOUNT;
import static com.tbse.threenews.mysyncadapter.NewsAlarmManager.ACCOUNT_TYPE;


public class MySyncAdapter extends AbstractThreadedSyncAdapter {

    private ContentResolver contentResolver;
    private String apiKey;

    @DebugLog
    MySyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        Log.d("nano", "MySyncAdapter init");
        contentResolver = context.getContentResolver();
        apiKey = getContext().getString(R.string.newsapikey);
    }

    @DebugLog
    @Override
    public void onPerformSync(Account account, Bundle extras,
                              String authority, ContentProviderClient provider,
                              SyncResult syncResult) {
        final StringRequest stringRequest = new StringRequest(Request.Method.GET,
                getContext().getString(R.string.apiurl)
                        + "?source=techcrunch&apiKey="
                        + getContext().getString(R.string.newsapikey)
                        + "&sortBy=top",
                new MyResponseListener(), new MyErrorListener());
        stringRequest.setTag(this);

        final RequestQueue queue = Volley.newRequestQueue(getContext());
        queue.add(stringRequest);
    }

    @Override
    public void onSyncCanceled() {
        super.onSyncCanceled();
    }

    private class MyResponseListener implements Response.Listener<String> {
        @Override
        @DebugLog
        public void onResponse(String response) {
            try {
                final JSONObject respJSON = new JSONObject(response);
                final JSONArray array = respJSON.getJSONArray("articles");
                contentResolver.delete(CONTENT_URI, null, null);
                ArrayList<String> titles = new ArrayList<>();
                for (int i = 0; i < array.length(); i++) {
                    final JSONObject jsonArticle = array.getJSONObject(i);
                    final String title = jsonArticle.getString("title");
                    if (titles.contains(title)) {
                        continue;
                    }
                    titles.add(title);
                    final ContentValues contentValues = new ContentValues();
                    contentValues.put(IMG, jsonArticle.getString("urlToImage"));
                    contentValues.put(HEADLINE, title);
                    contentValues.put(LINK, jsonArticle.getString("url"));
                    contentValues.put(DATE, jsonArticle.getString("publishedAt"));
                    contentValues.put(PRIORITY, 1);
                    contentResolver.insert(CONTENT_URI, contentValues);
                    if (titles.size() == 3) {
                        break;
                    }
                }
            } catch (JSONException e) {
                Log.e("nano", "json error: " + e);
            }
        }
    }

    private class MyErrorListener implements Response.ErrorListener {
        @Override
        @DebugLog
        public void onErrorResponse(VolleyError error) {
            Log.e("nano", "got an error: " + error);
        }
    }

    @DebugLog
    public static Account createSyncAccount(Context context) {
        final Account newAccount = new Account(ACCOUNT, ACCOUNT_TYPE);
        final AccountManager accountManager = (AccountManager) context.getSystemService(ACCOUNT_SERVICE);
        accountManager.addAccountExplicitly(newAccount, null, null);
        return newAccount;
    }

    public static Bundle getSettingsBundle() {
        final Bundle settingsBundle = new Bundle();
        settingsBundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        return settingsBundle;
    }

}
