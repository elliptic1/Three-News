package com.tbse.threenews.mysyncadapter;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;


public class MySyncAdapter extends AbstractThreadedSyncAdapter {

    private ContentResolver contentResolver;

    public MySyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        Log.d("nano", "MySyncAdapter init");
        contentResolver = context.getContentResolver();
    }

    @Override
    public void onPerformSync(Account account, Bundle extras,
                              String authority, ContentProviderClient provider,
                              SyncResult syncResult) {
        Log.d("nano", "MySyncAdapter onPerformSync");
        final JSONObject paramsObject = new JSONObject();
        try {
            paramsObject.put("source", "cnn");
            paramsObject.put("apiKey", getContext().getString(R.string.newsapikey));
            paramsObject.put("sortBy", "latest");
        } catch (JSONException ignore) {
            Log.e("nano", "couldn't make json req obj");
        }
        final JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, getContext().getString(R.string.apiurl),
                        paramsObject, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d("nano", "got a response: " + response);
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("nano", "got an error: " + error);
                    }
                });
        jsonObjectRequest.setTag(this);

        final RequestQueue queue = Volley.newRequestQueue(getContext());
        Log.d("nano", "adding request " + jsonObjectRequest.getBody());
        queue.add(jsonObjectRequest);
    }

    @Override
    public void onSyncCanceled() {
        super.onSyncCanceled();
    }
}
