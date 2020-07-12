package com.myvideoyun.giftrenderer.common;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import static android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
import static android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE;

public class UI {

    // ---------- 状态栏更新 ----------

    enum Brightness {
        LIGHT, DARK
    }

    public static class SystemStyle {

        final Brightness statusBarIconBrightness;

        final Integer systemNavigationBarColor;

        final Brightness systemNavigationBarIconBrightness;

        SystemStyle(Brightness statusBarIconBrightness, Integer systemNavigationBarColor, Brightness systemNavigationBarIconBrightness) {
            this.statusBarIconBrightness = statusBarIconBrightness;
            this.systemNavigationBarColor = systemNavigationBarColor;
            this.systemNavigationBarIconBrightness = systemNavigationBarIconBrightness;
        }
    }

    public static SystemStyle light = new SystemStyle(
            Brightness.LIGHT,
            0xFF000000,
            Brightness.LIGHT);

    public static SystemStyle dark = new SystemStyle(
            Brightness.DARK,
            0xFF000000,
            Brightness.LIGHT);

    /**
     * 更新状态栏
     */
    public static void setSystemStyle(Activity activity, SystemStyle systemStyle) {
        Window window = activity.getWindow();
        View view = window.getDecorView();
        int flags = view.getSystemUiVisibility();

        // 底部导航栏图标颜色
        if (Build.VERSION.SDK_INT >= 26) {
            if (systemStyle.systemNavigationBarIconBrightness != null) {
                switch (systemStyle.systemNavigationBarIconBrightness) {
                    case DARK:
                        flags |= 16; // SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                        break;
                    case LIGHT:
                        flags &= -17;
                }
            }

            if (systemStyle.systemNavigationBarColor != null) {
                window.setNavigationBarColor(systemStyle.systemNavigationBarColor);
            }
        }

        // 顶部状态栏图标颜色
        if (Build.VERSION.SDK_INT >= 23) {
            if (systemStyle.statusBarIconBrightness != null) {
                switch (systemStyle.statusBarIconBrightness) {
                    case DARK:
                        flags |= 8192;
                        break;
                    case LIGHT:
                        flags &= -8193;
                }
            }
        }

        // 沉浸式状态栏
        if (Build.VERSION.SDK_INT >= 21) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
        }

        // 显示范围扩展到全屏
        if (Build.VERSION.SDK_INT >= 18) {
            flags |= SYSTEM_UI_FLAG_LAYOUT_STABLE;
            flags |= SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
        }

        view.setSystemUiVisibility(flags);
    }
}
