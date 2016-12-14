package com.jasonette.seed.Launcher;

import android.app.Application;
import com.bumptech.glide.request.target.ViewTarget;
import com.jasonette.seed.R;

public class Launcher extends Application {
    public Launcher() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ViewTarget.setTagId(R.id.glide_request);
    }

}
