package com.tbse.threenews.mysyncadapter;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

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
        contentResolver = context.getContentResolver();
    }

    @Override
    public void onPerformSync(Account account, Bundle extras,
                              String authority, ContentProviderClient provider,
                              SyncResult syncResult) {
        final String url = getContext().getString(
                getContext().getResources().getIdentifier(
                        "apiurl", "string", "com.tbse.threenews.mysyncadapter"));
        Log.d("nano", "url is " + url);
        final JSONObject requestObject = new JSONObject();
        try {
            requestObject.put("source", "cnn");
            final String key =
                    getContext().getString(
                            getContext().getResources().getIdentifier(
                                    "newsapikey", "string", "com.tbse.threenews.mysyncadapter"));
            requestObject.put("apiKey", key);
            Log.d("nano", "key is " + key);
        } catch (JSONException ignore) {

        }
        final JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        Toast.makeText(getContext(), "resp", Toast.LENGTH_LONG).show();
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO Auto-generated method stub

                    }
                });

        final RequestQueue queue = Volley.newRequestQueue(getContext());
        queue.add(jsonObjectRequest);
    }

    @Override
    public void onSyncCanceled() {
        super.onSyncCanceled();
    }
}
