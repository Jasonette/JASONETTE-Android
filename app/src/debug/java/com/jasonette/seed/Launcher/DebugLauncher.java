package com.jasonette.seed.Launcher;

import android.util.Log;

import com.facebook.stetho.Stetho;
import com.facebook.stetho.okhttp3.StethoInterceptor;

import okhttp3.OkHttpClient;

/**
 * Provides debug-build specific Application
 */
public class DebugLauncher extends Launcher {

    private static final String LOGTAG = DebugLauncher.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(LOGTAG, "Initialised Stetho debugging");
        Stetho.initializeWithDefaults(this);
    }

    @Override
    public OkHttpClient getHttpClient() {
        return new OkHttpClient.Builder()
                .addNetworkInterceptor(new StethoInterceptor())
                .build();
    }

}
