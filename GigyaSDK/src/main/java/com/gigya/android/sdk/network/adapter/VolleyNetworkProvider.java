package com.gigya.android.sdk.network.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.api.GigyaApiRequest;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.utils.UrlUtils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

public class VolleyNetworkProvider extends NetworkProvider {

    private static final String LOG_TAG = "VolleyNetworkProvider";

    private RequestQueue _requestQueue;
    private Queue<VolleyNetworkRequest> _blockedQueue = new ConcurrentLinkedQueue<>();

    VolleyNetworkProvider(Context appContext) {
        _requestQueue = Volley.newRequestQueue(appContext);
        // Enable Volley logs.
        VolleyLog.DEBUG = GigyaLogger.isDebug();
    }

    public static boolean isAvailable() {
        try {
            Class.forName("com.android.volley.toolbox.Volley");
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public void addToQueue(GigyaApiRequest request, IRestAdapterCallback networkCallbacks) {
        _requestQueue.getCache().clear();
        VolleyNetworkRequest newRequest = newRequest(request, networkCallbacks);
        if (_blocked) {
            GigyaLogger.debug(LOG_TAG, "addToQueue: is blocked. adding to blocked queued - " + request.getUrl());
            _blockedQueue.add(newRequest);
            return;
        }
        if (!_blockedQueue.isEmpty()) {
            GigyaLogger.debug(LOG_TAG, "addToQueue: blockedQueue is empty releasing it - " + request.getUrl());
            release();
        }

        GigyaLogger.debug(LOG_TAG, "addToQueue: adding to queue - " + request.getUrl());
        _requestQueue.add(newRequest);
    }

    @Override
    public void sendBlocking(GigyaApiRequest request, IRestAdapterCallback networkCallbacks) {
        GigyaLogger.debug(LOG_TAG, "sendBlocking: " + request.getUrl());
        _requestQueue.getCache().clear();
        VolleyNetworkRequest newRequest = newRequest(request, networkCallbacks);
        _requestQueue.add(newRequest);
        _blocked = true;
    }

    @Override
    public void release() {
        super.release();
        if (_blockedQueue.isEmpty()) {
            return;
        }

        // Traverse over blocked queue and release all.
        while (!_blockedQueue.isEmpty()) {
            final VolleyNetworkRequest queued = _blockedQueue.poll();
            if (queued != null) {
                GigyaLogger.debug(LOG_TAG, "release: polled request  - " + queued.getUrl());
                _requestQueue.add(queued);
            }
        }
    }

    @Override
    public void cancel(String tag) {
        if (tag == null) {
            // Cancel all.
            _requestQueue.cancelAll(new RequestQueue.RequestFilter() {

                @Override
                public boolean apply(Request<?> request) {
                    return true;
                }
            });
            _blockedQueue.clear();
            return;
        }
        _requestQueue.cancelAll(tag);
        if (!_blockedQueue.isEmpty()) {
            Iterator it = _blockedQueue.iterator();
            while (it.hasNext()) {
                final VolleyNetworkRequest request = (VolleyNetworkRequest) it.next();
                final String requestTag = (String) request.getTag();
                if (requestTag.equals(tag)) {
                    it.remove();
                }
            }
        }
    }

    //region VOLLEY SPECIFIC IMPLEMENTATION

    /*
    Generate a new Volley request.
     */
    private VolleyNetworkRequest newRequest(final GigyaApiRequest request, final IRestAdapterCallback networkCallbacks) {
        return new VolleyNetworkRequest(request.getMethod(), request.getUrl(),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        GigyaLogger.debug("GigyaApiResponse", "ApiService: " + request.getUrl() + "\n" + response);
                        if (networkCallbacks != null) {
                            networkCallbacks.onResponse(response);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        int errorCode = 0;
                        if (error.networkResponse != null) {
                            errorCode = error.networkResponse.statusCode;
                        }
                        final String localizedMessage = error.getLocalizedMessage() == null ? "" : error.getLocalizedMessage();
                        final GigyaError gigyaError = new GigyaError(errorCode, localizedMessage, null);
                        GigyaLogger.debug("GigyaApiResponse", "GigyaApiResponse: Error " +
                                "ApiService: " + request.getUrl() + "\n" +
                                gigyaError.toString());
                        if (networkCallbacks != null) {
                            networkCallbacks.onError(gigyaError);
                        }
                    }
                }
                , request.getEncodedParams()
                , request.getTag()
        );
    }

    private static class VolleyNetworkRequest extends StringRequest {

        @Nullable
        private String body;

        VolleyNetworkRequest(int method,
                             String url,
                             @NonNull Response.Listener<String> listener,
                             @NonNull Response.ErrorListener errorListener,
                             @Nullable String body,
                             String tag) {
            super(method, url, listener, errorListener);
            setTag(tag);
            this.body = body;
            setShouldCache(false);
            setRetryPolicy(new DefaultRetryPolicy(
                    (int) TimeUnit.SECONDS.toMillis(30), //After the set time elapses the request will timeout
                    0,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        }

        @Override
        public Map<String, String> getHeaders() {
            Map<String, String> headers = new HashMap<>();
            headers.put("Accept-Encoding", "gzip, deflate");
            headers.put("connection", "close");
            return headers;
        }

        @Override
        public byte[] getBody() throws AuthFailureError {
            if (body != null) {
                return this.body.getBytes();
            }
            return super.getBody();
        }

        @Override
        protected Response<String> parseNetworkResponse(NetworkResponse response) {
            String jsonString;
            try {
                final String encoding = response.headers.get("Content-Encoding");
                if (encoding != null && encoding.equals("gzip")) {
                    // Response contains GZIP encoding.
                    jsonString = UrlUtils.gzipDecode(response.data);
                } else {
                    jsonString = new String(
                            response.data,
                            HttpHeaderParser.parseCharset(response.headers, "utf-8"));
                }
                return Response.success(
                        jsonString, HttpHeaderParser.parseCacheHeaders(response));
            } catch (Exception e) {
                return Response.error(new ParseError(e));
            }
        }
    }

    //endregion

}
