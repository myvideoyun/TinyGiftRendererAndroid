package com.myvideoyun.giftrenderer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.myvideoyun.video.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

//import com.myvideoyun.giftrenderer.GiftRenderer;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    public static void deleteFile(File file) {
        if (file.exists()) {
            if (file.isDirectory()) {
                for (File f : file.listFiles()) {
                    deleteFile(f);
                }
            } else {
                boolean result = file.delete();
            }
        }
    }

    public static boolean copyFileFromAssets(String src, String dst, AssetManager manager) {
        try {
            String[] files = manager.list(src);
            if (files.length > 0) {     //如果是文件夹
                File folder = new File(dst);
                if (!folder.exists()) {
                    boolean b = folder.mkdirs();
                    if (!b) {
                        return false;
                    }
                }
                for (String fileName : files) {
                    if (!copyFileFromAssets(src + File.separator + fileName, dst +
                            File.separator + fileName, manager)) {
                        return false;
                    }
                }
            } else {  //如果是文件
                if (!copyAssetsFile(src, dst, manager)) {
                    return false;
                }
            }
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    private static boolean copyAssetsFile(String src, String dst, AssetManager manager) {
        InputStream in;
        OutputStream out;
        try {
            File file = new File(dst);
            if (!file.exists()) {
                in = manager.open(src);
                out = new FileOutputStream(dst);
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                out.flush();
                out.close();
                in.close();
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        findViewById(R.id.main_camera).setOnClickListener(this);
        findViewById(R.id.main_animation).setOnClickListener(this);

        // 初始化License
        int ret = MVYLicenseManager.initLicense(getApplicationContext(), "jAwdRWLiAhQN3lJ2zfJv7Wo5D0gs6PF/l/kAo0vbMdZbgaV7+E06henBKCM13hkL", 48);
        if(ret == 0)
            Log.d("TGR", "Authenticate OK");
        else
            Log.d("TGR", "Authenticate Fail");
        // copy数据
        Thread t = new Thread(() -> {
            String dstPath = getExternalCacheDir() + "/myvideoyun/gifts";
            if (!new File(dstPath).exists()) {
                deleteFile(new File(dstPath));
                copyFileFromAssets("modelsticker", dstPath, getAssets());
            }
        });
        t.start();
        try {
            t.join();
        }catch (InterruptedException e) { e.printStackTrace(); }
        enterAnimationPage();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1001) {
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
            }

            enterCameraNextPage();
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.main_camera) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, 1001);
                } else {
                    enterCameraNextPage();
                }
            } else {
                enterCameraNextPage();
            }

        } else if (view.getId() == R.id.main_animation) {
            enterAnimationPage();
        }

    }

    private void enterCameraNextPage() {
        startActivity(new Intent(this, CameraActivity.class));
    }

    private void enterAnimationPage() {
        startActivity(new Intent(this, AnimationActivity.class));
    }
}
