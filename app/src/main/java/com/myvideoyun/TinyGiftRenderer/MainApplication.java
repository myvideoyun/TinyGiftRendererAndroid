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
package com.myvideoyun.TinyGiftRenderer;

import android.app.Application;

import com.myvideoyun.TinyGiftRenderer.utils.CrashHandler;
import com.squareup.leakcanary.LeakCanary;

public class MainApplication extends Application{

    @Override
    public void onCreate() {
        super.onCreate();
        LeakCanary.install(this);

        // exception handler, can be commented out;
        CrashHandler crashHandler = CrashHandler.getInstance();
        // register the exception handler
        crashHandler.init(getApplicationContext());
    }
}
