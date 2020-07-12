package com.myvideoyun.giftrenderer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.util.Log;

import com.myvideoyun.giftrenderer.render.MVYGPUImageConstants;
import com.myvideoyun.giftrenderer.render.MVYGPUImageEGLContext;
import com.myvideoyun.giftrenderer.render.MVYGPUImageFilter;
import com.myvideoyun.giftrenderer.render.filter.MVYGPUImageEffectFilter;
import com.myvideoyun.giftrenderer.render.filter.MVYGPUImageEffectPlayFinishListener;
import com.myvideoyun.giftrenderer.render.filter.convert.MVYGPUImageTextureInput;
import com.myvideoyun.giftrenderer.render.filter.convert.MVYGPUImageTextureOutput;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static android.opengl.GLES20.GL_FRAMEBUFFER;
import static android.opengl.GLES20.GL_FRAMEBUFFER_BINDING;
import static android.opengl.GLES20.GL_RENDERBUFFER;
import static android.opengl.GLES20.GL_RENDERBUFFER_BINDING;
import static android.opengl.GLES20.GL_RGBA;
import static android.opengl.GLES20.GL_UNSIGNED_BYTE;
import static android.opengl.GLES20.GL_VERTEX_ATTRIB_ARRAY_ENABLED;
import static android.opengl.GLES20.GL_VIEWPORT;
import static android.opengl.GLES20.glBindFramebuffer;
import static android.opengl.GLES20.glBindRenderbuffer;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGetIntegerv;
import static android.opengl.GLES20.glGetVertexAttribiv;
import static android.opengl.GLES20.glReadPixels;
import static android.opengl.GLES20.glViewport;
import static com.myvideoyun.giftrenderer.render.MVYGPUImageConstants.AYGPUImageRotationMode.kAYGPUImageRotateLeft;
import static com.myvideoyun.giftrenderer.render.MVYGPUImageConstants.AYGPUImageRotationMode.kAYGPUImageRotateRight;
import static com.myvideoyun.giftrenderer.render.MVYGPUImageConstants.TAG;

public class MVYAnimHandler {

    private MVYGPUImageEGLContext eglContext;
    private SurfaceTexture surfaceTexture;

    protected MVYGPUImageTextureInput textureInput;
    protected MVYGPUImageTextureOutput textureOutput;

    protected MVYGPUImageFilter commonInputFilter;
    protected MVYGPUImageFilter commonOutputFilter;

    protected MVYGPUImageEffectFilter effectFilter;

    protected boolean initCommonProcess = false;
    protected boolean initProcess = false;

    private int[] bindingFrameBuffer = new int[1];
    private int[] bindingRenderBuffer = new int[1];
    private int[] viewPoint = new int[4];
    private int vertexAttribEnableArraySize = 5;
    private ArrayList<Integer> vertexAttribEnableArray = new ArrayList(vertexAttribEnableArraySize);

    public MVYAnimHandler(final Context context) {
        this(context, true);
    }

    public MVYAnimHandler(final Context context, boolean useCurrentEGLContext) {

        eglContext = new MVYGPUImageEGLContext();
        if (useCurrentEGLContext) {
            if (EGL14.eglGetCurrentContext() == null) {
                surfaceTexture = new SurfaceTexture(0);
                eglContext.initWithEGLWindow(surfaceTexture);
            } else {
                Log.d(TAG, "EGL Enviroment is initialied.");
            }
        } else {
            surfaceTexture = new SurfaceTexture(0);
            eglContext.initWithEGLWindow(surfaceTexture);
        }

        eglContext.syncRunOnRenderThread(new Runnable(){
            @Override
            public void run() {
                textureInput = new MVYGPUImageTextureInput(eglContext);
                textureOutput = new MVYGPUImageTextureOutput(eglContext);

                commonInputFilter = new MVYGPUImageFilter(eglContext);
                commonOutputFilter = new MVYGPUImageFilter(eglContext);

                effectFilter = new MVYGPUImageEffectFilter(eglContext);
            }
        });
    }

    public void setAssetPath(String effectPath) {
        File file = new File(effectPath);
        if (!file.exists() && !effectPath.equals("")) {
            Log.e("MVYGiftRenderer", "Invalid asset path");
            return;
        }
        if (effectFilter != null) {
            effectFilter.setEffectPath(effectPath);
        }
    }

    public void setAssetPlayCount(int effectPlayCount) {
        if (effectFilter != null) {
            effectFilter.setEffectPlayCount(effectPlayCount);
        }
    }

    public void setAssetPlayFinishListener(MVYGPUImageEffectPlayFinishListener effectPlayFinishListener) {
        if (effectFilter != null) {
            effectFilter.setEffectPlayFinishListener(effectPlayFinishListener);
        }
    }

    public void pauseAsset() {
        if (effectFilter != null) {
            effectFilter.pause();
        }
    }

    public void resumeAsset() {
        if (effectFilter != null) {
            effectFilter.resume();
        }
    }

    public void setRotateMode(MVYGPUImageConstants.AYGPUImageRotationMode rotateMode) {
        this.textureInput.setRotateMode(rotateMode);

        if (rotateMode == kAYGPUImageRotateLeft) {
            rotateMode = kAYGPUImageRotateRight;
        }else if (rotateMode == kAYGPUImageRotateRight) {
            rotateMode = kAYGPUImageRotateLeft;
        }

        this.textureOutput.setRotateMode(rotateMode);
    }

    protected void commonProcess(boolean useDelay) {

        if (!initCommonProcess) {
            List<MVYGPUImageFilter> filterChainArray = new ArrayList<MVYGPUImageFilter>();

            if (effectFilter != null) {
                filterChainArray.add(effectFilter);
            }

            if (filterChainArray.size() > 0) {
                commonInputFilter.addTarget(filterChainArray.get(0));
                for (int x = 0; x < filterChainArray.size() - 1; x++) {
                    filterChainArray.get(x).addTarget(filterChainArray.get(x+1));
                }
                filterChainArray.get(filterChainArray.size()-1).addTarget(commonOutputFilter);

            }else {
                commonInputFilter.addTarget(commonOutputFilter);
            }

            initCommonProcess = true;
        }
    }

    public void processWithTexture(final int texture, final int width, final int height) {
        eglContext.syncRunOnRenderThread(new Runnable() {
            @Override
            public void run() {
                eglContext.makeCurrent();

                saveOpenGLState();

                commonProcess(false);

                if (!initProcess) {
                    textureInput.addTarget(commonInputFilter);
                    commonOutputFilter.addTarget(textureOutput);
                    initProcess = true;
                }

                // 设置输出的Filter
                textureOutput.setOutputWithBGRATexture(texture, width, height);

                // 设置输入的Filter, 同时开始处理纹理数据
                textureInput.processWithBGRATexture(texture, width, height);

                restoreOpenGLState();
            }
        });
    }

    public Bitmap getCurrentImage(final int width, final int height) {
        final ByteBuffer byteBuffer = ByteBuffer.allocate(width*height*4);

        eglContext.syncRunOnRenderThread(new Runnable() {
            @Override
            public void run() {
                glReadPixels(0,0,width,height,GL_RGBA, GL_UNSIGNED_BYTE, byteBuffer);
            }
        });
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(byteBuffer);
        Matrix matrix = new Matrix();
        matrix.setScale(1, -1);
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);

        return bitmap;
    }

    private void saveOpenGLState() {
        // 获取当前绑定的FrameBuffer
        glGetIntegerv(GL_FRAMEBUFFER_BINDING, bindingFrameBuffer, 0);

        // 获取当前绑定的RenderBuffer
        glGetIntegerv(GL_RENDERBUFFER_BINDING, bindingRenderBuffer, 0);

        // 获取viewpoint
        glGetIntegerv(GL_VIEWPORT, viewPoint, 0);

        // 获取顶点数据
        vertexAttribEnableArray.clear();
        for (int x = 0 ; x < vertexAttribEnableArraySize; x++) {
            int[] vertexAttribEnable = new int[1];
            glGetVertexAttribiv(x, GL_VERTEX_ATTRIB_ARRAY_ENABLED, vertexAttribEnable, 0);
            if (vertexAttribEnable[0] != 0) {
                vertexAttribEnableArray.add(x);
            }
        }
    }

    private void restoreOpenGLState() {
        // 还原当前绑定的FrameBuffer
        glBindFramebuffer(GL_FRAMEBUFFER, bindingFrameBuffer[0]);

        // 还原当前绑定的RenderBuffer
        glBindRenderbuffer(GL_RENDERBUFFER, bindingRenderBuffer[0]);

        // 还原viewpoint
        glViewport(viewPoint[0], viewPoint[1], viewPoint[2], viewPoint[3]);

        // 还原顶点数据
        for (int x = 0 ; x < vertexAttribEnableArray.size(); x++) {
            glEnableVertexAttribArray(vertexAttribEnableArray.get(x));
        }
    }

    public void destroy() {
        if (eglContext != null) {
            eglContext.syncRunOnRenderThread(new Runnable() {
                @Override
                public void run() {
                    eglContext.makeCurrent();

                    textureInput.destroy();
                    textureOutput.destroy();

                    commonInputFilter.destroy();
                    commonOutputFilter.destroy();

                    if (effectFilter != null) {
                        effectFilter.destroy();
                    }

                    if (surfaceTexture != null) {
                        surfaceTexture.release();
                    }

                    eglContext.destroyEGLWindow();
                }
            });
        }
    }
}
