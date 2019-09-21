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
package com.myvideoyun.TinyGiftRenderer.render;

import java.lang.ref.WeakReference;

public class WeakEventListener implements IEventListener {

    public WeakReference<IEventListener> mWeak;

    public WeakEventListener(IEventListener listener) {
        this.mWeak = new WeakReference<>(listener);
    }

    public WeakEventListener() {
    }

    public void setEventListener(IEventListener listener) {
        this.mWeak = new WeakReference<>(listener);
    }

    @Override
    public int onEvent(int type, int ret, String info) {
        if (mWeak != null) {
            IEventListener listener = mWeak.get();
            if (listener != null) {
                return listener.onEvent(type, ret, info);
            }
        }
        return 0;
    }
}
