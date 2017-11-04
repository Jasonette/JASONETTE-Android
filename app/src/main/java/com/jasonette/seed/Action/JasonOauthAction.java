package com.jasonette.seed.Action;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.builder.api.DefaultApi10a;
import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.model.OAuth1AccessToken;
import com.github.scribejava.core.model.OAuth1RequestToken;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth10aService;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.jasonette.seed.Helper.JasonHelper;
import com.jasonette.seed.Launcher.Launcher;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import okhttp3.Authenticator;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Route;

public class JasonOauthAction {
    public void auth(final JSONObject action, final JSONObject data, final JSONObject event, final Context context) {
        try {
            final JSONObject options = action.getJSONObject("options");

            if(options.has("version") && options.getString("version").equals("1")) {
                //
                //OAuth 1
                //
                JSONObject request_options = options.getJSONObject("request");
                JSONObject authorize_options = options.getJSONObject("authorize");

                String client_id = request_options.getString("client_id");
                String client_secret = request_options.getString("client_secret");

                if(!request_options.has("scheme") || request_options.getString("scheme").length() == 0
                    || !request_options.has("host") || request_options.getString("host").length() == 0
                    || !request_options.has("path") || request_options.getString("path").length() == 0
                    || !authorize_options.has("scheme") || authorize_options.getString("scheme").length() == 0
                    || !authorize_options.has("host") || authorize_options.getString("host").length() == 0
                    || !authorize_options.has("path") || authorize_options.getString("path").length() == 0
                ) {
                    JasonHelper.next("error", action, data, event, context);
                } else {
                    JSONObject request_options_data = request_options.getJSONObject("data");

                    Uri.Builder uriBuilder = new Uri.Builder();
                    uriBuilder.scheme(request_options.getString("scheme"))
                            .encodedAuthority(request_options.getString("host"))
                            .encodedPath(request_options.getString("path"));
                    final String requestUri = uriBuilder.build().toString();

                    final Uri.Builder authorizeUriBuilder = new Uri.Builder();
                    authorizeUriBuilder.scheme(authorize_options.getString("scheme"))
                            .encodedAuthority(authorize_options.getString("host"))
                            .encodedPath(authorize_options.getString("path"));

                    String callback_uri = request_options_data.getString("oauth_callback");

                    DefaultApi10a oauthApi = new DefaultApi10a() {
                        @Override
                        public String getRequestTokenEndpoint() {
                            return requestUri;
                        }

                        @Override
                        public String getAccessTokenEndpoint() {
                            return null;
                        }

                        @Override
                        public String getAuthorizationUrl(OAuth1RequestToken requestToken) {
                            return authorizeUriBuilder
                                    .appendQueryParameter("oauth_token", requestToken.getToken())
                                    .build().toString();
                        }
                    };

                    final OAuth10aService oauthService = new ServiceBuilder()
                            .apiKey(client_id)
                            .apiSecret(client_secret)
                            .callback(callback_uri)
                            .build(oauthApi);

                    new AsyncTask<String, Void, Void>() {
                        @Override
                        protected Void doInBackground(String... params) {
                            try {
                                String client_id = params[0];

                                OAuth1RequestToken request_token = oauthService.getRequestToken();

                                SharedPreferences preferences = context.getSharedPreferences("oauth", Context.MODE_PRIVATE);
                                preferences.edit().putString(client_id + "_request_token_secret", request_token.getTokenSecret()).apply();

                                String auth_url = oauthService.getAuthorizationUrl(request_token);

                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                intent.setData(Uri.parse(auth_url));

                                JSONObject callback = new JSONObject();
                                callback.put("class", "JasonOauthAction");
                                callback.put("method", "oauth_callback");
                                JasonHelper.dispatchIntent("oauth", action, data, event, context, intent, callback);
                            } catch(Exception e) {
                                handleError(e, action, event, context);
                            }
                            return null;
                        }
                    }.execute(client_id);
                }
            } else {
                //
                //OAuth 2
                //
                JSONObject authorize_options = options.getJSONObject("authorize");

                JSONObject authorize_options_data = new JSONObject();
                if(authorize_options.has("data")) {
                    authorize_options_data = authorize_options.getJSONObject("data");
                }

                if(authorize_options_data.has("grant_type") && authorize_options_data.getString("grant_type").equals("password")) {
                    String client_id = authorize_options.getString("client_id");
                    String client_secret = "";

                    if(authorize_options.has("client_secret")) {
                        client_secret = authorize_options.getString("client_secret");
                    }

                    if(!authorize_options.has("scheme") || authorize_options.getString("scheme").length() == 0
                        || !authorize_options.has("host") || authorize_options.getString("host").length() == 0
                        || !authorize_options.has("path") || authorize_options.getString("path").length() == 0
                        || !authorize_options_data.has("username") || authorize_options_data.getString("username").length() == 0
                        || !authorize_options_data.has("password") || authorize_options_data.getString("password").length() == 0
                    ) {
                        JasonHelper.next("error", action, data, event, context);
                    } else {
                        String username = authorize_options_data.getString("username");
                        String password = authorize_options_data.getString("password");

                        Uri.Builder builder = new Uri.Builder();

                        builder.scheme(authorize_options.getString("scheme"))
                            .encodedAuthority(authorize_options.getString("host"))
                            .encodedPath(authorize_options.getString("path"));

                        final Uri uri = builder.build();

                        DefaultApi20 oauthApi = new DefaultApi20() {
                            @Override
                            public String getAccessTokenEndpoint() {
                                return uri.toString();
                            }

                            @Override
                            protected String getAuthorizationBaseUrl() {
                                return null;
                            }
                        };

                        ServiceBuilder serviceBuilder = new ServiceBuilder();
                        serviceBuilder.apiKey(client_id);
                        if(client_secret.length() > 0) {
                            serviceBuilder.apiSecret(client_secret);
                        }
                        if(authorize_options_data.has("scope") && authorize_options_data.getString("scope").length() > 0) {
                            serviceBuilder.scope(authorize_options_data.getString("scope"));
                        }
                        if(authorize_options_data.has("state") && authorize_options_data.getString("state").length() > 0) {
                            serviceBuilder.state(authorize_options_data.getString("state"));
                        }

                        final OAuth20Service oauthService = serviceBuilder.build(oauthApi);

                        Map<String, String> additionalParams = new HashMap<>();

                        Iterator paramKeys = authorize_options_data.keys();
                        while(paramKeys.hasNext()) {
                            String key = (String)paramKeys.next();
                            if(key != "redirect_uri" && key != "response_type" && key != "scope" && key != "state") {
                                String value = authorize_options_data.getString(key);
                                additionalParams.put(key, value);
                            }
                        }

                        new AsyncTask<String, Void, Void>() {
                            @Override
                            protected Void doInBackground(String... params) {
                                try {
                                    String username = params[0];
                                    String password = params[1];
                                    String client_id = params[2];

                                    String access_token = oauthService.getAccessTokenPasswordGrant(username, password).getAccessToken();

                                    SharedPreferences preferences = context.getSharedPreferences("oauth", Context.MODE_PRIVATE);
                                    preferences.edit().putString(client_id, access_token).apply();

                                    JSONObject result = new JSONObject();
                                    try {
                                        result.put("token", access_token);
                                        JasonHelper.next("success", action, result, event, context);
                                    } catch(JSONException e) {
                                        handleError(e, action, event, context);
                                    }
                                } catch(Exception e) {
                                    handleError(e, action, event, context);
                                }
                                return null;
                            }
                        }.execute(username, password, client_id);
                    }
                } else {
                    //
                    //Assuming code auth
                    //
                    String client_id = authorize_options.getString("client_id");

                    SharedPreferences sharedPreferences = context.getSharedPreferences("oauth", Context.MODE_PRIVATE);

                    if(
                        sharedPreferences.contains(client_id) &&
                        sharedPreferences.contains(client_id + "_refresh_token") &&
                        (
                            sharedPreferences.contains(client_id + "_expires_in") &&
                            (sharedPreferences.getInt(client_id + "_created_at", 0) + sharedPreferences.getInt(client_id + "_expires_in", 0))
                                    <
                            (int)(System.currentTimeMillis() / 1000)
                        )
                    ) {
                        request_oauth20_access_token(action, data, event, context, null, sharedPreferences.getString(client_id + "_refresh_token", null));
                    } else {
                        if(authorize_options.has("data")) {
                            authorize_options_data = authorize_options.getJSONObject("data");

                            if(authorize_options.length() == 0) {
                                JasonHelper.next("error", action, data, event, context);
                            } else {
                                String client_secret = "";
                                String redirect_uri = "";

                                //Secret can be missing in implicit authentication
                                if(authorize_options.has("client_secret")) {
                                    client_secret = authorize_options.getString("client_secret");
                                }
                                if(authorize_options_data.has("redirect_uri")) {
                                    redirect_uri = authorize_options_data.getString("redirect_uri");
                                }

                                if(!authorize_options.has("scheme") || authorize_options.getString("scheme").length() == 0
                                        || !authorize_options.has("host") || authorize_options.getString("host").length() == 0
                                        || !authorize_options.has("path") || authorize_options.getString("path").length() == 0
                                        ) {
                                    JasonHelper.next("error", action, data, event, context);
                                } else {
                                    Uri.Builder builder = new Uri.Builder();

                                    builder.scheme(authorize_options.getString("scheme"))
                                            .encodedAuthority(authorize_options.getString("host"))
                                            .encodedPath(authorize_options.getString("path"));

                                    final Uri uri = builder.build();

                                    DefaultApi20 oauthApi = new DefaultApi20() {
                                        @Override
                                        public String getAccessTokenEndpoint() {
                                            return null;
                                        }

                                        @Override
                                        protected String getAuthorizationBaseUrl() {
                                            return uri.toString();
                                        }
                                    };

                                    ServiceBuilder serviceBuilder = new ServiceBuilder();
                                    serviceBuilder.apiKey(client_id);

                                    if(client_secret.length() > 0) {
                                        serviceBuilder.apiSecret(client_secret);
                                    }
                                    if(authorize_options_data.has("scope") && authorize_options_data.getString("scope").length() > 0) {
                                        serviceBuilder.scope(authorize_options_data.getString("scope"));
                                    }
                                    if(authorize_options_data.has("state") && authorize_options_data.getString("state").length() > 0) {
                                        serviceBuilder.state(authorize_options_data.getString("state"));
                                    }
                                    serviceBuilder.callback(redirect_uri);

                                    OAuth20Service oauthService = serviceBuilder.build(oauthApi);

                                    Map<String, String> additionalParams = new HashMap<>();

                                    Iterator paramKeys = authorize_options_data.keys();
                                    while(paramKeys.hasNext()) {
                                        String key = (String)paramKeys.next();
                                        if(key != "redirect_uri" && key != "response_type" && key != "scope" && key != "state") {
                                            String value = authorize_options_data.getString(key);
                                            additionalParams.put(key, value);
                                        }
                                    }

                                    Intent intent = new Intent(Intent.ACTION_VIEW);
                                    intent.setData(Uri.parse(oauthService.getAuthorizationUrl(additionalParams)));

                                    JSONObject callback = new JSONObject();
                                    callback.put("class", "JasonOauthAction");
                                    callback.put("method", "oauth_callback");
                                    JasonHelper.dispatchIntent("oauth", action, data, event, context, intent, callback);
                                }
                            }

                        } else {
                            JSONObject error = new JSONObject();
                            error.put("data", "Authorize data missing");
                            JasonHelper.next("error", action, error, event, context);
                        }
                    }
                }
            }
        } catch(JSONException e)  {
            handleError(e, action, event, context);
        }
    }

    public void access_token(final JSONObject action, final JSONObject data, final JSONObject event, final Context context) {
        try {
            final JSONObject options = action.getJSONObject("options");

            String client_id = options.getJSONObject("access").getString("client_id");

            SharedPreferences sharedPreferences = context.getSharedPreferences("oauth", Context.MODE_PRIVATE);
            String access_token = sharedPreferences.getString(client_id, null);
            if(access_token != null) {
                JSONObject result = new JSONObject();
                result.put("token", access_token);
                JasonHelper.next("success", action, result, event, context);
            } else {
                JSONObject error = new JSONObject();
                error.put("data", "access token not found");
                JasonHelper.next("error", action, error, event, context);
            }
        } catch(JSONException e) {
            handleError(e, action, event, context);
        }
    }

    public void oauth_callback(Intent intent, final JSONObject intent_options) {
        try {
            final JSONObject action = intent_options.getJSONObject("action");
            final JSONObject data = intent_options.getJSONObject("data");
            final JSONObject event = intent_options.getJSONObject("event");
            final Context context = (Context)intent_options.get("context");

            JSONObject options = action.getJSONObject("options");

            Uri uri = intent.getData();

            if (options.has("version") && options.getString("version").equals("1")) {
                //OAuth 1
                String oauth_token = uri.getQueryParameter("oauth_token");
                String oauth_verifier = uri.getQueryParameter("oauth_verifier");

                JSONObject access_options = options.getJSONObject("access");

                if(
                    oauth_token.length() > 0  && oauth_verifier.length() > 0
                    && access_options.has("scheme") && access_options.getString("scheme").length() > 0
                    && access_options.has("host") && access_options.getString("host").length() > 0
                    && access_options.has("path") && access_options.getString("path").length() > 0
                    && access_options.has("path") && access_options.getString("path").length() > 0
                    && access_options.has("client_id") && access_options.getString("client_id").length() > 0
                    && access_options.has("client_secret") && access_options.getString("client_secret").length() > 0
                ) {
                    String client_id = access_options.getString("client_id");
                    String client_secret = access_options.getString("client_secret");

                    Uri.Builder uriBuilder = new Uri.Builder();
                    uriBuilder.scheme(access_options.getString("scheme"))
                        .encodedAuthority(access_options.getString("host"))
                        .encodedPath(access_options.getString("path"));

                    final String accessUri = uriBuilder.build().toString();

                    DefaultApi10a oauthApi = new DefaultApi10a() {
                        @Override
                        public String getAuthorizationUrl(OAuth1RequestToken requestToken) { return null; }

                        @Override
                        public String getRequestTokenEndpoint() { return null; }

                        @Override
                        public String getAccessTokenEndpoint() {
                            return accessUri.toString();
                        }
                    };

                    final OAuth10aService oauthService = new ServiceBuilder()
                            .apiKey(client_id)
                            .apiSecret(client_secret)
                            .build(oauthApi);

                    new AsyncTask<String, Void, Void>() {
                        @Override
                        protected Void doInBackground(String... params) {
                            try {
                                SharedPreferences preferences = context.getSharedPreferences("oauth", Context.MODE_PRIVATE);

                                String string_oauth_token = params[0];
                                String oauth_verifier = params[1];
                                String client_id = params[2];
                                String oauth_token_secret = preferences.getString(client_id + "_request_token_secret", null);

                                OAuth1RequestToken oauthToken = new OAuth1RequestToken(string_oauth_token, oauth_token_secret);

                                OAuth1AccessToken access_token = oauthService.getAccessToken(oauthToken, oauth_verifier);

                                preferences.edit().putString(client_id, access_token.getToken()).apply();
                                preferences.edit().putString(client_id + "_access_token_secret", access_token.getTokenSecret()).apply();

                                JSONObject result = new JSONObject();
                                result.put("token", access_token.getToken());

                                JasonHelper.next("success", action, result, event, context);
                            } catch(Exception e) {
                                handleError(e, action, event, context);
                            }

                            return null;
                        }
                    }.execute(oauth_token, oauth_verifier, client_id);

                } else {
                    JasonHelper.next("error", action, data, event, context);
                }
            } else {
                // OAuth 2
                String access_token = uri.getQueryParameter("access_token"); // get access token from url here

                JSONObject authorize_options = options.getJSONObject("authorize");

                if (access_token != null && access_token.length() > 0) {
                    String client_id = authorize_options.getString("client_id");

                    SharedPreferences preferences = context.getSharedPreferences("oauth", Context.MODE_PRIVATE);
                    preferences.edit().putString(client_id, access_token).apply();

                    JSONObject result = new JSONObject();
                    result.put("token", access_token);

                    JasonHelper.next("success", action, result, event, context);
                } else {
                    String code = uri.getQueryParameter("code");

                    request_oauth20_access_token(action, data, event, context, code, null);
                }
            }
        }
        catch(JSONException err) {
            try {
                JSONObject error = new JSONObject();
                error.put("data", err.toString());

                JasonHelper.next("error", intent_options.getJSONObject("action"), error, intent_options.getJSONObject("event"), (Context)intent_options.get("context"));
            } catch(JSONException e) {
                Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
            }
        }
    }

    private void request_oauth20_access_token(final JSONObject action, final JSONObject data, final JSONObject event, final Context context, String code, String refresh_token) {
        try {
            JSONObject options = action.getJSONObject("options");

            JSONObject access_options = options.getJSONObject("access");
            JSONObject access_options_data = access_options.has("data") ? access_options.getJSONObject("data") : new JSONObject();

            final String client_id = access_options.getString("client_id");
            //
            // also in access_options_data
            //
            final String client_secret = access_options.has("client_secret") ? access_options.getString("client_secret") : "";

            String redirect_uri = access_options_data.has("redirect_uri") ? access_options_data.getString("redirect_uri") : "";

            String grant_type;
            if(refresh_token != null) {
                grant_type = "refresh_token";
            } else {
                grant_type = access_options_data.has("grant_type") ? access_options_data.getString("grant_type") : "";
            }

            if (access_options.length() == 0
                    || !access_options.has("scheme") || access_options.getString("scheme").length() == 0
                    || !access_options.has("host") || access_options.getString("host").length() == 0
                    || !access_options.has("path") || access_options.getString("path").length() == 0
                    ) {
                JasonHelper.next("error", action, data, event, context);
            } else {
                final Uri.Builder uri_builder = new Uri.Builder();
                uri_builder.scheme(access_options.getString("scheme"))
                        .authority(access_options.getString("host"))
                        .appendEncodedPath(access_options.getString("path"));

                if(redirect_uri != "") {
                    uri_builder.appendQueryParameter("redirect_uri", redirect_uri);
                }

                if(grant_type != "") {
                    uri_builder.appendQueryParameter("grant_type", grant_type);
                }

                if(code != null) {
                    uri_builder.appendQueryParameter("code", code);
                }

                if(refresh_token != null) {
                    uri_builder.appendQueryParameter("refresh_token", refresh_token);
                }

                OkHttpClient client = null;

                if(access_options.has("basic") && access_options.getBoolean("basic")) {
                    OkHttpClient.Builder b = new OkHttpClient.Builder();
                    b.authenticator(new Authenticator() {
                        @Override
                        public Request authenticate(Route route, okhttp3.Response response) throws IOException {
                            if (response.request().header("Authorization") != null) {
                                return null;
                            }

                            String credential = okhttp3.Credentials.basic(client_id, client_secret);
                            return response.request().newBuilder().header("Authorization", credential).build();
                        }
                    });
                    client = b.build();
                } else {
                    uri_builder.appendQueryParameter("client_id", client_id);
                    uri_builder.appendQueryParameter("client_secret", client_secret);
                    client = ((Launcher)context.getApplicationContext()).getHttpClient(0);
                }

                Request request;
                Request.Builder requestBuilder = new Request.Builder()
                        .url(uri_builder.build().toString())
                        .method("POST", RequestBody.create(null, new byte[0]));
                request = requestBuilder.build();

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        handleError(e, action, event, context);
                    }

                    @Override
                    public void onResponse(Call call, okhttp3.Response response) throws IOException {
                        try {
                            JSONObject jsonResponse = new JSONObject(response.body().source().readString(Charset.defaultCharset()));
                            String access_token = jsonResponse.getString("access_token");

                            String refresh_token = jsonResponse.has("refresh_token") ? jsonResponse.getString("refresh_token") : "";
                            int expires_in = jsonResponse.has("expires_in") ? jsonResponse.getInt("expires_in") : -1;

                            SharedPreferences preferences = context.getSharedPreferences("oauth", Context.MODE_PRIVATE);
                            preferences.edit().putString(client_id, access_token).apply();
                            preferences.edit().putInt(client_id + "_created_at", (int)(System.currentTimeMillis() / 1000)).apply();
                            preferences.edit().putInt(client_id + "_expires_in", expires_in).apply();
                            if(refresh_token.length() > 0) {
                                preferences.edit().putString(client_id + "_refresh_token", refresh_token).apply();
                            }

                            JSONObject result = new JSONObject();
                            result.put("token", access_token);

                            JasonHelper.next("success", action, result, event, context);
                        } catch(JSONException e) {
                            handleError(e, action, event, context);
                        }
                    }
                });
            }
        } catch(JSONException e) {
            handleError(e, action, event, context);
        }
    }

    public void reset(final JSONObject action, final JSONObject data, final JSONObject event, final Context context) {
        try {
            final JSONObject options = action.getJSONObject("options");

            String client_id = options.getString("client_id");

            if(options.has("version") && options.getString("version").equals("1")) {
                SharedPreferences preferences = context.getSharedPreferences("oauth", Context.MODE_PRIVATE);
                preferences.edit().remove(client_id).apply();

                if(preferences.contains(client_id + "_request_token_secret")) {
                    preferences.edit().remove(client_id + "_request_token_secret");
                }

                if(preferences.contains(client_id + "_access_token_secret")) {
                    preferences.edit().remove(client_id + "_access_token_secret");
                }

                JasonHelper.next("success", action, data, event, context);
            } else {
                SharedPreferences preferences = context.getSharedPreferences("oauth", Context.MODE_PRIVATE);
                preferences.edit().remove(client_id).apply();

                if(preferences.contains(client_id + "_refresh_token")) {
                    preferences.edit().remove(client_id + "_refresh_token");
                }

                if(preferences.contains(client_id + "_expires_in")) {
                    preferences.edit().remove(client_id + "_expires_in");
                }

                if(preferences.contains(client_id + "_created_at")) {
                    preferences.edit().remove(client_id + "_created_at");
                }

                JasonHelper.next("success", action, data, event, context);
            }
        } catch(JSONException e) {
            handleError(e, action, event, context);
        }
    }

    public void request(final JSONObject action, final JSONObject data, final JSONObject event, final Context context) {
        try {
            JSONObject options = action.getJSONObject("options");

            String client_id = options.getString("client_id");

            String client_secret = "";
            if(options.has("client_secret") && options.getString("client_secret").length() > 0) {
                client_secret = options.getString("client_secret");
            }

            SharedPreferences sharedPreferences = context.getSharedPreferences("oauth", Context.MODE_PRIVATE);
            String access_token = sharedPreferences.getString(client_id, null);

            String path = options.getString("path");
            String scheme = options.getString("scheme");
            String host = options.getString("host");

            String method;
            if(options.has("method")) {
                method = options.getString("method");
            } else {
                method = "GET";
            }

            if(access_token != null && access_token.length() > 0) {
                JSONObject params = new JSONObject();
                if(options.has("data")) {
                    params = options.getJSONObject("data");
                }

                JSONObject headers = new JSONObject();
                if(options.has("headers")) {
                    headers = options.getJSONObject("headers");
                }

                Uri.Builder uriBuilder = new Uri.Builder();
                uriBuilder.scheme(scheme);
                uriBuilder.encodedAuthority(host);
                uriBuilder.path(path);

                Uri uri = uriBuilder.build();
                String url = uri.toString();

                final OAuthRequest request = new OAuthRequest(Verb.valueOf(method), url);

                Iterator paramKeys = params.keys();
                while(paramKeys.hasNext()) {
                    String key = (String)paramKeys.next();
                    String value = params.getString(key);
                    request.addParameter(key, value);
                }

                Iterator headerKeys = headers.keys();
                while(headerKeys.hasNext()) {
                    String key = (String)headerKeys.next();
                    String value = headers.getString(key);
                    request.addHeader(key, value);
                }

                if(options.has("version") && options.getString("version").equals("1")) {
                    DefaultApi10a oauthApi = new DefaultApi10a() {
                        @Override
                        public String getRequestTokenEndpoint() { return null; }

                        @Override
                        public String getAccessTokenEndpoint() { return null; }

                        @Override
                        public String getAuthorizationUrl(OAuth1RequestToken requestToken) { return null; }
                    };

                    ServiceBuilder serviceBuilder = new ServiceBuilder();
                    serviceBuilder.apiKey(client_id);

                    if(client_secret.length() > 0) {
                        serviceBuilder.apiSecret(client_secret);
                    }

                    final OAuth10aService oauthService = serviceBuilder.build(oauthApi);

                    String access_token_secret = sharedPreferences.getString(client_id + "_access_token_secret", null);

                    oauthService.signRequest(new OAuth1AccessToken(access_token, access_token_secret), request);

                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... voids) {
                            try {
                                Response response = oauthService.execute(request);

                                if(response.getCode() == 200) {
                                    JasonHelper.next("success", action, response.getBody(), event, context);
                                } else {
                                    JasonHelper.next("error", action, response.getBody(), event, context);
                                }
                            } catch(Exception e) {
                                handleError(e, action, event, context);
                            }

                            return null;
                        }
                    }.execute();
                } else {
                    DefaultApi20 oauthApi = new DefaultApi20() {
                        @Override
                        public String getAccessTokenEndpoint() {
                            return null;
                        }

                        @Override
                        protected String getAuthorizationBaseUrl() {
                            return null;
                        }
                    };

                    ServiceBuilder serviceBuilder = new ServiceBuilder();
                    serviceBuilder.apiKey(client_id);

                    if(client_secret.length() > 0) {
                        serviceBuilder.apiSecret(client_secret);
                    }

                    final OAuth20Service oauthService = serviceBuilder.build(oauthApi);

                    oauthService.signRequest(new OAuth2AccessToken(access_token), request);

                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... voids) {
                            try {
                                Response response = oauthService.execute(request);

                                if(response.getCode() == 200) {
                                    JasonHelper.next("success", action, response.getBody(), event, context);
                                } else {
                                    JasonHelper.next("error", action, response.getBody(), event, context);
                                }
                            } catch(Exception e) {
                                handleError(e, action, event, context);
                            }
                            return null;
                        }
                    }.execute();
                }
            } else {
                JasonHelper.next("error", action, data, event, context);
            }
            //change exception
        } catch(JSONException e) {
            handleError(e, action, event, context);
        }
    }

    private void handleError(Exception err, JSONObject action, JSONObject event, Context context) {
        try {
            JSONObject error = new JSONObject();
            error.put("data", err.toString());
            JasonHelper.next("error", action, error, event, context);
        } catch(JSONException e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }
    }
}
