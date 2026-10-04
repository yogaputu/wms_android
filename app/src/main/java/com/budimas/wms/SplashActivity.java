package com.budimas.wms;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SplashActivity extends Activity {
    private static final long SPLASH_DELAY_MS = 900L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupWindow();
        setContentView(createContent());

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }, SPLASH_DELAY_MS);
    }

    private void setupWindow() {
        Window window = getWindow();
        window.setStatusBarColor(Color.rgb(5, 7, 10));
        window.setNavigationBarColor(Color.rgb(5, 7, 10));
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            window.getDecorView().setSystemUiVisibility(0);
        }
    }

    private View createContent() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(Color.rgb(5, 7, 10));
        int screenPadding = dp(28);
        root.setPadding(screenPadding, screenPadding, screenPadding, screenPadding);

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.budimas_logo_full);
        logo.setAdjustViewBounds(true);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        int logoWidth = Math.min(dp(300), getResources().getDisplayMetrics().widthPixels - dp(40));
        int logoHeight = Math.round(logoWidth * (187f / 280f));
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(logoWidth, logoHeight);
        root.addView(logo, logoParams);

        TextView title = new TextView(this);
        title.setText("Budimas WMS");
        title.setTextColor(Color.WHITE);
        title.setTextSize(23);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setIncludeFontPadding(false);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        titleParams.topMargin = dp(22);
        root.addView(title, titleParams);

        TextView subtitle = new TextView(this);
        subtitle.setText("Warehouse Mobile");
        subtitle.setTextColor(Color.rgb(245, 196, 0));
        subtitle.setTextSize(13);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setLetterSpacing(0.04f);
        subtitle.setIncludeFontPadding(false);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        subtitleParams.topMargin = dp(8);
        root.addView(subtitle, subtitleParams);

        View line = new View(this);
        line.setBackgroundColor(Color.rgb(245, 196, 0));
        LinearLayout.LayoutParams lineParams = new LinearLayout.LayoutParams(dp(72), dp(4));
        lineParams.topMargin = dp(22);
        root.addView(line, lineParams);

        return root;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
