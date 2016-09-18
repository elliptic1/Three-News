package com.tbse.threenews.mysyncadapter;

import com.android.volley.toolbox.HurlStack;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;

/**
 * Created by todd on 9/18/16.
 */

public class ProxiedHurlStack extends HurlStack {
    @Override
    protected HttpURLConnection createConnection(URL url) throws IOException {

        // Start the connection by specifying a proxy server
        final Proxy proxy = new Proxy(Proxy.Type.HTTP,
                InetSocketAddress.createUnresolved("192.168.1.72", 8888));
        final HttpURLConnection returnThis = (HttpURLConnection) url.openConnection(proxy);

        return returnThis;
    }
}
