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

import android.Manifest;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.net.Uri;
import android.opengl.GLES20;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.widget.VideoView;

import com.myvideoyun.TinyGiftRenderer.TinyGiftRenderer;
import com.myvideoyun.TinyGiftRenderer.R;
import com.myvideoyun.TinyGiftRenderer.render.IEventListener;
import com.myvideoyun.TinyGiftRenderer.render.LightGiftFilter;

import com.myvideoyun.TinyGiftRenderer.gles.EglCore;
import com.myvideoyun.TinyGiftRenderer.gles.WindowSurface;

public class VideoAndTextureActivity extends AppCompatActivity {
  private static final String TAG = "TGR.videoActivity";

  // Must be static or it'll get reset on every Activity pause/resume.
  private static volatile boolean sReleaseInCallback = true;
  private LightGiftFilter mGiftFilter = null;
  private Runnable start =
      new Runnable() {
        @Override
        public void run() {
          TinyGiftRenderer.setAuthEventListener(
              new IEventListener() {
                @Override
                public int onEvent(int i, int i1, String s) {
                  Log.e(TAG, "MSG(type/ret/info):" + i + "/" + i1 + "/" + s);
                  return 0;
                }
              });

          int id =
              TinyGiftRenderer.auth(
                  getApplicationContext(),
                  "umE46LDxwL3YPGDWz6Q8Ou0ORshb3drGJurQHHFGmLF8YkXh4GV6s2QgaHbAzttAPBui2F+tPJqDw9HaIyqYtA==",
                  64);
          Log.e(TAG, "id:" + id);
        }
      };
  private VideoView mVideoView;
  private TextureView mTextureView;
  private Renderer mRenderer;

  private void auth() {
    TinyGiftRenderer.setAuthEventListener(
        new IEventListener() {
          @Override
          public int onEvent(int i, int i1, String s) {
            Log.e(TAG, "MSG(type/ret/info):" + i + "/" + i1 + "/" + s);
            return 0;
          }
        });

    int id =
        TinyGiftRenderer.auth(
            getApplicationContext(),
            "NkkOmEE7rb5haWEHWjLTrvNnRiGaFe5hgM0t0YGuOF1Bd2zGpA5EUx2Oj/uo1eO9RQBKO+nKjwt2Rqlow1JDpg==",
            64);
    Log.e(TAG, "id:" + id);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Start up the Renderer thread.  It'll sleep until the TextureView is ready.
    mRenderer = new Renderer();
    mRenderer.start();
    auth();

    setContentView(R.layout.activity_video_and_texture);
    requestPermission();
    initView();
  }

  @Override
  protected void onStart() {
    super.onStart();
    Log.i(TAG, "create gift render filter and set filter path");
    mGiftFilter = new LightGiftFilter(getApplicationContext(), 0);
    mGiftFilter.setGiftPath("assets/modelsticker/huacao/meta.json");
    //mGiftFilter.setGiftPath("assets/modelsticker/mojing/meta.json");
    //mGiftFilter.setGiftPath("assets/modelsticker/dog_model/meta.json");
    //mGiftFilter.setGiftPath("assets/modelsticker/bunny/meta.json");
    mGiftFilter.setRenderEventListener(
        new IEventListener() {
          @Override
          public int onEvent(int msgType, int msgID, String s) {
            if (msgType == TinyGiftRenderer.MSG_TYPE_INFO) {
              if (msgID == TinyGiftRenderer.MSG_GIFT_RENDER_END) {
                // mGiftFilter.setGiftPath(null);
                Log.d(TAG, "Finish rendering");
              } else if (msgID == TinyGiftRenderer.MSG_GIFT_RENDER_START) {
                Log.d(TAG, "Start to render the gift:");
              }
            } else if (msgType == TinyGiftRenderer.MSG_TYPE_ERROR) {
              Log.e(TAG, "Rendering Error " + "/" + msgType + "/" + msgID + "/" + s);
            }
            return 0;
          }
        });
    mRenderer.setGiftFilter(mGiftFilter);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    mRenderer.halt();
  }

  private void requestPermission() {
    PermissionUtils.askPermission(
        this,
        new String[] {
          Manifest.permission.WRITE_EXTERNAL_STORAGE,
          Manifest.permission.CAMERA,
          Manifest.permission.INTERNET,
          Manifest.permission.RECORD_AUDIO
        },
        10,
        start);
  }

  private void initView() {
    mVideoView = findViewById(R.id.mVideoView);
    mVideoView.setOnPreparedListener(
        new MediaPlayer.OnPreparedListener() {
          @Override
          public void onPrepared(MediaPlayer mp) {
            mp.setLooping(true);
          }
        });

    mTextureView = findViewById(R.id.textureView);
    mTextureView.setSurfaceTextureListener(mRenderer);
    mTextureView.setOpaque(false);
    playVideo();
  }

  private void playVideo() {
    String fileName = "android.resource://" + getPackageName() + "/raw/jam1";
    mVideoView.setVideoURI(Uri.parse(fileName));
    mVideoView.start();
  }

  /**
   * Handles GL rendering and SurfaceTexture callbacks.
   *
   * <p>We don't create a Looper, so the SurfaceTexture-by-way-of-TextureView callbacks happen on
   * the UI thread.
   */
  private static class Renderer extends Thread implements TextureView.SurfaceTextureListener {
    private Object mLock = new Object(); // guards mSurfaceTexture, mDone
    private SurfaceTexture mSurfaceTexture;
    private EglCore mEglCore;
    private boolean mDone;
    private LightGiftFilter mGiftFilter = null;
    private boolean mInitDim = true;

    public Renderer() {
      super("TextureViewGL Renderer");
    }

    public void setGiftFilter(LightGiftFilter giftFilter) {
      mGiftFilter = giftFilter;
    }

    @Override
    public void run() {
      while (true) {
        SurfaceTexture surfaceTexture = null;

        // Latch the SurfaceTexture when it becomes available.  We have to wait for
        // the TextureView to create it.
        synchronized (mLock) {
          while (!mDone && (surfaceTexture = mSurfaceTexture) == null) {
            try {
              mLock.wait();
            } catch (InterruptedException ie) {
              throw new RuntimeException(ie); // not expected
            }
          }
          if (mDone) {
            break;
          }
        }
        Log.d(TAG, "Got surfaceTexture=" + surfaceTexture);

        // Create an EGL surface for our new SurfaceTexture.  We're not on the same
        // thread as the SurfaceTexture, which is a concern for the *consumer*, which
        // wants to call updateTexImage().  Because we're the *producer*, i.e. the
        // one generating the frames, we don't need to worry about being on the same
        // thread.
        mEglCore = new EglCore(null, EglCore.FLAG_TRY_GLES3);
        WindowSurface windowSurface = new WindowSurface(mEglCore, mSurfaceTexture);
        windowSurface.makeCurrent();

        if (mInitDim) {
          int width = windowSurface.getWidth();
          int height = windowSurface.getHeight();
          Log.d(TAG, "initial width " + width + " height " + height);

          if (mGiftFilter != null) mGiftFilter.onSizeChanged(width, height);

          mInitDim = false;
        }

        // Render frames until we're told to stop or the SurfaceTexture is destroyed.
        doAnimation(windowSurface);

        // release gift render;
        if (mGiftFilter != null) {
          // mGiftFilter.destroy();
          mGiftFilter.releaseGLResource();
          mGiftFilter.releaseFilter();
          mGiftFilter = null;
        }

        windowSurface.release();
        mEglCore.release();
        if (!sReleaseInCallback) {
          Log.i(TAG, "Releasing SurfaceTexture in renderer thread");
          surfaceTexture.release();
        }
      }

      Log.d(TAG, "Renderer thread exiting");
    }

    /**
     * Draws updates as fast as the system will allow.
     *
     * <p>In 4.4, with the synchronous buffer queue queue, the frame rate will be limited. In
     * previous (and future) releases, with the async queue, many of the frames we render may be
     * dropped.
     *
     * <p>The correct thing to do here is use Choreographer to schedule frame updates off of vsync,
     * but that's not nearly as much fun.
     */
    private void doAnimation(WindowSurface eglSurface) {
      int width = eglSurface.getWidth();
      int height = eglSurface.getHeight();

      Log.d(TAG, "Animating " + width + "x" + height + " EGL surface");

      while (true) {
        // Check to see if the TextureView's SurfaceTexture is still valid.
        synchronized (mLock) {
          SurfaceTexture surfaceTexture = mSurfaceTexture;
          if (surfaceTexture == null) {
            Log.d(TAG, "doAnimation exiting");
            return;
          }
        }

        // Still alive, render a frame.
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        mGiftFilter.renderToScreen(0); // passing a invalid texture id

        // Publish the frame.  If we overrun the consumer, frames will be dropped,
        // so on a sufficiently fast device the animation will run at faster than
        // the display refresh rate.
        //
        // If the SurfaceTexture has been destroyed, this will throw an exception.
        eglSurface.swapBuffers();
      }
    }

    /** Tells the thread to stop running. */
    public void halt() {
      synchronized (mLock) {
        mDone = true;
        mLock.notify();
      }
    }

    @Override // will be called on UI thread
    public void onSurfaceTextureAvailable(SurfaceTexture st, int width, int height) {
      Log.d(TAG, "onSurfaceTextureAvailable(" + width + "x" + height + ")");
      synchronized (mLock) {
        mSurfaceTexture = st;
        mLock.notify();
      }
    }

    @Override // will be called on UI thread
    public void onSurfaceTextureSizeChanged(SurfaceTexture st, int width, int height) {
      Log.d(TAG, "onSurfaceTextureSizeChanged(" + width + "x" + height + ")");
      // TODO: ?
    }

    @Override // will be called on UI thread
    public boolean onSurfaceTextureDestroyed(SurfaceTexture st) {
      Log.d(TAG, "onSurfaceTextureDestroyed");

      // We set the SurfaceTexture reference to null to tell the Renderer thread that
      // it needs to stop.  The renderer might be in the middle of drawing, so we want
      // to return false here so that the caller doesn't try to release the ST out
      // from under us.
      //
      // In theory.
      //
      // In 4.4, the buffer queue was changed to be synchronous, which means we block
      // in dequeueBuffer().  If the renderer has been running flat out and is currently
      // sleeping in eglSwapBuffers(), it's going to be stuck there until somebody
      // tears down the SurfaceTexture.  So we need to tear it down here to ensure
      // that the renderer thread will break.  If we don't, the thread sticks there
      // forever.
      //
      // The only down side to releasing it here is we'll get some complaints in logcat
      // when eglSwapBuffers() fails.
      synchronized (mLock) {
        mSurfaceTexture = null;
      }
      if (sReleaseInCallback) {
        Log.i(TAG, "Allowing TextureView to release SurfaceTexture");
      }
      return sReleaseInCallback;
    }

    @Override // will be called on UI thread
    public void onSurfaceTextureUpdated(SurfaceTexture st) {
      // Log.d(TAG, "onSurfaceTextureUpdated");
    }
  }
}
