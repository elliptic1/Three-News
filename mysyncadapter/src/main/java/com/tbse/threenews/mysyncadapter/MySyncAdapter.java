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
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

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
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    private static RequestQueue queue;
    public static HashMap<String, String> sourceToName;

    @DebugLog
    MySyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        Log.d("nano", "MySyncAdapter init");
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
        if (!shouldGetNews(getContext())) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getContext(),
                            "Can't update news while on mobile data!", Toast.LENGTH_LONG).show();
                }
            });
            return;
        }

        getContext().getContentResolver().delete(CONTENT_URI, null, null);

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        final List<String> sourcesToCall = new ArrayList<>();
        for (String source : sourceToName.keySet()) {
            if (prefs.getBoolean(source, false)) {
                sourcesToCall.add(source);
            }
        }
        if (sourcesToCall.size() == 0) {
            sourcesToCall.add("cnn");
        }
        for (String source : sourcesToCall) {
            startRequestForSource(getContext(), source);
        }
    }

    public static void startRequestForSource(Context context, String source) {
        final StringRequest stringRequest = new StringRequest(Request.Method.GET,
                context.getString(R.string.apiurl)
                        + "?source=" + source + "&apiKey="
                        + context.getString(R.string.newsapikey),
                new MyResponseListener(context, source), new MyErrorListener());
        stringRequest.setTag(context);
        queue.add(stringRequest);
    }

    public static boolean shouldGetNews(Context context) {
        return checkWifiOnAndConnected(context)
                || PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean("allow-mobile-data", false);
    }

    private static boolean checkWifiOnAndConnected(Context context) {
        final WifiManager wifiMgr = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (wifiMgr.isWifiEnabled()) { // Wi-Fi adapter is ON
            final WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
            return wifiInfo.getNetworkId() != -1;
        } else {
            return false; // Wi-Fi adapter is OFF
        }
    }

    @Override
    public void onSyncCanceled() {
        super.onSyncCanceled();
    }

    private static class MyResponseListener implements Response.Listener<String> {
        private String source;
        private Context context;
        private ExecutorService executorService;

        MyResponseListener(Context context, String source) {
            this.context = context;
            this.source = source;
            executorService = Executors.newFixedThreadPool(30);
        }

        @Override
        @DebugLog
        public void onResponse(String response) {
            try {
                final JSONObject respJSON = new JSONObject(response);
                final JSONArray array = respJSON.getJSONArray("articles");
                final ArrayList<String> titles = new ArrayList<>();
                context.getContentResolver().delete(
                        CONTENT_URI, SOURCE + " = ? ", new String[]{source});
                for (int i = 0; i < array.length(); i++) {
                    final JSONObject jsonArticle = array.getJSONObject(i);
                    if (jsonArticle.get("title").equals(JSONObject.NULL)
                            || jsonArticle.get("urlToImage").equals(JSONObject.NULL)
                            || jsonArticle.get("url").equals(JSONObject.NULL)
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
                    executorService.submit(new Runnable() {
                        @Override
                        public void run() {
                            Log.d("nano", "IIIIIIIIIIIII");
                            context.getContentResolver().insert(CONTENT_URI, contentValues);
                            Log.d("nano", "-------IIIIIIIIIIIII");
                        }
                    });
                }
            } catch (JSONException e) {
                Log.e("nano", "json error: " + e);
            }
        }
    }

    private static class MyErrorListener implements Response.ErrorListener {
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
