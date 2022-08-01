package com.myvideoyun.giftrenderer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.myvideoyun.giftrenderer.animTool.MVYAnimRenderThread;
import com.myvideoyun.giftrenderer.animTool.MVYAnimView;
import com.myvideoyun.giftrenderer.animTool.MVYAnimViewListener;
import com.myvideoyun.giftrenderer.gpuImage.GPUImageCustomFilter.MVYGPUImageEffectPlayFinishListener;
import com.myvideoyun.giftrenderer.gpuImage.MVYGPUImageConstants;
import com.myvideoyun.giftrenderer.gpuImage.MVYGPUImageFramebuffer;
import com.myvideoyun.video.R;

import static android.opengl.GLES20.*;
import static com.myvideoyun.giftrenderer.gpuImage.MVYGPUImageConstants.AYGPUImageContentMode.kAYGPUImageScaleAspectFill;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class AnimationActivity extends AppCompatActivity {

    MVYAnimView animView;
    MVYAnimHandler effectHandler;
    Button button;
    Switch overlaySwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_animation);
        
        // 下载图片
        File imageFile =  new File(getExternalCacheDir(), "image.png");
        if (!imageFile.exists()) {
            new Thread(() -> {
                try {
                    downloadImageFromUrl("https://avatars.githubusercontent.com/u/45362645?v=4", imageFile.getPath());
                } catch (IOException e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        Toast.makeText(this, "overlay image download failure", Toast.LENGTH_SHORT).show();
                    });
                }
            }).start();
        }

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
                    effectHandler.setAssetPlayFinishListener(new MVYGPUImageEffectPlayFinishListener() {
                        public void playFinish(int ret) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    button.setText("play");
                                }
                            });

                            Log.d("AnimationActivity", "当前特效播放完成, ret: " + ret);
                        }
                    });
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

                    glClearColor(0, 0, 0, 0);
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

        button = findViewById(R.id.animation_play_bt);
        button.setOnClickListener(v -> {
            Button button = (Button) v;
            if (button.getText().equals("play")) {
                // 设置特效
                effectHandler.setAssetPath(getExternalCacheDir() + "/myvideoyun/gifts/yurenjie/overlay_setting1.json");
                File myImageFile =  new File(getExternalCacheDir(), "image.png");
                if (overlaySwitch.isChecked()) {
                    if (myImageFile.exists()) {
                        effectHandler.setOverlayPath(myImageFile.getPath());
                    } else {
                        Toast.makeText(this, "overlay image download failure", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    effectHandler.setOverlayPath("");
                }
                effectHandler.setAssetPlayCount(1);
                button.setText("stop");
            } else if (button.getText().equals("stop")) {
                // 设置特效
                effectHandler.setAssetPath("");
                effectHandler.setOverlayPath("");
                button.setText("play");
            }
        });

        overlaySwitch = findViewById(R.id.overlay_switch);
    }


    public void downloadImageFromUrl(String imageUrl, String storeImageLocalPath) throws IOException {
        InputStream in = null;
        Bitmap bitmap = null;
        int responseCode = -1;

        URL url = new URL(imageUrl);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setDoInput(true);
        con.connect();
        responseCode = con.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            in = con.getInputStream();
            bitmap = BitmapFactory.decodeStream(in);
            in.close();
        }

        if (bitmap != null) {
            try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(storeImageLocalPath))) {
                if (bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos)) {
                    bos.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
