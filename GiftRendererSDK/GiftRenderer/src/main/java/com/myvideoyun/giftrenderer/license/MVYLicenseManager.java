package com.myvideoyun.giftrenderer.license;

import android.content.Context;

import com.myvideoyun.giftrenderer.render.basic.MVYRenderer;

public class MVYLicenseManager {
    public static void initLicense(Context context, String key) {
        MVYRenderer.InitLicense(context, key, key.length());
    }
}
