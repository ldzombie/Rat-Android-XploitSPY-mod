package com.remote.app.Managers.DeviceControl;

import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.PowerManager;
import android.provider.Settings;

import com.remote.app.MainService;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;


public class Screen {

    public static void setWallpaperPath(Context context,String path){
        try {
            Bitmap bitmap = BitmapFactory.decodeFile(path);
            WallpaperManager wpm = WallpaperManager.getInstance(context);
            wpm.setBitmap(bitmap);
            //wpm.setBitmap(bitmap,null,true,WallpaperManager.FLAG_LOCK);Для экрана блокировки
        }catch (Exception e){}
    }



    public static void setBrightnessLevel(Context context,int value){
        Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, value);
    }

    public static void screenOn(Context context){
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        final PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK |PowerManager.ACQUIRE_CAUSES_WAKEUP |PowerManager.ON_AFTER_RELEASE, "com.remote.app:wakelock");
        wl.acquire();
    }


}
