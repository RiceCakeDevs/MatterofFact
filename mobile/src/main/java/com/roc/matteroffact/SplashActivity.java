package com.roc.matteroffact;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;

/**
 * Created by kaushik on 7/6/17.
 */

public class SplashActivity extends Activity {

    // Splash screen timer
    private static int SPLASH_TIME_OUT = 3500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        TextView miniMessage = (TextView) findViewById(R.id.mini_message);
        TextView tag = (TextView) findViewById(R.id.tag);
        Typeface font = Typeface.createFromAsset(getAssets(), "fonts/Asap-Medium.otf");
        tag.setTypeface(font);
        miniMessage.setTypeface(font);
        miniMessage.setText(R.string.mini_message);

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                Intent i = new Intent(SplashActivity.this, MainActivity.class);
                startActivity(i);
                finish();
            }
        }, SPLASH_TIME_OUT);
    }

}
