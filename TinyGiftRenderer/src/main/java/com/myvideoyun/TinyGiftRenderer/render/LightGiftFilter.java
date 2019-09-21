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

package com.myvideoyun.TinyGiftRenderer.render;

import android.content.Context;
import android.util.Log;

import com.myvideoyun.TinyGiftRenderer.glesWrapper.OffScreenFrameBuffer;
import com.myvideoyun.TinyGiftRenderer.TinyGiftRenderer;

/**
 * Gift filter
 */
public class LightGiftFilter {
    private static final String TAG = "TGR.filter";
    private TinyGiftRenderer mGift;
    private OffScreenFrameBuffer mFrameBuffer;
    private int mWidth=-1, mHeight=-1;
    private String[] effectsArray = {"assets/modelsticker/shoutao/meta.json",
                                "assets/modelsticker/dog_model/meta.json"};
    private int effetsID = 0;
    private int count = 0;
    private static float g = 0;

    /**
     * vertFlip: flag to flip the texture vertically；
     */
    public LightGiftFilter(Context context, int vertFlip) {
        super();
        mGift = new TinyGiftRenderer(context.getApplicationContext(), vertFlip);
        Log.i(TAG, "create gift renderer");
    }

    public void setGiftPath(String effect) {
        mGift.setGiftPath(effect);
    }

    public void setRenderEventListener(IEventListener listener) {
        mGift.setRenderEventListener(listener);
    }

    public void onSizeChanged(int width, int height){
        Log.i(TAG, "update dimension to width " + width + " height: " + height);
        if(mFrameBuffer != null)
            mFrameBuffer.release();
        mFrameBuffer = new OffScreenFrameBuffer(width, height);
        mWidth = width;
        mHeight = height;
    }

    public int renderToTexture(int texture) {
        if(mWidth == -1 || mHeight == -1){
            Log.e(TAG, "dimension is not initialized, should call onSizeChanged before renderToTexture\n");
            return -1;
        }

        mFrameBuffer.bind();
        mGift.renderGift(texture, mWidth, mHeight);
        mFrameBuffer.unbind();
        return mFrameBuffer.getTexture();
    }

    public int renderToScreen(int texture){
        if(mWidth == -1 || mHeight == -1){
            Log.e(TAG, "dimension is not initialized, should call onSizeChanged before renderToTexture\n");
            return -1;
        }

        float []m = {0.999559f, 0.006781f, -0.028911f, 0.000000f, -0.005831f, 0.999445f, 0.032811f, 0.000000f,
                    0.029117f, -0.032628f, 0.999043f, 0.000000f, 0.001027f, 0.004984f, -0.864680f, 1.000000f};
        mGift.setModelView(m);
        mGift.renderGift(texture, mWidth, mHeight);
        g += 0.001;

        return 0;
    }

    public void releaseGLResource() {
        mGift.releaseGLResources();
        mFrameBuffer.release();
    }

    public void releaseFilter() {
        mGift.release();
    }

}
