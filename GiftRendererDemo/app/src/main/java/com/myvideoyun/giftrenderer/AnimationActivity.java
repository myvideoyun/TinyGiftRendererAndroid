package com.myvideoyun.giftrenderer;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.myvideoyun.giftrenderer.adapter.Adapter;
import com.myvideoyun.giftrenderer.animTool.MVYAnimRenderThread;
import com.myvideoyun.giftrenderer.animTool.MVYAnimView;
import com.myvideoyun.giftrenderer.animTool.MVYAnimViewListener;
import com.myvideoyun.giftrenderer.common.UI;
import com.myvideoyun.giftrenderer.render.MVYGPUImageConstants;
import com.myvideoyun.giftrenderer.render.MVYGPUImageFramebuffer;

import java.util.ArrayList;
import java.util.List;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static com.myvideoyun.giftrenderer.render.MVYGPUImageConstants.AYGPUImageContentMode.kAYGPUImageScaleAspectFill;

public class AnimationActivity extends AppCompatActivity implements Adapter.OnItemClickListener {

    MVYAnimView animView;
    MVYAnimHandler effectHandler;
    private Adapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        UI.setSystemStyle(this, UI.dark);

        setContentView(R.layout.activity_animation);

        initUI();
        initRender();

        // 设置特效数据
        List<Object[]> beautyDataList = new ArrayList<>();
        beautyDataList.add(new Object[]{R.mipmap.anim_flower, "花瓣", getCacheDir() + "/effect/data/LoveRoss/meta.json"});
        adapter.refreshEffectData(beautyDataList);
        adapter.setMode(0);
    }

    private void initUI() {
        RecyclerView recyclerView = findViewById(R.id.animation_recycle_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getBaseContext(), LinearLayoutManager.HORIZONTAL, false);
        recyclerView.setLayoutManager(layoutManager);

        adapter = new Adapter();
        adapter.setOnItemClickListener(this);

        recyclerView.setAdapter(adapter);
    }

    private void initRender() {
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
    }

    @Override
    public void onItemClick(int mode, Object data) {
        if (effectHandler != null) {
            effectHandler.setAssetPath(data.toString());
        }
    }
}
