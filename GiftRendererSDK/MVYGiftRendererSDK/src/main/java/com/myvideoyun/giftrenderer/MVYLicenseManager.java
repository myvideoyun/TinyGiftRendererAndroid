package com.myvideoyun.giftrenderer;

import android.content.Context;

public class MVYLicenseManager {
    public static int initLicense(Context context, String key, int keyLength) {
        return MvyRenderer.InitLicense(context, key, keyLength);
    }
}
