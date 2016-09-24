package com.tbse.threenews.mysyncadapter;

import android.accounts.Account;
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

import static com.tbse.threenews.mysyncadapter.MyContentProvider.CONTENT_URI;
import static com.tbse.threenews.mysyncadapter.MyContentProvider.DATE;
import static com.tbse.threenews.mysyncadapter.MyContentProvider.HEADLINE;
import static com.tbse.threenews.mysyncadapter.MyContentProvider.IMG;
import static com.tbse.threenews.mysyncadapter.MyContentProvider.LINK;
import static com.tbse.threenews.mysyncadapter.MyContentProvider.PRIORITY;


public class MySyncAdapter extends AbstractThreadedSyncAdapter {

    private ContentResolver contentResolver;
    private String apiKey;

    public MySyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        Log.d("nano", "MySyncAdapter init");
        contentResolver = context.getContentResolver();
        apiKey = getContext().getString(R.string.newsapikey);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras,
                              String authority, ContentProviderClient provider,
                              SyncResult syncResult) {
        Log.d("nano", "MySyncAdapter onPerformSync");
        final JSONObject paramsObject = new JSONObject();
        try {
            paramsObject.put("source", "techcrunch");
            paramsObject.put("apiKey", apiKey);
            paramsObject.put("sortBy", "latest");

            Log.d("nano", "params is " + paramsObject);
        } catch (JSONException ignore) {
            Log.e("nano", "couldn't make json req obj");
        }
        StringRequest stringRequest = new StringRequest(Request.Method.GET,
                getContext().getString(R.string.apiurl)
                + "?source=cnn&apiKey="
                + getContext().getString(R.string.newsapikey)
                + "&sortBy=top",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d("nano", "got a response: " + response);
                        try {
                            final JSONObject respJSON = new JSONObject(response);
                            Log.d("nano", "got a json: " + respJSON);
                            final JSONArray array = respJSON.getJSONArray("articles");
                            for (int i=0; i<array.length(); i++) {
                                final JSONObject jsonArticle = array.getJSONObject(i);
                                Log.d("nano", "title: " + jsonArticle.getString("title"));
                                Log.d("nano", " - desc: " + jsonArticle.getString("description"));
                                Log.d("nano", " - url: " + jsonArticle.getString("url"));
                                Log.d("nano", " - imageUrl: " + jsonArticle.getString("urlToImage"));
                                Log.d("nano", " - pubAt: " + jsonArticle.getString("publishedAt"));
                                final ContentValues contentValues = new ContentValues();
                                contentValues.put(IMG, jsonArticle.getString("urlToImage"));
                                contentValues.put(HEADLINE, jsonArticle.getString("title"));
                                contentValues.put(LINK, jsonArticle.getString("url"));
                                contentValues.put(DATE, jsonArticle.getString("publishedAt"));
                                contentValues.put(PRIORITY, 1);
                                contentResolver.insert(CONTENT_URI, contentValues);
                            }
                            Log.d("nano", " articles count " + array.length());
                        } catch (JSONException e) {
                            Log.e("nano", "json error: " + e);
                        }
                    }
                }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("nano", "got an error: " + error);
            }
        });
        stringRequest.setTag(this);

        final RequestQueue queue = Volley.newRequestQueue(getContext());
        Log.d("nano", "adding request " + stringRequest);
        queue.add(stringRequest);
    }

    @Override
    public void onSyncCanceled() {
        super.onSyncCanceled();
    }
}
