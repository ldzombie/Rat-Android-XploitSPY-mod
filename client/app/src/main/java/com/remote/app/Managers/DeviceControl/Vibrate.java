package com.remote.app.Managers.DeviceControl;

import android.content.Context;
import android.os.Vibrator;

import com.remote.app.MainService;

public class Vibrate {



    public static void vib(Context context,int count,int duration, int sleep){
        if(duration==0)
            duration=500;
        if(sleep ==0)
            sleep=800;
        Vibrator vibrator = (Vibrator) context.getSystemService( context.VIBRATOR_SERVICE);

        for(int k=0;k<count;k++){
            try {
                vibrator.vibrate(duration);//500
                Thread.sleep(sleep);//800
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void vib(Context context,int count){
        Vibrator vibrator = (Vibrator) context.getSystemService( context.VIBRATOR_SERVICE);

        for(int k=0;k<count;k++){
            try {
                vibrator.vibrate(500);
                Thread.sleep(800);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
