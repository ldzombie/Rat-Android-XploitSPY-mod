package com.remote.app.Managers.DeviceControl;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioManager;

import com.remote.app.MainService;

import org.json.JSONObject;

public class Audio {
    //voiceCall 0, system 1, ring 2, music 3, alarm 4, notif 5,
    public static void VolumeUp(Context context,int type){
        AudioManager audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        audioManager.adjustStreamVolume(type,AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
    }

    public static void VolumeDown(Context context,int type){
        AudioManager audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        audioManager.adjustStreamVolume(type,AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
    }

    public static JSONObject getVolume(Context context){
        JSONObject info = new JSONObject();
        try{
            AudioManager audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
            info.put("voiceCall",audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL));
            info.put("alarm",audioManager.getStreamVolume(AudioManager.STREAM_ALARM));
            info.put("notif",audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION));
            info.put("music",audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
            info.put("ring",audioManager.getStreamVolume(AudioManager.STREAM_RING));

            return info;

        }
        catch (Exception e){return null;}
    }

    @SuppressLint("NewApi")
    public static JSONObject getMinMaxVolume(Context context){
        JSONObject info = new JSONObject();
        try{
            AudioManager audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
            info.put("voiceCallMax",audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL));
            info.put("voiceCallMin",audioManager.getStreamMinVolume(AudioManager.STREAM_VOICE_CALL));
            info.put("alarmMax",audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM));
            info.put("alarmMin",audioManager.getStreamMinVolume(AudioManager.STREAM_ALARM));
            info.put("notifMax",audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION));
            info.put("notifMin",audioManager.getStreamMinVolume(AudioManager.STREAM_NOTIFICATION));
            info.put("musicMax",audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
            info.put("musicMin",audioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC));
            info.put("ringMax",audioManager.getStreamMaxVolume(AudioManager.STREAM_RING));
            info.put("ringMin",audioManager.getStreamMinVolume(AudioManager.STREAM_RING));

            return info;

        }
        catch (Exception e){return null;}
    }

    public static void setMode(Context context,int type){
        AudioManager audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        switch (type){
            case 1:
                audioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
                break;
            case 2:
                audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                break;
            case 3:
                audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                break;
        }

    }




}
