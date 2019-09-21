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
package com.myvideoyun.TinyGiftRenderer;

import android.content.Context;
import android.util.Log;

import com.myvideoyun.TinyGiftRenderer.render.IEventListener;
import com.myvideoyun.TinyGiftRenderer.render.WeakEventListener;

/** The java wrapper of the giftRenderer */
public class TinyGiftRenderer {
  private static final String TAG = "TGR.render";
  public static final int MSG_ERROR_GL_ENVIRONMENT = 0xFE100001; // error in gl context
  public static final int MSG_GIFT_RENDER_END = 0x00040000; // end of the gift rendering
  public static final int MSG_GIFT_RENDER_START = 0x00080000; // start of the gift rendering
  public static final int MSG_TYPE_INFO = 4;
  public static final int MSG_TYPE_ERROR = 6;

  public static final WeakEventListener mWeakAuthListener = new WeakEventListener();
  public static final WeakEventListener mWeakRenderListener = new WeakEventListener();

  private long giftrenderer;
  private final Object LOCK = new Object();

  public TinyGiftRenderer(Context context, int vertFlip) {
    giftrenderer = _createGiftRenderer(context, 0, vertFlip);
    if (giftrenderer == 0) Log.e(TAG, "Create gift renderer failed!");
    else Log.d(TAG, "create gift renderer :" + giftrenderer);
  }

  public void setGiftPath(String effect) {
    _setGiftPath(giftrenderer, effect);
  }

  /**
   * render the gift over the backgroud texture.
   *
   * @param bgTexId backgroud texture id, usually from camera or video, and it's dynamically
   *     updated;
   * @param width width
   * @param height height
   * @return texture id
   */
  public int renderGift(int bgTexId, int width, int height) {
    return _renderGift(giftrenderer, bgTexId, width, height);
  }

  public int setModelView(float[] m) {
    return _setModelView(giftrenderer, m);
  }

  public int pauseRendering() {
    return _pause(giftrenderer, 1);
  }

  public int resumeRendering() {
    return _pause(giftrenderer, 0);
  }

  public int setRenderParams(String paramName, int value) {
    return _setRenderParams(giftrenderer, paramName, value);
  }

  public void releaseGLResources() {
    synchronized (LOCK) {
      _releaseGLResources(giftrenderer);
    }
  }

  @Override
  protected void finalize() throws Throwable {
    release();
    super.finalize();
  }

  public void release() {
    synchronized (LOCK) {
      _release(giftrenderer);
      giftrenderer = 0;
    }
  }

  public void setRenderEventListener(IEventListener listener) {
    mWeakRenderListener.setEventListener(listener);
    _setRenderEventListener(giftrenderer, mWeakRenderListener);
  }

  public static void setAuthEventListener(IEventListener listener) {
    mWeakAuthListener.setEventListener(listener);
    _setAuthEventListener(mWeakAuthListener);
  }

  public static int auth(Context context, String authKey, int authKeyLength) {
    return _auth(context.getApplicationContext(), authKey, authKeyLength);
  }

  private static native int _auth(Context context, String authKey, int authKeyLength);

  private static native long _createGiftRenderer(Context context, int type, int vertFlip);

  private static native int _setGiftPath(long id, String effect);

  private static native int _renderGift(long id, int texture, int width, int height);

  private static native void _setRenderEventListener(long id, IEventListener listener);

  private static native void _setAuthEventListener(IEventListener listener);

  private static native int _release(long id);

  private static native int _releaseGLResources(long id);

  private static native int _pause(long id, int pause);

  private static native int _setRenderParams(long id, String key, long value);

  private static native int _setModelView(long id,  float[] m);

  static {
    System.loadLibrary("giftrenderer");
    System.loadLibrary("giftRendererJni");
  }
}
