package com.myvideoyun.giftrenderer;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MvyRenderer {

    static {
        System.loadLibrary("lightgift");
        System.loadLibrary("MvyRendererJni");
    }



    /**
     * 特效播放中
     */
    public static final int MSG_STAT_EFFECTS_PLAY = 0x00020000;

    /**
     * 特效播放结束
     */
    public static final int MSG_STAT_EFFECTS_END = 0x00040000;

    /**
     * 特效播放开始
     */
    public static final int MSG_STAT_EFFECTS_START = 0x00080000;


    private long render;
    private String effectPath;
    private long faceData;
    private boolean enalbeVFilp;
    private boolean updateEffectPath;
    private boolean updateFaceData;
    private boolean updateVFlip;

    public void initGLResource() {
        render = Create();
    }

    public void releaseGLResource() {
        Destroy(render);
    }

    public void setCallback(OnEffectCallback callback) {
        Callback(render, callback);
    }

    public void processWithTexture(int texture, int width, int height) {
        if (updateEffectPath) {
            SetStickerPath(render, effectPath);
            updateEffectPath = false;
        }

        if (updateFaceData) {
            SetFaceData(render, faceData);
            updateFaceData =false;
        } else {
            SetFaceData(render, 0);
        }

        if (updateVFlip) {
            SetEnableVFlip(render, enalbeVFilp);
        }

        Draw(render, texture, width, height);
    }

    public void pauseProcess() {
        SetPause(render);
    }

    public void resumeProcess() {
        SetResume(render);
    }

    public void setEffectPath(String effectPath) {
        this.effectPath = effectPath;

        updateEffectPath = true;
    }

    public void setFaceData(long faceData) {
        this.faceData = faceData;

        updateFaceData = true;
    }

    public void setEnalbeVFilp(boolean enalbeVFilp) {
        this.enalbeVFilp = enalbeVFilp;

        updateVFlip = true;
    }

    public void SetMVPMatrix(float[] mvp){
        SetMVPMatrix(render, mvp);
    }

    native long Create();
    native void Destroy(long renderer);
    native void Callback(long renderer, OnEffectCallback callback);
    native void SetFaceData(long renderer, long value);
    native void SetStickerPath(long renderer, String ptah);
    native void SetEnableVFlip(long renderer,  boolean enable);
    native void SetPause(long renderer);
    native void SetResume(long renderer);
    native void SetMVPMatrix(long renderer, float [] mvp);
    native void Draw(long renderer, int texture, int width, int height);
    public static native int InitLicense(Context context, String key, int keyLength);

    public interface OnEffectCallback {
        void MVYRenderMsg(int type, int ret);
    }
}
