package com.myvideoyun.giftrenderer;

import android.content.Context;

public class MVYLicenseManager {
    public static int initLicense(Context context, String key, int keyLength) {
        return MvyRenderer.InitLicense(context, key, keyLength);
    }
    public static int initLicenseEx(Context context, String license, int licenseLength, String key, int keyLength) {
        return MvyRenderer.InitLicenseEx(context, license, licenseLength, key, keyLength);
    }
}
