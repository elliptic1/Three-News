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
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
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

    MySyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        queue = Volley.newRequestQueue(getContext());
        sourceToName = getHashMapFromStringArrayIds(R.array.newssources, R.array.newssourcesnames);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras,
                              String authority, ContentProviderClient provider,
                              SyncResult syncResult) {
        if (!shouldGetNews(getContext())) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getContext(),
                            R.string.cantupdateondata, Toast.LENGTH_LONG).show();
                }
            });
            return;
        }

        getContext().getContentResolver().delete(CONTENT_URI, null, null);

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        startRequestForSource(getContext(), prefs.getString(getContext().getString(R.string.news_source), "cnn"));
    }

    public static void startRequestForSource(Context context, String source) {
        final StringRequest stringRequest = new StringRequest(Request.Method.GET,
                context.getString(R.string.apiurl, source,
                        context.getString(R.string.newsapikey)),
                new MyResponseListener(context, source), new MyErrorListener());
        stringRequest.setTag(context);
        queue.add(stringRequest);
    }

    public static boolean shouldGetNews(Context context) {
        return checkWifiOnAndConnected(context)
                || PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(context.getString(R.string.pref_allow_mobile_data), false);
    }

    private static boolean checkWifiOnAndConnected(Context context) {
        // Use ConnectivityManager transport checks rather than the deprecated
        // WifiManager.getConnectionInfo().getNetworkId(), which modern Android
        // redacts to -1 unless the app holds location permission (which it does
        // not). Only ACCESS_NETWORK_STATE is required here.
        final ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            final Network network = cm.getActiveNetwork();
            if (network == null) {
                return false;
            }
            final NetworkCapabilities caps = cm.getNetworkCapabilities(network);
            return caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
        } else {
            final NetworkInfo info = cm.getActiveNetworkInfo();
            return info != null && info.isConnected()
                    && info.getType() == ConnectivityManager.TYPE_WIFI;
        }
    }

    @Override
    public void onSyncCanceled() {
        super.onSyncCanceled();
    }

    private static class MyResponseListener implements Response.Listener<String> {
        private String source;
        private Context context;

        MyResponseListener(Context context, String source) {
            this.context = context;
            this.source = source;
        }

        @Override
        public void onResponse(String response) {
            try {
                final JSONObject respJSON = new JSONObject(response);
                final JSONArray array = respJSON.getJSONArray(context.getString(R.string.articles));
                final ArrayList<String> titles = new ArrayList<>();
                Log.d("nano", "responded with " + array.length() + " articles");
                context.getContentResolver().delete(CONTENT_URI, null, null);

                for (int i = 0; i < array.length(); i++) {
                    final JSONObject jsonArticle = array.getJSONObject(i);
                    if (jsonArticle.get(context.getString(R.string.json_title)).equals(JSONObject.NULL)
                            || jsonArticle.get(context.getString(R.string.json_title)).equals(JSONObject.NULL)
                            || jsonArticle.get(context.getString(R.string.cv_url_link)).equals(JSONObject.NULL)
                            || jsonArticle.get(context.getString(R.string.cv_pub_at)).equals(JSONObject.NULL)
                            ) {
                        continue;
                    }
                    final String title = jsonArticle.getString(context.getString(R.string.json_title));
                    if (titles.contains(title) || title.length() < 1) {
                        continue;
                    }
                    titles.add(title);
                    final ContentValues contentValues = new ContentValues();
                    contentValues.put(IMG, jsonArticle.getString(context.getString(R.string.cv_url_image)));
                    contentValues.put(HEADLINE, title);
                    contentValues.put(SOURCE, source);
                    contentValues.put(LINK, jsonArticle.getString(context.getString(R.string.cv_url_link)));
                    final DateTime dateTime = new DateTime(jsonArticle.get(context.getString(R.string.cv_pub_at)));
                    contentValues.put(DATE, dateTime.getMillis() / 1000);
                    context.getContentResolver().insert(CONTENT_URI, contentValues);
                }
            } catch (JSONException e) {
                Log.e("nano", "failed to parse news response", e);
            }
        }
    }

    private static class MyErrorListener implements Response.ErrorListener {
        @Override
        public void onErrorResponse(VolleyError error) {
            Log.e("nano", "got an error: " + error);
        }
    }

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

    private HashMap<String, String> getHashMapFromStringArrayIds(final int a, final int b) {
        final String[] sources = getContext().getResources().getStringArray(a);
        final String[] sourcesnames = getContext().getResources().getStringArray(b);
        sourceToName = new HashMap<>();
        for (int i = 0; i < sources.length; i++) {
            sourceToName.put(sources[i], sourcesnames[i]);
        }
        return sourceToName;
    }
}
