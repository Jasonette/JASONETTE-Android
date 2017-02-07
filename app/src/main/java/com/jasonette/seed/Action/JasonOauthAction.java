package com.jasonette.seed.Action;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.jasonette.seed.Helper.JasonHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.Charset;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class JasonOauthAction {
    public void auth(final JSONObject action, final JSONObject data, final Context context) {
        try{
            final JSONObject options = action.getJSONObject("options");

            if(options.getString("version").equals("1")) {
                //OAuth 1 - TODO
                JasonHelper.next("error", action, data, context);
            } else {
                //OAuth 2
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
                    JasonHelper.next("error", action, error, context);
                }

                JSONObject authorize_options_data = null;

                if(authorize_options.has("data")) {
                    authorize_options_data = authorize_options.getJSONObject("data");
                } else {
                    JSONObject error = new JSONObject();
                    error.put("data", "Authorize data missing");
                    JasonHelper.next("error", action, error, context);
                }

                if(authorize_options_data.has("response_type") && authorize_options_data.getString("response_type").equals("password")) {
                    //Password auth - TODO
                    JasonHelper.next("error", action, data, context);
                } else {
                    //Assuming code auth
                    if(authorize_options == null || authorize_options.length() == 0) {
                        JasonHelper.next("error", action, data, context);
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
                            JasonHelper.next("error", action, data, context);
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
            try {
                JSONObject error = new JSONObject();
                error.put("data", e.toString());
                JasonHelper.next("error", action, error, context);
            } catch(JSONException error) {
                Log.d("Error", error.toString());
            }
        }
    }

    public void access_token(final JSONObject action, final JSONObject data, final Context context) {
        try {
            final JSONObject options = action.getJSONObject("options");
            if (options.has("version") && options.getString("version").equals("1")) {
                //OAuth 1 - TODO
            } else {
                //OAuth 2
                String client_id = options.getJSONObject("access").getString("client_id");

                SharedPreferences sharedPreferences = context.getSharedPreferences("oauth", Context.MODE_PRIVATE);
                String access_token = sharedPreferences.getString(client_id, null);
                if(access_token != null) {
                    JSONObject result = new JSONObject();
                    result.put("token", access_token);
                    JasonHelper.next("success", action, result, context);
                } else {
                    JSONObject error = new JSONObject();
                    error.put("data", "access token not found");
                    JasonHelper.next("error", action, error, context);
                }
            }
        } catch(JSONException e) {
            try {
                JSONObject error = new JSONObject();
                error.put("data", e.toString());
                JasonHelper.next("error", action, error, context);
            } catch(JSONException error) {
                Log.d("Error", error.toString());
            }
        }
    }

    public void oauth_callback(final JSONObject action, final JSONObject data, final Context context) {
        try {
            final JSONObject options = action.getJSONObject("options");
            if (options.has("version") && options.getString("version").equals("1")) {
                //OAuth 1
                JasonHelper.next("error", action, data, context);
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

                    JasonHelper.next("success", action, result, context);
                } else {

                    JSONObject access_options = options.getJSONObject("access");

                    final String client_id = access_options.getString("client_id");
                    String client_secret = access_options.getString("client_secret");

                    String redirect_uri = "";
                    if(access_options.has("redirect_uri")) {
                        redirect_uri = access_options.getString("redirect_uri");
                    }

                    String code = uri.getQueryParameter("code");

                    if (access_options.length() == 0
                        || !access_options.has("scheme") || access_options.getString("scheme").length() == 0
                        || !access_options.has("host") || access_options.getString("host").length() == 0
                        || !access_options.has("path") || access_options.getString("path").length() == 0
                    ) {
                        JasonHelper.next("error", action, data, context);
                    } else {
                        Uri.Builder builder = new Uri.Builder();
                        builder.scheme(access_options.getString("scheme"))
                                .authority(access_options.getString("host"))
                                .appendEncodedPath(access_options.getString("path"))
                                .appendQueryParameter("client_id", client_id)
                                .appendQueryParameter("client_secret", client_secret)
                                .appendQueryParameter("code", code);
                        if(redirect_uri != "") {
                            builder.appendQueryParameter("redirect_uri", redirect_uri);
                        }

                        Request request;
                        Request.Builder requestBuilder = new Request.Builder();

                        requestBuilder.url(builder.build().toString());
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
                                        JasonHelper.next("error", action, error, context);
                                    }
                                } catch (Exception err) {
                                    Log.d("Error", err.toString());
                                }
                            }

                            @Override
                            public void onResponse(Call call, Response response) throws IOException {
                                try {
                                    JSONObject jsonBody = new JSONObject(response.body().source().readString(Charset.defaultCharset()));
                                    String access_token = jsonBody.getString("access_token");

                                    SharedPreferences preferences = context.getSharedPreferences("oauth", Context.MODE_PRIVATE);
                                    preferences.edit().putString(client_id, access_token).apply();

                                    JSONObject result = new JSONObject();
                                    result.put("token", access_token);

                                    JasonHelper.next("success", action, result, context);
                                } catch(JSONException e) {

                                }
                            }
                        });
                    }
                }
            }
        }
        catch(JSONException e) {
            try {
                JSONObject error = new JSONObject();
                error.put("data", e.toString());
                JasonHelper.next("error", action, error, context);
            } catch(JSONException error) {
                Log.d("Error", error.toString());
            }
        }
    }

    public void reset(final JSONObject action, final JSONObject data, final Context context) {
        try {
            final JSONObject options = action.getJSONObject("options");

            String client_id = options.getString("client_id");

            if(options.has("version") && options.getString("version").equals("1")) {
                //TODO
            } else {
                SharedPreferences preferences = context.getSharedPreferences("oauth", Context.MODE_PRIVATE);
                preferences.edit().remove(client_id).apply();
                JasonHelper.next("success", action, data, context);
            }
        } catch(JSONException e) {
            handleError(e, action, context);
        }
    }

    public void request(final JSONObject action, final JSONObject data, final Context context) {
        try {
            JSONObject options = action.getJSONObject("options");

            String client_id = options.getString("client_id");

            SharedPreferences sharedPreferences = context.getSharedPreferences("oauth", Context.MODE_PRIVATE);
            String access_token = sharedPreferences.getString(client_id, null);

            if(access_token != null && access_token.length() > 0) {
                String path = options.getString("path");
                String scheme = options.getString("scheme");
                String host = options.getString("host");

                String method = "";
                if(options.has("method")) {
                    method = options.getString("method");
                } else {
                    method = "get";
                }

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
                                JasonHelper.next("error", action, error, context);
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
                                    JasonHelper.next("error", action, error, context);
                                }
                            } else {
                                String jsonData = response.body().string();
                                JasonHelper.next("success", action, jsonData, context);
                            }
                        } catch(Exception err) {
                            Log.d("Error", err.toString());
                        }
                    }
                });
            } else {
                JasonHelper.next("error", action, data, context);
            }
        } catch(JSONException e) {
            handleError(e, action, context);
        }
    }

    private void handleError(Exception e, JSONObject action, Context context) {
        try {
            JSONObject error = new JSONObject();
            error.put("data", e.toString());
            JasonHelper.next("error", action, error, context);
        } catch(JSONException error) {
            Log.d("Error", error.toString());
        }
    }
}