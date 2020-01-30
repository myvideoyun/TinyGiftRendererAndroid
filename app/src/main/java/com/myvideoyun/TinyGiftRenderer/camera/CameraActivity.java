/*
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
import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.myvideoyun.TinyGiftRenderer.TinyGiftRenderer;
import com.myvideoyun.TinyGiftRenderer.R;
import com.myvideoyun.TinyGiftRenderer.render.IEventListener;

import java.io.IOException;
import java.lang.ref.WeakReference;

public class CameraActivity extends AppCompatActivity
    implements SurfaceTexture.OnFrameAvailableListener {
  private static final String TAG = "TGR.cameraActivity";
  private Camera mCamera;
  private Camera.Parameters mCameraParameters;
  private int mCameraPreviewWidth, mCameraPreviewHeight;
  private CameraHandler mCameraHandler = new CameraHandler(this);
  private CameraSurfaceRenderer mCamRender = null;
  private int mFacing = 1; // facing camera
  private GLSurfaceView mGLView;
  private int mCameraNum = 0;
  private int mCurrCameraID = 0;

  private Runnable start =
      new Runnable() {
        @Override
        public void run() {
          setContentView(R.layout.activity_camera);
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
                  "jAwdRWLiAhQN3lJ2zfJv7e0ORshb3drGJurQHHFGmLF8YkXh4GV6s2QgaHbAzttAPBui2F+tPJqDw9HaIyqYtA==",
                  64);
          Log.e(TAG, "id:" + id);
        }
      };

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    Log.d(TAG, "crate the activity");
    super.onCreate(savedInstanceState);
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
    setContentView(R.layout.activity_camera);

    mCamRender = new CameraSurfaceRenderer(mCameraHandler, getApplicationContext());
    mGLView = (GLSurfaceView) findViewById(R.id.mSurface);
    mGLView.setEGLContextClientVersion(2);
    mGLView.setRenderer(mCamRender);
    mGLView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY); // todo: checkit
  }

  @Override
  public void onRequestPermissionsResult(
      int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    PermissionUtils.onRequestPermissionsResult(
        requestCode == 10,
        grantResults,
        start,
        new Runnable() {
          @Override
          public void run() {
            finish();
            Toast.makeText(
                    CameraActivity.this, "Can not get required permission", Toast.LENGTH_SHORT)
                .show();
          }
        });
  }

  @Override
  protected void onResume() {
    Log.d(TAG, "onResume");
    super.onResume();
    openCamera();
    mCamRender.setLandScape(false);
    mGLView.queueEvent(
        new Runnable() {
          @Override
          public void run() {
            mCamRender.setPreviewDim(mCameraPreviewWidth, mCameraPreviewHeight);
          }
        });
  }

  @Override
  protected void onPause() {
    Log.d(TAG, "onPause");
    super.onPause();
    releaseCamera();
  }

  @Override
  protected void onStart() {
    Log.d(TAG, "onStart");
    super.onStart();
    mCamRender.createGiftRender();
    mGLView.onResume();
  }

  @Override
  protected void onStop() {
    Log.d(TAG, "onStop");
    super.onStop();
    mGLView.queueEvent(
        new Runnable() {
          @Override
          public void run() {
            mCamRender.releaseGLResources();
          }
        });
    mGLView.onPause();
  }

  @Override
  protected void onDestroy() {
    Log.d(TAG, "onDestroy");
    super.onDestroy();
    mCameraHandler.invalidateHandler();
  }

  boolean isCameraOpened() {
    return mCamera != null;
  }

  public void openCamera() {
    openCamera(720, 1280);
  }

  int getCameraCount() {
    return Camera.getNumberOfCameras();
  }

  /**
   * Opens a camera, and attempts to establish preview mode at the specified width and height.
   *
   * <p>
   *
   * <p>Sets mCameraPreviewWidth and mCameraPreviewHeight to the actual width/height of the preview.
   */
  private void openCamera(int desiredWidth, int desiredHeight) {
    Log.i(
        TAG,
        "openCamera() called with: desiredWidth = ["
            + desiredWidth
            + "], desiredHeight = ["
            + desiredHeight
            + "]");
    if (mCamera != null) {
      throw new RuntimeException("camera already initialized");
    }

    Camera.CameraInfo info = new Camera.CameraInfo();

    // Try to find a front-facing camera (e.g. for videoconferencing).
    mCameraNum = Camera.getNumberOfCameras();
    int numCameras = Camera.getNumberOfCameras();
    for (int i = 0; i < numCameras; i++) {
      Camera.getCameraInfo(i, info);
      if (info.facing == mFacing) {
        mCamera = Camera.open(i);
        mCurrCameraID = i;
        break;
      }
    }
    if (mCamera == null) {
      Log.d(TAG, "No front-facing camera found; opening default");
      mCamera = Camera.open(); // opens first back-facing camera
      mCurrCameraID = 0;
    }
    if (mCamera == null) {
      throw new RuntimeException("Unable to open camera");
    }

    Camera.Parameters parms = mCamera.getParameters();
    mCameraParameters = parms;

    // CameraUtils.choosePreviewSize(false, parms, desiredWidth, desiredHeight);

    // Give the camera a hint that we're recording video.  This can have a big
    // impact on frame rate.
    parms.setRecordingHint(true);

    // leave the frame rate set to default
    // int orientation = (orientation + 45) / 90 * 90;
    // parms.setRotation(90);
    mCamera.setParameters(parms);

    int[] fpsRange = new int[2];
    Camera.Size mCameraPreviewSize = parms.getPreviewSize();
    parms.getPreviewFpsRange(fpsRange);
    String previewFacts = mCameraPreviewSize.width + "x" + mCameraPreviewSize.height;
    if (fpsRange[0] == fpsRange[1]) {
      previewFacts += " @" + (fpsRange[0] / 1000.0) + "fps";
    } else {
      previewFacts += " @[" + (fpsRange[0] / 1000.0) + " - " + (fpsRange[1] / 1000.0) + "] fps";
    }

    mCameraPreviewWidth = mCameraPreviewSize.width;
    mCameraPreviewHeight = mCameraPreviewSize.height;
    Log.i(TAG, "openCamera: Camera Opened" + previewFacts);
  }

  /** Stops camera preview, and releases the camera to the system. */
  private void releaseCamera() {
    if (mCamera != null) {
      mCamera.stopPreview();
      mCamera.release();
      mCamera = null;
      Log.i(TAG, "releaseCamera -- done");
    }
  }

  /** Connects the SurfaceTexture to the Camera preview output, and starts the preview. */
  private void handleSetSurfaceTexture(SurfaceTexture st) {
    Log.i(TAG, "handleSetSurfaceTexture() called with: st = [" + st + "] camera " + mCamera);
    st.setOnFrameAvailableListener(this);
    try {
      mCamera.setPreviewTexture(st);
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
    // mCamera.setDisplayOrientation(90); // for portrait view.
    // setCameraDisplayOrientation(mCurrCameraID, mCamera);
    mCamera.startPreview();
  }

  @Override
  public void onFrameAvailable(SurfaceTexture st) {
    // The SurfaceTexture uses this to signal the availability of a new frame.  The
    // thread that "owns" the external texture associated with the SurfaceTexture (which,
    // by virtue of the context being shared, *should* be either one) needs to call
    // updateTexImage() to latch the buffer.
    //
    // Once the buffer is latched, the GLSurfaceView thread can signal the encoder thread.
    // This feels backward -- we want recording to be prioritized over rendering -- but
    // since recording is only enabled some of the time it's easier to do it this way.
    //
    // Since GLSurfaceView doesn't establish a Looper, this will *probably* execute on
    // the main UI thread.  Fortunately, requestRender() can be called from any thread,
    // so it doesn't really matter.
    mCamRender.setFrameAvailable(st);
    mGLView.requestRender();
  }

  /**
   * Handles camera operation requests from other threads. Necessary because the Camera must only be
   * accessed from one thread.
   *
   * <p>
   *
   * <p>The object is created on the UI thread, and all handlers run there. Messages are sent from
   * other threads, using sendMessage().
   */
  static class CameraHandler extends Handler {
    public static final int MSG_SET_SURFACE_TEXTURE = 0;

    // Weak reference to the Activity; only access this from the UI thread.
    private WeakReference<CameraActivity> mWeakActivity;

    public CameraHandler(CameraActivity activity) {
      mWeakActivity = new WeakReference<CameraActivity>(activity);
    }

    /**
     * Drop the reference to the activity. Useful as a paranoid measure to ensure that attempts to
     * access a stale Activity through a handler are caught.
     */
    public void invalidateHandler() {
      mWeakActivity.clear();
    }

    @Override // runs on UI thread
    public void handleMessage(Message inputMessage) {
      int what = inputMessage.what;
      Log.d(TAG, "CameraHandler [" + this + "]: what=" + what);

      CameraActivity activity = mWeakActivity.get();
      if (activity == null) {
        Log.w(TAG, "CameraHandler.handleMessage: activity is null");
        return;
      }

      switch (what) {
        case MSG_SET_SURFACE_TEXTURE:
          activity.handleSetSurfaceTexture((SurfaceTexture) inputMessage.obj);
          break;
        default:
          throw new RuntimeException("unknown msg " + what);
      }
    }
  }
}
