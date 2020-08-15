package com.myvideoyun.giftrenderer;

import android.content.Context;

public class MVYLicenseManager {
    public static void initLicense(Context context, String key, int keyLength, MvyRenderer.OnResultCallback callback) {
        MvyRenderer.InitLicense(context, key, keyLength, callback);
    }
}
