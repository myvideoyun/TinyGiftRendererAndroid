/*
 * Copyright 2019 myvideoyun Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.myvideoyun.TinyGiftRenderer.camera;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import com.myvideoyun.TinyGiftRenderer.TinyGiftRenderer;
import com.myvideoyun.TinyGiftRenderer.gles.GLUtil;
import com.myvideoyun.TinyGiftRenderer.gles.FullFrameRect;
import com.myvideoyun.TinyGiftRenderer.gles.Texture2dProgram;
import com.myvideoyun.TinyGiftRenderer.gles.OffScreenFrameBuffer;
import com.myvideoyun.TinyGiftRenderer.render.IEventListener;
import com.myvideoyun.TinyGiftRenderer.render.LightGiftFilter;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


/**
 * Renderer object for our GLSurfaceView.
 * <p>
 * Do not call any methods here directly from another thread -- use the
 * GLSurfaceView#queueEvent() call.
 */
class CameraSurfaceRenderer implements GLSurfaceView.Renderer {
    private static final String TAG = "TGR.surfaceRender";
    private static final boolean VERBOSE = false;

    private CameraActivity.CameraHandler mCameraHandler;

    private FullFrameRect mFullScreen;
    private FullFrameRect mExtFullScreen;

    private final float[] mSTMatrix = new float[16];
    private int mTextureId = -1;

    private SurfaceTexture mSurfaceTexture;
    private boolean mTextureUpdated;

    // width/height of the incoming camera preview frames
    private boolean mIncomingSizeUpdated;
    private int mIncomingWidth;
    private int mIncomingHeight;
    private int mPreviewWidth;
    private int mPreviewHeight;

    // create off-screen frame buffer and texture
    private int mOffscreenTexture;
    private OffScreenFrameBuffer mOffScreenFB = null; // offscreen frame buffer

    private float[] mTransMatrix;

    private boolean mInitOFB = true;
    private boolean mSetCameraSurface = true;
    private boolean mIsLandscape = true;
    private LightGiftFilter mGiftFilter = null;
    private Context mContext;
    /**
     * Constructs CameraSurfaceRenderer.
     * <p>
     * @param cameraHandler Handler for communicating with UI thread
     */
    public CameraSurfaceRenderer(CameraActivity.CameraHandler cameraHandler, Context context) {
        mCameraHandler = cameraHandler;
        mTextureId = -1;
        mContext = context;
        mIncomingSizeUpdated = false;
        mIncomingWidth = mIncomingHeight = -1;
        mPreviewWidth = mPreviewHeight = -1;

        mTransMatrix = new float[16];
        Matrix.setIdentityM(mTransMatrix, 0);
    }

    public void createGiftRender(){
        Log.i(TAG, "create gift render filter and set filter path");
        mGiftFilter = new LightGiftFilter(mContext, 0);
        //mGiftFilter.create();
        mGiftFilter.setGiftPath("assets/modelsticker/huacao/meta.json");
        mGiftFilter.setRenderEventListener(new IEventListener() {
            @Override
            public int onEvent(int msgType, int msgID, String s) {
                if (msgType == TinyGiftRenderer.MSG_TYPE_INFO) {
                    if (msgID == TinyGiftRenderer.MSG_GIFT_RENDER_END) {
                        mGiftFilter.setGiftPath(null);
                        Log.d(TAG, "Finish rendering");
                    } else if (msgID == TinyGiftRenderer.MSG_GIFT_RENDER_START) {
                        Log.d(TAG, "Start to render the gift:");
                    }
                } else if (msgType == TinyGiftRenderer.MSG_TYPE_ERROR) {
                    Log.e(TAG, "Error Msg：" + "/" + msgType + "/" + msgID + "/" + s);
                }
                return 0;
            }
        });
        mTextureUpdated = false;
    }
    /**
     * Notifies the renderer thread that the activity is pausing.
     * <p>
     * For best results, call this *after* disabling Camera preview.
     */
    public void releaseGLResources() {
        if (mSurfaceTexture != null) {
            Log.d(TAG, "renderer pausing -- releasing SurfaceTexture");
            mSurfaceTexture.release();
            mSurfaceTexture = null;
        }

        if(mOffScreenFB != null) {
            mOffScreenFB.release();
            mOffScreenFB = null;
        }

        if (mFullScreen != null) {
            mFullScreen.release(true);     // assume the GLSurfaceView EGL context is about
            mFullScreen = null;             //  to be destroyed
        }

        if(mExtFullScreen != null){
            mExtFullScreen.release(true);
            mExtFullScreen = null;
        }

        if(mTextureId != -1){
            GLES20.glDeleteTextures(1, new int[]{mTextureId}, 0);
            mTextureId = -1;
        }

        if(mGiftFilter != null) {
            //mGiftFilter.destroy();
            mGiftFilter.releaseGLResource();
            mGiftFilter.releaseFilter();
            mGiftFilter = null;
        }

        mInitOFB = true;
        mSetCameraSurface = true;
        mIncomingWidth = mIncomingHeight = -1;
    }

    /**
     * Records the size of the incoming camera preview frames.
     * <p>
     * It's not clear whether this is guaranteed to execute before or after onSurfaceCreated(),
     * so we assume it could go either way.  (Fortunately they both run on the same thread,
     * so we at least know that they won't execute concurrently.)
     */
    public void setPreviewDim(int width, int height) {
        Log.d(TAG, "setPreviewDim() called with: width = [" + width + "], height = [" + height + "]");
        if(mIncomingHeight != height || mIncomingWidth != width){
            Log.i(TAG, "setPreviewDim: different dimension, re-auth offscreen buffer");
            mInitOFB = true;
        }
        if(mIsLandscape) {
            mIncomingWidth = width;
            mIncomingHeight = height;
        }else{
            mIncomingWidth = height;
            mIncomingHeight = width;
        }
        mIncomingSizeUpdated = true;
    }

    public void setLandScape(boolean isLandscape){
        Log.d(TAG, "setLandScape() called with: isLandscape = [" + isLandscape + "]");
        mIsLandscape = isLandscape;
    }

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        Log.d(TAG, "onSurfaceCreated, recreate the surface");

        // Set up the texture blitter that will be used for on-screen display.  This
        // is *not* applied to the recording, because that uses a separate shader.
        mFullScreen = new FullFrameRect(
                new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_2D));

        mExtFullScreen = new FullFrameRect(
                new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_EXT));

        mTextureId = GLUtil.createOESTexture();

        // Create a SurfaceTexture, with an external texture, in this EGL context.  We don't
        // have a Looper in this thread -- GLSurfaceView doesn't create one -- so the frame
        // available messages will arrive on the main thread.
        mSurfaceTexture = new SurfaceTexture(mTextureId);
        mTextureUpdated = false;

        Log.i(TAG, "onSurfaceCreated: SurfaceTexture: " + mSurfaceTexture);
    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        Log.d(TAG, "onSurfaceChanged " + width + "x" + height);

        mInitOFB = true;
        mSetCameraSurface = true;

        mPreviewWidth = width;
        mPreviewHeight = height;

        // handle the new dimension
        mGiftFilter.onSizeChanged(width, height);
        GLES20.glViewport(0, 0, mPreviewWidth, mPreviewHeight);
        Log.i(TAG, "onSurfaceChanged: preview width: " + width + " height " + height);

    }

    @Override
    public void onDrawFrame(GL10 unused) {
        if (VERBOSE) Log.d(TAG, "onDrawFrame tex=" + mTextureId);
        if(mSurfaceTexture == null){
            //Log.d(TAG, "onDrawFrame: mSurfaceTxture is not Ready");
            return;
        }

        if(mSetCameraSurface){
            Log.d(TAG, "onDrawFrame: send message to Camera");
            mCameraHandler.sendMessage(mCameraHandler.obtainMessage(
                    CameraActivity.CameraHandler.MSG_SET_SURFACE_TEXTURE, mSurfaceTexture));
            mSetCameraSurface = false;
        }

        synchronized (this){
            if(mTextureUpdated) {
                mSurfaceTexture.updateTexImage();
                mSurfaceTexture.getTransformMatrix(mSTMatrix);
                mTextureUpdated = false;
            }
        }

        if (mIncomingWidth <= 0 || mIncomingHeight <= 0) {
            // Texture size isn't set yet.  This is only used for the filters, but to be
            // safe we can just skip drawing while we wait for the various races to resolve.
            // (This seems to happen if you toggle the screen off/on with power button.)
            Log.i(TAG, "Drawing before incoming texture size set; skipping");
            return;
        }

        if(mInitOFB){
            Log.i(TAG, "onDrawFrame: Create offscreen FB with width " + mIncomingWidth + " height " + mIncomingHeight);
            if (mOffScreenFB != null) {
                mOffScreenFB.release();
                mOffScreenFB = null;
            }
            mOffScreenFB = new OffScreenFrameBuffer(mIncomingWidth, mIncomingHeight);
            mOffscreenTexture = mOffScreenFB.getTexture();

            float AspectRatioView = (float)mPreviewWidth/(float)mPreviewHeight;
            float AspectRatioImg = (float)mIncomingWidth / (float)mIncomingHeight;
            Log.d(TAG, "View width " + mPreviewWidth + " height " + mPreviewHeight + " aspect ratio: " + AspectRatioView);
            Log.d(TAG, "Image width " + mIncomingWidth + " height " + mIncomingHeight + " aspect ratio: " + AspectRatioImg);

            mInitOFB = false;
            Log.i(TAG, "onDrawFrame: isLandscape: " + mIsLandscape);
        }

        if (mIncomingSizeUpdated) {
            //mOffscreenFullScreen.getProgram().setTexSize(mIncomingWidth, mIncomingHeight);
            mIncomingSizeUpdated = false;
        }

        // handle landscape mode
        mTransMatrix = mSTMatrix.clone();
        if(!mIsLandscape) {
            //Log.i(TAG, "onDrawFrame: Rotate it");
            Matrix.rotateM(mTransMatrix, 0, 90, 0f, 0f, 1f);
            Matrix.translateM(mTransMatrix, 0, 0, -1, 0);
        }

        // draw on offscreen first, the texture will be shared by encoder and on-screen render.
        mOffScreenFB.bind();
        GLES20.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
        GLES20.glViewport(0, 0, mIncomingWidth, mIncomingHeight);
        mExtFullScreen.drawFrame(mTextureId, mTransMatrix);
        mOffScreenFB.unbind();

        // draw on-screen
        int newTex = mGiftFilter.renderToTexture(mOffscreenTexture);

        // clear the screen first to get clean backgroud frame buffer
        mFullScreen.clearScreen();
        GLES20.glViewport(0, 0, mPreviewWidth, mPreviewHeight);
        Matrix.setIdentityM(mTransMatrix, 0);
        mFullScreen.drawFrameEx(newTex, mTransMatrix, mPreviewWidth, mPreviewHeight, mIncomingWidth, mIncomingHeight);
    }

    synchronized public void setFrameAvailable(SurfaceTexture st){
        if(VERBOSE) Log.d(TAG, "setFrameAvailable: new frame is available");
        mTextureUpdated = true;
    }
}
