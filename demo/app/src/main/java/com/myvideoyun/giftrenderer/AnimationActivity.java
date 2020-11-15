package com.myvideoyun.giftrenderer;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;

import com.myvideoyun.giftrenderer.animTool.MVYAnimRenderThread;
import com.myvideoyun.giftrenderer.animTool.MVYAnimView;
import com.myvideoyun.giftrenderer.animTool.MVYAnimViewListener;
import com.myvideoyun.giftrenderer.gpuImage.MVYGPUImageConstants;
import com.myvideoyun.giftrenderer.gpuImage.MVYGPUImageFramebuffer;
import com.myvideoyun.video.R;

import static android.opengl.GLES20.*;
import static com.myvideoyun.giftrenderer.gpuImage.MVYGPUImageConstants.AYGPUImageContentMode.kAYGPUImageScaleAspectFill;

public class AnimationActivity extends AppCompatActivity {

    MVYAnimView animView;
    MVYAnimHandler effectHandler;

    @Override
    protected void onCreate( Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_animation);

        // 第一个渲染视图
        animView = findViewById(R.id.animation_preview);
        animView.setListener(new MVYAnimViewListener() {

            private MVYGPUImageFramebuffer inputImageFramebuffer;
            private MVYAnimRenderThread renderThread;

            @Override
            public void createGLEnvironment() {
                animView.eglContext.syncRunOnRenderThread(() -> {
                    animView.eglContext.makeCurrent();

                    effectHandler = new MVYAnimHandler(AnimationActivity.this);
                    effectHandler.setRotateMode(MVYGPUImageConstants.AYGPUImageRotationMode.kAYGPUImageFlipVertical);
                    effectHandler.setAssetPlayFinishListener(() -> Log.d("AnimationActivity", "当前特效播放完成"));
                    effectHandler.setAssetPath(getExternalCacheDir() + "/myvideoyun/gifts/aixinmeigui_v1/meta.json");
                    //effectHandler.setAssetPlayCount(3);
                });

                renderThread = new MVYAnimRenderThread();
                renderThread.renderHandler = () -> animView.eglContext.syncRunOnRenderThread(() -> {
                    animView.eglContext.makeCurrent();

                    int width = 1080;
                    int height = 1920;

                    if (inputImageFramebuffer == null) {
                        inputImageFramebuffer = new MVYGPUImageFramebuffer(width, height);
                    }

                    inputImageFramebuffer.activateFramebuffer();

                    glClearColor(0,0,0,0);
                    glClear(GL_COLOR_BUFFER_BIT);

                    if (effectHandler != null) {
                        effectHandler.processWithTexture(inputImageFramebuffer.texture[0], width, height);
                    }

                    // 渲染到surfaceView
                    if (animView != null) {
                        animView.render(inputImageFramebuffer.texture[0], width, height);
                    }
                });
                renderThread.start();
            }

            @Override
            public void destroyGLEnvironment() {
                if (renderThread != null) {
                    renderThread.stopRenderThread();
                    renderThread = null;
                }

                animView.eglContext.syncRunOnRenderThread(() -> {
                    animView.eglContext.makeCurrent();

                    if (effectHandler != null) {
                        effectHandler.destroy();
                        effectHandler = null;
                    }

                    if (inputImageFramebuffer != null) {
                        inputImageFramebuffer.destroy();
                        inputImageFramebuffer = null;
                    }
                });
            }
        });
        animView.setContentMode(kAYGPUImageScaleAspectFill);

        findViewById(R.id.animation_play_bt).setOnClickListener(v -> {
            Button button = (Button) v;
            if (button.getText().equals("play")) {
                // 设置特效
                //effectHandler.setAssetPath(getExternalCacheDir() + "/myvideoyun/gifts/aixinmeigui_v2/meta.json");
                //effectHandler.setAssetPlayCount(1);
                button.setText("stop");
            } else if (button.getText().equals("stop")) {
                // 设置特效
                //effectHandler.setAssetPath("");
                button.setText("play");
            }
        });
    }

}
