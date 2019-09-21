/*
 * Copyright 2019 myvideoyun Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.myvideoyun.TinyGiftRenderer.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Looper;
import android.text.format.Time;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Properties;
import java.util.TreeSet;

/**
 */

public class CrashHandler implements Thread.UncaughtExceptionHandler {

    /**
     * Debug Log tag
     */
    public static final String TAG = "TGR.crashHandler";
    /**
     * check if enable logging
     */
    public static final boolean DEBUG = true;
    /**
     * default UncaughtException  class
     */
    private Thread.UncaughtExceptionHandler mDefaultHandler;
    /**
     * CrashHandler instance
     */
    private static CrashHandler INSTANCE;
    /**
     * Context for the application
     */
    private Context mContext;
    /**
     * use properties to save device info and stack info
     */
    private Properties mDeviceCrashInfo = new Properties();
    private static final String VERSION_NAME = "versionName";
    private static final String VERSION_CODE = "versionCode";
    private static final String STACK_TRACE = "STACK_TRACE";
    /**
     * file extension for the crash report
     */
    private static final String CRASH_REPORTER_EXTENSION = ".cr";

    private static Object syncRoot = new Object();

    /**
     * ensure only one CrashHandler instance
     */
    private CrashHandler() {
    }

    /**
     * singular mode
     */
    public static CrashHandler getInstance() {
       /* if (INSTANCE == null) {
            INSTANCE = new CrashHandler();
        }
        return INSTANCE;*/
        // double lock to ensure the multithreaded access
        if (INSTANCE == null) {

            synchronized (syncRoot) {

                if (INSTANCE == null) {
                    INSTANCE = new CrashHandler();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * @param ctx
     */
    public void init(Context ctx) {
        mContext = ctx;
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        if (!handleException(ex) && mDefaultHandler != null) {
            mDefaultHandler.uncaughtException(thread, ex);
        } else {
            //Sleep to wait the finish of the application
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Log.e(TAG, "Error : ", e);
            }
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(10);
        }
    }

    /**
     * customize the exception handling
     *
     * @param ex
     * @return true: exception is handled; return false otherwise
     */
    private boolean handleException(Throwable ex) {
        if (ex == null) {
            Log.w(TAG, "handleException --- ex==null");
            return true;
        }
        final String msg = ex.getLocalizedMessage();
        if (msg == null) {
            return false;
        }
        //Using Toast to display exception info
        new Thread() {
            @Override
            public void run() {
                Looper.prepare();

                if (DEBUG) {
                    Log.d(TAG, "Exception ->" + msg);
                    Toast toast = Toast.makeText(mContext, "Application error:\r\n" + msg,
                            Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();

                    //save error report
                    LogToFile.w("my", msg);
                }
                Looper.loop();
            }
        }.start();

        collectCrashDeviceInfo(mContext);
        return true;
    }

    /**
     * Previous report can be send when the application start up.
     */
    public void sendPreviousReportsToServer() {
        sendCrashReportsToServer(mContext);
    }

    /**
     * send the crash report to server
     *
     * @param ctx
     */
    private void sendCrashReportsToServer(Context ctx) {
        String[] crFiles = getCrashReportFiles(ctx);
        if (crFiles != null && crFiles.length > 0) {
            TreeSet<String> sortedFiles = new TreeSet<String>();
            sortedFiles.addAll(Arrays.asList(crFiles));
            for (String fileName : sortedFiles) {
                File cr = new File(ctx.getFilesDir(), fileName);
                postReport(cr);
                cr.delete();// delete previous report
            }
        }
    }

    private void postReport(File file) {
        // TODO: send report to the server;
    }

    /**
     * get crash report file
     *
     * @param ctx
     * @return
     */
    private String[] getCrashReportFiles(Context ctx) {
        File filesDir = ctx.getFilesDir();
        FilenameFilter filter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(CRASH_REPORTER_EXTENSION);
            }
        };
        return filesDir.list(filter);
    }

    /**
     * save error info to the file;
     *
     * @param ex
     * @return
     */
    private String saveCrashInfoToFile(Throwable ex) {
        Writer info = new StringWriter();
        PrintWriter printWriter = new PrintWriter(info);
        ex.printStackTrace(printWriter);
        Throwable cause = ex.getCause();
        while (cause != null) {
            cause.printStackTrace(printWriter);
            cause = cause.getCause();
        }
        String result = info.toString();
        printWriter.close();
        mDeviceCrashInfo.put("EXEPTION", ex.getLocalizedMessage());
        mDeviceCrashInfo.put(STACK_TRACE, result);
        try {
            //long timestamp = System.currentTimeMillis();
            Time t = new Time("GMT+8");
            t.setToNow(); // get system time
            int date = t.year * 10000 + t.month * 100 + t.monthDay;
            int time = t.hour * 10000 + t.minute * 100 + t.second;
            String fileName = "crash-" + date + "-" + time + CRASH_REPORTER_EXTENSION;
            FileOutputStream trace = mContext.openFileOutput(fileName,
                    Context.MODE_PRIVATE);
            mDeviceCrashInfo.store(trace, "");
            trace.flush();
            trace.close();
            return fileName;
        } catch (Exception e) {
            Log.e(TAG, "an error occured while writing report file...", e);
        }
        return null;
    }

    /**
     * collect device info
     *
     * @param ctx
     */
    public void collectCrashDeviceInfo(Context ctx) {
        try {
            PackageManager pm = ctx.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(ctx.getPackageName(),
                    PackageManager.GET_ACTIVITIES);
            if (pi != null) {
                mDeviceCrashInfo.put(VERSION_NAME,
                        pi.versionName == null ? "not set" : pi.versionName);
                mDeviceCrashInfo.put(VERSION_CODE, "" + pi.versionCode);
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Error while collect package info", e);
        }
        // using reflection to collect device info.
        // The Build class contains various devices info,
        // like system version, device manufactures,
        Field[] fields = Build.class.getDeclaredFields();
        for (Field field : fields) {
            try {
                field.setAccessible(true);
                mDeviceCrashInfo.put(field.getName(), "" + field.get(null));
                if (DEBUG) {
                    Log.d(TAG, field.getName() + " : " + field.get(null));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error while collect crash info", e);
            }
        }
    }
}
