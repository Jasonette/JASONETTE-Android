package com.jasonette.seed.Lib;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.hanks.passcodeview.PasscodeView;
import com.jasonette.seed.Launcher.Launcher;
import com.jasonette.seed.Service.key.JasonKeyService;

import org.json.JSONObject;


public class PasscodeActivity extends AppCompatActivity {
    String mode;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        class PView extends PasscodeView {
            public PView(Context context) {
                super(context);
            }
            @Override protected Boolean equals(String psd) {
                try {
                    if (mode.equalsIgnoreCase("authenticate")) {
                        JasonKeyService keyService = (JasonKeyService)((Launcher)PasscodeActivity.this.getApplicationContext()).services.get("JasonKeyService");
                        return keyService.is_authenticated(psd);
                    } else if (mode.equalsIgnoreCase("register")) {
                        return getLocalPasscode().equals(psd);
                    }
                } catch (Exception e) {
                }
                return false;
            }
        }
        /*
            1. Does this view already have a password set?
            => YES: Ask for password
            => NO: Call password() to ask for password
        */

        PView passcodeView = new PView(PasscodeActivity.this);

        Intent intent = getIntent();
        if (intent.hasExtra("mode")) {
            mode = intent.getStringExtra("mode");
            if (mode.equalsIgnoreCase("authenticate")) {
                // Doesn't matter what it's set to because we'll use a custom "equals()" logic from above (PView)
                passcodeView.setPasscodeLength(4).setLocalPasscode("0000");
            } else if (mode.equalsIgnoreCase("register")) {

            }
        }

        // 1. Get password for the current url
        passcodeView.setBackgroundColor(Color.parseColor("#468af6"));
        FrameLayout.LayoutParams layout = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);
        passcodeView.setLayoutParams(layout);
        setContentView(passcodeView);

        /**
            Customize the view to get rid of unnecessary graphics that come with the library
         **/
        for(int i = 0; i < passcodeView.getChildCount(); i++) {
            View child = passcodeView.getChildAt(i);
            if (child.getClass().equals(LinearLayout.class)) {
                int index = 0;
                for (int j=0; j<((LinearLayout)child).getChildCount(); j++) {
                    View grandchild = ((LinearLayout)child).getChildAt(j);
                    if (grandchild.getClass().equals(RelativeLayout.class)) {
                        index++;
                        if (index == 2) {
                            grandchild.setBackgroundColor(Color.WHITE);
                            break;
                        }
                    }
                }
            }
        }


        passcodeView.setListener(new PasscodeView.PasscodeViewListener() {
            @Override
            public void onFail() {

            }

            @Override
            public void onSuccess(String number) {
                // Set return intent with extras
                try {
                    JasonKeyService keyService = (JasonKeyService)((Launcher)PasscodeActivity.this.getApplicationContext()).services.get("JasonKeyService");
                    if (mode.equalsIgnoreCase("register")) {
                        keyService.register(number);
                    }
                    Toast.makeText(getApplication(),"Success",Toast.LENGTH_SHORT).show();
                } catch (Exception e) {

                }
                Intent intent = new Intent();
                setResult(RESULT_OK, intent);
                finish();
            }
        });
    }
}
