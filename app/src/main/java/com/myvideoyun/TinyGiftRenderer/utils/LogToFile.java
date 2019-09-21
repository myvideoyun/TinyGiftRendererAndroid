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
import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LogToFile {
    public static final boolean DEBUG_FLAG = false;
    private static String TAG = "TGR.logtofile";

    private static String logPath = null;

    // data format
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US);

    // log file is named using the date, using static member to
    // ensure that only one log file exists during the Application's life cycle.
    private static Date date = new Date();

    /**
     * Better invoke this when the Application is created.
     * @param context
     */
    public static void init(Context context) {
        logPath = getFilePath(context) + "/Logs";
    }

    private static String getFilePath(Context context) {

        if (Environment.MEDIA_MOUNTED.equals(Environment.MEDIA_MOUNTED) || !Environment.isExternalStorageRemovable()) {
            // get external storage path, default is  /storage/emulated/0/Android/data/com.waka.workspace.logtofile/files/
            return context.getExternalFilesDir(null).getPath();
        } else {
            // store to /data/data/
            return context.getFilesDir().getPath();
        }
    }

    private static final char VERBOSE = 'v';

    private static final char DEBUG = 'd';

    private static final char INFO = 'i';

    private static final char WARN = 'w';

    private static final char ERROR = 'e';

    public static void v(String tag, String msg) {
        if(DEBUG_FLAG){
            writeToFile(VERBOSE, tag, msg);
        }

    }

    public static void d(String tag, String msg) {
        if(DEBUG_FLAG){
            writeToFile(DEBUG, tag, msg);
        }
    }

    public static void i(String tag, String msg) {
        if(DEBUG_FLAG){
            writeToFile(INFO, tag, msg);
        }

    }

    public static void w(String tag, String msg) {
        if(DEBUG_FLAG){
            writeToFile(WARN, tag, msg);
        }
    }

    public static void e(String tag, String msg) {
        if(DEBUG_FLAG){
            writeToFile(ERROR, tag, msg);
        }

    }

    private static void writeToFile(char type, String tag, String msg) {

        if (null == logPath) {
            Log.e(TAG, "logPath is null ，");
            return;
        }

        String fileName = logPath + "/log_" + dateFormat.format(new Date()) + ".log";//including the data into log file
        String log = dateFormat.format(date) + " " + type + " " + tag + " " + msg + "\n";// log content;

        File file = new File(logPath);
        if (!file.exists()) {
            file.mkdirs();
        }

        FileOutputStream fos = null;//no need to close it.
        BufferedWriter bw = null;
        try {
            //the second param represent append or overwrite ，true means append，flase means overwrite;
            fos = new FileOutputStream(fileName, true);
            bw = new BufferedWriter(new OutputStreamWriter(fos));
            bw.write(log);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bw != null) {
                    bw.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
