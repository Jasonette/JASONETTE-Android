package com.jasonette.seed.Launcher;

import android.content.res.Resources;
import android.util.Log;

import com.facebook.stetho.Stetho;
import com.facebook.stetho.okhttp3.StethoInterceptor;
import com.jasonette.seed.R;

import okhttp3.OkHttpClient;
import timber.log.Timber;

/**
 * Provides debug-build specific Application.
 * 
 * To disable Stetho console logging change the setting in src/debug/res/values/bools.xml
 */
public class DebugLauncher extends Launcher {

    private static final String LOGTAG = DebugLauncher.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();

        Stetho.initializeWithDefaults(this);
        Resources res = getResources();
        boolean enableStethoConsole = res.getBoolean(R.bool.enableStethoConsole);

        if (enableStethoConsole) {
            Timber.plant(new ConfigurableStethoTree(new ConfigurableStethoTree.Configuration.Builder()
                   .showTags(true)
                   .minimumPriority(Log.DEBUG)
                   .build()));
            Log.i(LOGTAG, "Using Stetho console logging");
        } else  {
            Timber.plant(new Timber.DebugTree());
        }
        Timber.i("Initialised Stetho debugging"+getEnv());
    }

    @Override
    public OkHttpClient getHttpClient() {
        return new OkHttpClient.Builder()
                .addNetworkInterceptor(new StethoInterceptor())
                .build();
    }

}
