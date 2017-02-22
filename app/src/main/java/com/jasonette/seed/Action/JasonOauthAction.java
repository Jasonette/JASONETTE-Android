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
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth10aService;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.jasonette.seed.Helper.JasonHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class JasonOauthAction {
    public void auth(final JSONObject action, final JSONObject data, final JSONObject event, final Context context) {
        try {
            final JSONObject options = action.getJSONObject("options");

            if(options.getString("version").equals("1")) {
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
                            .encodedAuthority(request_options.getString("host"));
                    for(String fragment: request_options.getString("path").split("/")) {
                        if(!fragment.equals("")) {
                            uriBuilder.appendPath(fragment);
                        }
                    }
                    final String requestUri = uriBuilder.build().toString();

                    final Uri.Builder authorizeUriBuilder = new Uri.Builder();
                    authorizeUriBuilder.scheme(authorize_options.getString("scheme"))
                            .encodedAuthority(authorize_options.getString("host"));
                    for(String fragment: authorize_options.getString("path").split("/")) {
                        if(!fragment.equals("")) {
                            authorizeUriBuilder.appendPath(fragment);
                        }
                    }

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
                                preferences.edit().putString(client_id + "token_secret", request_token.getTokenSecret()).apply();

                                String auth_url = oauthService.getAuthorizationUrl(request_token);

                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                intent.setData(Uri.parse(auth_url));
                                context.startActivity(intent);
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

                JSONObject access_options = options.getJSONObject("access");

                JSONObject access_options_data = new JSONObject();
                if(access_options.has("data")) {
                    access_options_data = access_options.getJSONObject("data");
                }

                if(access_options_data.has("grant_type") && access_options_data.getString("grant_type").equals("password")) {
                    String client_id = access_options.getString("client_id");
                    String client_secret = "";

                    if(access_options.has("client_secret")) {
                        client_secret = access_options.getString("client_secret");
                    }

                    if(!access_options.has("scheme") || access_options.getString("scheme").length() == 0
                        || !access_options.has("host") || access_options.getString("host").length() == 0
                        || !access_options.has("path") || access_options.getString("path").length() == 0
                        || !access_options_data.has("username") || access_options_data.getString("username").length() == 0
                        || !access_options_data.has("password") || access_options_data.getString("password").length() == 0
                    ) {
                        JasonHelper.next("error", action, data, event, context);
                    } else {
                        String username = access_options_data.getString("username");
                        String password = access_options_data.getString("password");

                        Uri.Builder builder = new Uri.Builder();

                        builder.scheme(access_options.getString("scheme"))
                                .authority(access_options.getString("host"));

                        for(String fragment: access_options.getString("path").split("/")) {
                            if(!fragment.equals("")) {
                                builder.appendPath(fragment);
                            }
                        }

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
                        serviceBuilder.apiSecret(client_secret);

                        final OAuth20Service oauthService = serviceBuilder.build(oauthApi);

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
                    String view = "";

                    if(options.has("view")) {
                        view = options.getString("view");
                    }

                    JSONObject authorize_options = null;
                    if(options.has("authorize")) {
                        authorize_options = options.getJSONObject("authorize");
                    } else {
                        JSONObject error = new JSONObject();
                        error.put("data", "Authorize data missing");
                        JasonHelper.next("error", action, error, event, context);
                    }

                    JSONObject authorize_options_data = null;

                    if(authorize_options.has("data")) {
                        authorize_options_data = authorize_options.getJSONObject("data");
                    } else {
                        JSONObject error = new JSONObject();
                        error.put("data", "Authorize data missing");
                        JasonHelper.next("error", action, error, event, context);
                    }

                    //Assuming code auth
                    if(authorize_options == null || authorize_options.length() == 0) {
                        JasonHelper.next("error", action, data, event, context);
                    } else {
                        String client_id = authorize_options.getString("client_id");
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
                            // TODO
                            //CHECK IF CREDENTIALS EXISTS
                            //REFRESH ACCESS TOKEN IF THAT IS THE CASE

                            Uri.Builder builder = new Uri.Builder();

                            builder.scheme(authorize_options.getString("scheme"))
                                    .authority(authorize_options.getString("host"));

                            for(String fragment: authorize_options.getString("path").split("/")) {
                                if(!fragment.equals("")) {
                                    builder.appendPath(fragment);
                                }
                            }

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

                            if(client_secret != "") {
                                serviceBuilder.apiSecret(client_secret);
                            }
                            serviceBuilder.callback(redirect_uri);

                            OAuth20Service oauthService = serviceBuilder.build(oauthApi);

                            if(view.equals("app")) {
                                //error here
                            } else {
                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                intent.setData(Uri.parse(oauthService.getAuthorizationUrl()));
                                context.startActivity(intent);
                            }
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
            try {
                JSONObject error = new JSONObject();
                error.put("data", e.toString());
                JasonHelper.next("error", action, error, event, context);
            } catch(JSONException error) {
                Log.d("Error", error.toString());
            }
        }
    }

    public void oauth_callback(final JSONObject action, final JSONObject data, final JSONObject event, final Context context) {
        try {
            final JSONObject options = action.getJSONObject("options");
            if (options.has("version") && options.getString("version").equals("1")) {
                //OAuth 1

                if(action.has("uri")) {
                    Uri uri = Uri.parse(action.getString("uri"));

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
                                .encodedAuthority(access_options.getString("host"));
                        for(String fragment: access_options.getString("path").split("/")) {
                            if(!fragment.equals("")) {
                                uriBuilder.appendPath(fragment);
                            }
                        }
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
                                    String oauth_token_secret = preferences.getString(client_id + "token_secret", null);

                                    OAuth1RequestToken oauthToken = new OAuth1RequestToken(string_oauth_token, oauth_token_secret);

                                    String access_token = oauthService.getAccessToken(oauthToken, oauth_verifier).getToken();

                                    preferences.edit().putString(client_id, access_token).apply();

                                    JSONObject result = new JSONObject();
                                    result.put("token", access_token);

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
                    JasonHelper.next("error", action, data, event, context);
                }
            } else {
                // OAuth 2
                Uri uri = Uri.parse(action.getString("uri"));

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

                    JSONObject access_options = options.getJSONObject("access");

                    final String client_id = access_options.getString("client_id");
                    String client_secret = access_options.getString("client_secret");

                    String redirect_uri = "";
                    if(access_options.has("redirect_uri")) {
                        redirect_uri = access_options.getString("redirect_uri");
                    }

                    final String code = uri.getQueryParameter("code");

                    if (access_options.length() == 0
                        || !access_options.has("scheme") || access_options.getString("scheme").length() == 0
                        || !access_options.has("host") || access_options.getString("host").length() == 0
                        || !access_options.has("path") || access_options.getString("path").length() == 0
                    ) {
                        JasonHelper.next("error", action, data, event, context);
                    } else {
                        final Uri.Builder builder = new Uri.Builder();
                        builder.scheme(access_options.getString("scheme"))
                                .authority(access_options.getString("host"))
                                .appendEncodedPath(access_options.getString("path"));
                        if(redirect_uri != "") {
                            builder.appendQueryParameter("redirect_uri", redirect_uri);
                        }

                        DefaultApi20 oauthApi = new DefaultApi20() {
                            @Override
                            public String getAccessTokenEndpoint() {
                                return builder.build().toString();
                            }

                            @Override
                            protected String getAuthorizationBaseUrl() {
                                return null;
                            }
                        };

                        ServiceBuilder serviceBuilder = new ServiceBuilder();
                        serviceBuilder.apiKey(client_id);
                        serviceBuilder.apiSecret(client_secret);

                        if(redirect_uri != "") {
                            serviceBuilder.callback(redirect_uri);
                        }

                        final OAuth20Service oauthService = serviceBuilder.build(oauthApi);

                        new AsyncTask<Void, Void, Void>() {
                            @Override
                            protected Void doInBackground(Void... params) {
                                try {
                                    String access_token = oauthService.getAccessToken(code).getAccessToken();

                                    SharedPreferences preferences = context.getSharedPreferences("oauth", Context.MODE_PRIVATE);
                                    preferences.edit().putString(client_id, access_token).apply();

                                    JSONObject result = new JSONObject();
                                    try {
                                        result.put("token", access_token);
                                    } catch(JSONException e) {
                                        handleError(e, action, event, context);
                                    }

                                    JasonHelper.next("success", action, result, event, context);

                                } catch(Exception e) {
                                    handleError(e, action, event, context);
                                }
                                return null;
                            }
                        }.execute();
                    }
                }
            }
        }
        catch(JSONException e) {
            try {
                JSONObject error = new JSONObject();
                error.put("data", e.toString());
                JasonHelper.next("error", action, error, event, context);
            } catch(JSONException error) {
                Log.d("Error", error.toString());
            }
        }
    }

    public void reset(final JSONObject action, final JSONObject data, final JSONObject event, final Context context) {
        try {
            final JSONObject options = action.getJSONObject("options");

            String client_id = options.getString("client_id");

            if(options.has("version") && options.getString("version").equals("1")) {
                //TODO
            } else {
                SharedPreferences preferences = context.getSharedPreferences("oauth", Context.MODE_PRIVATE);
                preferences.edit().remove(client_id).apply();
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
            String client_secret = options.getString("client_secret");

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
                if(options.has("version") && options.getString("version").equals("1")) {
                    String access_token_secret = sharedPreferences.getString(client_id + "token_secret", null);

                    DefaultApi10a oauthApi = new DefaultApi10a() {
                        @Override
                        public String getRequestTokenEndpoint() { return null; }

                        @Override
                        public String getAccessTokenEndpoint() { return null; }

                        @Override
                        public String getAuthorizationUrl(OAuth1RequestToken requestToken) { return null; }
                    };

                    //Socket socket = new Socket(ip, port);

                    final OAuth10aService oauthService = new ServiceBuilder()
                        .apiKey(client_id)
                        .apiSecret(client_secret)
                        //.debugStream(socket.getOutputStream())
                        .build(oauthApi);

                    Uri.Builder uriBuilder = new Uri.Builder();
                    uriBuilder.scheme(scheme);
                    uriBuilder.encodedAuthority(host);
                    uriBuilder.path(path);

                    Uri uri = uriBuilder.build();
                    String url = uri.toString();

                    final OAuthRequest request = new OAuthRequest(Verb.valueOf(method), url);

                    oauthService.signRequest(new OAuth1AccessToken(access_token, access_token_secret), request);

                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... voids) {
                            try {
                                com.github.scribejava.core.model.Response response = oauthService.execute(request);

                                JasonHelper.next("success", action, response.getBody(), event, context);
                            } catch(Exception e) {
                                handleError(e, action, event, context);
                            }

                            return null;
                        }
                    }.execute();
                } else {

                    Request request;
                    Request.Builder requestBuilder = new Request.Builder();

                    Uri.Builder uriBuilder = new Uri.Builder();
                    uriBuilder.scheme(scheme);
                    uriBuilder.encodedAuthority(host);
                    uriBuilder.path(path);

                    Uri uri = uriBuilder.build();
                    String url = uri.toString();

                    requestBuilder.url(url)
                            .addHeader("AUTHORIZATION", "Bearer " + access_token);
                    request = requestBuilder.build();

                    OkHttpClient client = new OkHttpClient();
                    client.newCall(request).enqueue(new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            e.printStackTrace();
                            try {
                                if(action.has("error")) {
                                    JSONObject error = new JSONObject();
                                    error.put("data", e.toString());
                                    JasonHelper.next("error", action, error, event, context);
                                }
                            } catch (Exception err) {
                                Log.d("Error", err.toString());
                            }
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            try {
                                if(!response.isSuccessful()) {
                                    if(action.has("error")){
                                        JSONObject error = new JSONObject();
                                        error.put("data", response.toString());
                                        JasonHelper.next("error", action, error, event, context);
                                    }
                                } else {
                                    String jsonData = response.body().string();
                                    JasonHelper.next("success", action, jsonData, event, context);
                                }
                            } catch(Exception err) {
                                Log.d("Error", err.toString());
                            }
                        }
                    });
                }
            } else {
                JasonHelper.next("error", action, data, event, context);
            }
            //change exception
        } catch(JSONException e) {
            handleError(e, action, event, context);
        }
    }

    private void handleError(Exception e, JSONObject action, JSONObject event, Context context) {
        try {
            JSONObject error = new JSONObject();
            error.put("data", e.toString());
            JasonHelper.next("error", action, error, event, context);
        } catch(JSONException error) {
            Log.d("Error", error.toString());
        }
    }
}