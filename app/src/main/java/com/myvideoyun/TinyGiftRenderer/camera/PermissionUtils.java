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
package com.myvideoyun.TinyGiftRenderer.camera;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;

public class PermissionUtils {


    public static void askPermission(Activity context, String[] permissions,int req, Runnable
        runnable){
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
            int result= 0;
            for(String a:permissions){
                result+=ActivityCompat.checkSelfPermission(context,a);
            }
            if(result== PackageManager.PERMISSION_GRANTED){
                runnable.run();
            }else{
                ActivityCompat.requestPermissions(context,permissions,req);
            }
        }else{
            runnable.run();
        }
    }

    public static void onRequestPermissionsResult(boolean isReq,int[] grantResults,Runnable
        okRun,Runnable deniRun){
        if(isReq){
            boolean b=true;
            for (int a:grantResults){
                b&=(a==PackageManager.PERMISSION_GRANTED);
            }
            if (grantResults.length > 0&&b) {
                okRun.run();
            } else {
                deniRun.run();
            }
        }
    }

}
