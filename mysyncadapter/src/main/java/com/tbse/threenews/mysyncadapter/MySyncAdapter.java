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
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import static android.content.Context.ACCOUNT_SERVICE;
import static com.tbse.threenews.mysyncadapter.MyContentProvider.CONTENT_URI;
import static com.tbse.threenews.mysyncadapter.MyContentProvider.DATE;
import static com.tbse.threenews.mysyncadapter.MyContentProvider.HEADLINE;
import static com.tbse.threenews.mysyncadapter.MyContentProvider.LINK;
import static com.tbse.threenews.mysyncadapter.MyContentProvider.SOURCE;
import static com.tbse.threenews.mysyncadapter.NewsAlarmManager.ACCOUNT;
import static com.tbse.threenews.mysyncadapter.NewsAlarmManager.ACCOUNT_TYPE;


public class MySyncAdapter extends AbstractThreadedSyncAdapter {

    private static RequestQueue queue;
    public static HashMap<String, String> sourceToName;

    // Google News RSS publishes RFC-822 dates, e.g. "Tue, 24 Jun 2025 18:30:00 GMT".
    private static final DateTimeFormatter RSS_DATE =
            DateTimeFormat.forPattern("EEE, dd MMM yyyy HH:mm:ss zzz").withLocale(Locale.ENGLISH);

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
        startRequestForSource(getContext(), prefs.getString(getContext().getString(R.string.news_source), "cnn.com"));
    }

    public static void startRequestForSource(Context context, String source) {
        final String url;
        if (context.getString(R.string.rss_top_source).equals(source)) {
            url = context.getString(R.string.rss_top_url);
        } else {
            url = context.getString(R.string.apiurl, source);
        }
        final StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
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
                final XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(false);
                final XmlPullParser parser = factory.newPullParser();
                parser.setInput(new StringReader(response));

                final ArrayList<String> titles = new ArrayList<>();
                context.getContentResolver().delete(CONTENT_URI, null, null);

                boolean inItem = false;
                String title = null, link = null, pubDate = null;
                int event = parser.getEventType();
                while (event != XmlPullParser.END_DOCUMENT) {
                    if (event == XmlPullParser.START_TAG) {
                        final String name = parser.getName();
                        if ("item".equals(name)) {
                            inItem = true;
                            title = link = pubDate = null;
                        } else if (inItem && "title".equals(name)) {
                            title = parser.nextText();
                        } else if (inItem && "link".equals(name)) {
                            link = parser.nextText();
                        } else if (inItem && "pubDate".equals(name)) {
                            pubDate = parser.nextText();
                        }
                    } else if (event == XmlPullParser.END_TAG && "item".equals(parser.getName())) {
                        inItem = false;
                        title = stripPublisherSuffix(title);
                        if (title == null || link == null || title.length() < 1 || titles.contains(title)) {
                            event = parser.next();
                            continue;
                        }
                        titles.add(title);
                        final ContentValues contentValues = new ContentValues();
                        // Google News RSS items carry no image; leave IMG null so the UI shows its placeholder.
                        contentValues.put(HEADLINE, title);
                        contentValues.put(SOURCE, source);
                        contentValues.put(LINK, link);
                        contentValues.put(DATE, parsePubDateSeconds(pubDate));
                        context.getContentResolver().insert(CONTENT_URI, contentValues);
                    }
                    event = parser.next();
                }
                Log.d("nano", "parsed " + titles.size() + " articles from Google News RSS");
            } catch (Exception e) {
                Log.e("nano", "failed to parse news response", e);
            }
        }

        // Google News titles end with " - Publisher"; drop it since the source is shown separately.
        private static String stripPublisherSuffix(String title) {
            if (title == null) {
                return null;
            }
            final int idx = title.lastIndexOf(" - ");
            return idx > 0 ? title.substring(0, idx) : title;
        }

        private static long parsePubDateSeconds(String pubDate) {
            try {
                return RSS_DATE.parseDateTime(pubDate).getMillis() / 1000;
            } catch (Exception e) {
                return new DateTime().getMillis() / 1000;
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
