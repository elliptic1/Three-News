package com.tbse.threenews.mysyncadapter;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

import hugo.weaving.DebugLog;

import static android.content.Context.ACCOUNT_SERVICE;
import static com.tbse.threenews.mysyncadapter.MyContentProvider.CONTENT_URI;
import static com.tbse.threenews.mysyncadapter.MyContentProvider.DATE;
import static com.tbse.threenews.mysyncadapter.MyContentProvider.HEADLINE;
import static com.tbse.threenews.mysyncadapter.MyContentProvider.IMG;
import static com.tbse.threenews.mysyncadapter.MyContentProvider.LINK;
import static com.tbse.threenews.mysyncadapter.MyContentProvider.SOURCE;
import static com.tbse.threenews.mysyncadapter.NewsAlarmManager.ACCOUNT;
import static com.tbse.threenews.mysyncadapter.NewsAlarmManager.ACCOUNT_TYPE;


public class MySyncAdapter extends AbstractThreadedSyncAdapter {

    private ContentResolver contentResolver;
    private RequestQueue queue;
    public static HashMap<String, String> sourceToName;

    @DebugLog
    MySyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        Log.d("nano", "MySyncAdapter init");
        contentResolver = context.getContentResolver();
        queue = Volley.newRequestQueue(getContext());
        final String[] sources = getContext().getResources().getStringArray(R.array.newssources);
        final String[] sourcesnames = getContext().getResources().getStringArray(R.array.newssourcesnames);
        sourceToName = new HashMap<>();
        for (int i = 0; i < sources.length; i++) {
            sourceToName.put(sources[i], sourcesnames[i]);
        }
    }

    @DebugLog
    @Override
    public void onPerformSync(Account account, Bundle extras,
                              String authority, ContentProviderClient provider,
                              SyncResult syncResult) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        for (String source : sourceToName.keySet()) {
            final StringRequest stringRequest = new StringRequest(Request.Method.GET,
                    getContext().getString(R.string.apiurl)
                            + "?source=" + source + "&apiKey="
                            + getContext().getString(R.string.newsapikey),
                    new MyResponseListener(source), new MyErrorListener());
            stringRequest.setTag(this);
            queue.add(stringRequest);
        }
    }

    @Override
    public void onSyncCanceled() {
        super.onSyncCanceled();
    }

    private class MyResponseListener implements Response.Listener<String> {
        private String source;

        MyResponseListener(String source) {
            this.source = source;
        }

        @Override
        @DebugLog
        public void onResponse(String response) {
            try {
                final JSONObject respJSON = new JSONObject(response);
                final JSONArray array = respJSON.getJSONArray("articles");
                final ArrayList<String> titles = new ArrayList<>();
                contentResolver.delete(CONTENT_URI, null, null);
                for (int i = 0; i < array.length(); i++) {
                    final JSONObject jsonArticle = array.getJSONObject(i);
                    if (jsonArticle.get("title").equals(JSONObject.NULL)
                            || jsonArticle.get("urlToImage").equals(JSONObject.NULL)
                            || jsonArticle.get("publishedAt").equals(JSONObject.NULL)
                            ) {
                        continue;
                    }
                    final String title = jsonArticle.getString("title");
                    if (titles.contains(title)) {
                        continue;
                    }
                    titles.add(title);
                    final ContentValues contentValues = new ContentValues();
                    contentValues.put(IMG, jsonArticle.getString("urlToImage"));
//                    Log.d("nano", "putting " + sourceToName.get(source) + ": "
//                            + title.substring(0, Math.min(title.length()-1, 20)) + "...");
                    contentValues.put(HEADLINE, title);
                    contentValues.put(SOURCE, source);
                    contentValues.put(LINK, jsonArticle.getString("url"));
                    final DateTime dateTime = new DateTime(jsonArticle.get("publishedAt"));
                    contentValues.put(DATE, dateTime.getMillis() / 1000);
                    contentResolver.insert(CONTENT_URI, contentValues);
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
        final AccountManager accountManager
                = (AccountManager) context.getSystemService(ACCOUNT_SERVICE);
        accountManager.addAccountExplicitly(newAccount, null, null);
        return newAccount;
    }

    public static Bundle getSettingsBundle() {
        final Bundle settingsBundle = new Bundle();
        settingsBundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        return settingsBundle;
    }

}
