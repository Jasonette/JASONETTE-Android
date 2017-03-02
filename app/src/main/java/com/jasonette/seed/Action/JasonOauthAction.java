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

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

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
                    if(authorize_options.has("data")) {
                        authorize_options_data = authorize_options.getJSONObject("data");
                    } else {
                        JSONObject error = new JSONObject();
                        error.put("data", "Authorize data missing");
                        JasonHelper.next("error", action, error, event, context);
                    }
                    //
                    //Assuming code auth
                    //
                    if(authorize_options.length() == 0) {
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
                            serviceBuilder.callback(redirect_uri);

                            OAuth20Service oauthService = serviceBuilder.build(oauthApi);

                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setData(Uri.parse(oauthService.getAuthorizationUrl()));
                            context.startActivity(intent);
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

                                JasonHelper.next("success", action, response.getBody(), event, context);
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

                                JasonHelper.next("success", action, response.getBody(), event, context);
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