package com.myvideoyun.giftrenderer;

import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.myvideoyun.giftrenderer.cameraTool.MVYCameraPreviewListener;
import com.myvideoyun.giftrenderer.cameraTool.MVYCameraPreviewWrap;
import com.myvideoyun.giftrenderer.cameraTool.MVYPreviewView;
import com.myvideoyun.giftrenderer.cameraTool.MVYPreviewViewListener;
import com.myvideoyun.giftrenderer.gpuImage.MVYGPUImageConstants;
import com.myvideoyun.video.R;

import java.io.IOException;

import static com.myvideoyun.giftrenderer.gpuImage.MVYGPUImageConstants.AYGPUImageContentMode.kAYGPUImageScaleAspectFill;
import static com.myvideoyun.giftrenderer.gpuImage.MVYGPUImageConstants.AYGPUImageRotationMode.kAYGPUImageRotateRight;

public class CameraActivity extends AppCompatActivity implements MVYCameraPreviewListener, MVYPreviewViewListener {

    Camera camera;
    MVYCameraPreviewWrap cameraPreviewWrap;
    MVYPreviewView surfaceView;

    MVYEffectHandler effectHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_camera);
        surfaceView = findViewById(R.id.camera_preview);
        surfaceView.setListener(this);
        surfaceView.setContentMode(kAYGPUImageScaleAspectFill);
    }

    /**
     * 打开硬件设备
     */
    private void openHardware() {
        if (camera != null) {
            camera.release();
            camera = null;
        }
        camera = Camera.open(1);

        cameraPreviewWrap = new MVYCameraPreviewWrap(camera);
        cameraPreviewWrap.setPreviewListener(this);
        cameraPreviewWrap.setRotateMode(kAYGPUImageRotateRight);
        cameraPreviewWrap.startPreview(surfaceView.eglContext);
    }

    /**
     * 关闭硬件设备
     */
    private void closeHardware() {
        // 关闭相机
        if (camera != null) {
            cameraPreviewWrap.stopPreview();
            cameraPreviewWrap = null;
            camera.release();
            camera = null;
        }
    }

    @Override
    public void cameraCrateGLEnvironment() {

        effectHandler = new MVYEffectHandler(this);
        effectHandler.setRotateMode(MVYGPUImageConstants.AYGPUImageRotationMode.kAYGPUImageFlipVertical);
        // 设置特效
        effectHandler.setEffectPath(getExternalCacheDir() + "/myvideoyun/gifts/aixinmeigui_v1/meta.json");
        effectHandler.setEffectPlayCount(2);
        try {
            // 添加滤镜
            effectHandler.setStyle(BitmapFactory.decodeStream(getApplicationContext().getAssets().open("FilterResources/filter/03桃花.JPG")));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void cameraVideoOutput(int texture, int width, int height, long timeStamp) {
        if (effectHandler != null) {
            effectHandler.processWithTexture(texture, width, height);
        }

        // 渲染到surfaceView
        if (surfaceView != null) {
            surfaceView.render(texture, width, height);
        }
    }

    @Override
    public void cameraDestroyGLEnvironment() {
        if (effectHandler != null) {
            effectHandler.destroy();
            effectHandler = null;
        }
    }

    @Override
    public void createGLEnvironment() {
        openHardware();
    }

    @Override
    public void destroyGLEnvironment() {
        closeHardware();
    }
}
