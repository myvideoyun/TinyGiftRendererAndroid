package com.myvideoyun.giftrenderer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.myvideoyun.giftrenderer.common.FileUtil;
import com.myvideoyun.giftrenderer.common.UI;
import com.myvideoyun.giftrenderer.license.MVYLicenseManager;

import java.io.File;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        UI.setSystemStyle(this, UI.dark);

        setContentView(R.layout.activity_main);

        findViewById(R.id.main_camera).setOnClickListener(this);
        findViewById(R.id.main_animation).setOnClickListener(this);

        // 初始化License
        MVYLicenseManager.initLicense(getApplicationContext(), "jAwdRWLiAhQN3lJ2zfJv7aT4TxkdoEFIZ5B2TLf6AikLkNTMfJ97cLlgVKXNxZiB");

        // copy数据
        new Thread(() -> {
            String dstPath = getCacheDir().getPath() + File.separator + "effect";
            if (!new File(dstPath).exists()) {
                FileUtil.deleteFile(new File(dstPath));
                FileUtil.copyFileFromAssets("effect", dstPath, getAssets());
            }
            dstPath = getCacheDir().getPath() + File.separator + "style";
            if (!new File(dstPath).exists()) {
                FileUtil.deleteFile(new File(dstPath));
                FileUtil.copyFileFromAssets("style", dstPath, getAssets());
            }
        }).start();
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
