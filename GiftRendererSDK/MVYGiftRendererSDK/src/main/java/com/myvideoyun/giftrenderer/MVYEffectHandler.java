package com.myvideoyun.giftrenderer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.util.Log;

import com.myvideoyun.giftrenderer.gpuImage.MVYGPUImageEGLContext;
import com.myvideoyun.giftrenderer.gpuImage.GPUImageCustomFilter.MVYGPUImageDelayFilter;
import com.myvideoyun.giftrenderer.gpuImage.GPUImageCustomFilter.MVYGPUImageEffectFilter;
import com.myvideoyun.giftrenderer.gpuImage.GPUImageCustomFilter.MVYGPUImageEffectPlayFinishListener;
import com.myvideoyun.giftrenderer.gpuImage.GPUImageCustomFilter.MVYGPUImageLookupFilter;
import com.myvideoyun.giftrenderer.gpuImage.GPUImageCustomFilter.inputOutput.MVYGPUImageBGRADataInput;
import com.myvideoyun.giftrenderer.gpuImage.GPUImageCustomFilter.inputOutput.MVYGPUImageBGRADataOutput;
import com.myvideoyun.giftrenderer.gpuImage.GPUImageCustomFilter.inputOutput.MVYGPUImageI420DataInput;
import com.myvideoyun.giftrenderer.gpuImage.GPUImageCustomFilter.inputOutput.MVYGPUImageI420DataOutput;
import com.myvideoyun.giftrenderer.gpuImage.GPUImageCustomFilter.inputOutput.MVYGPUImageTextureInput;
import com.myvideoyun.giftrenderer.gpuImage.GPUImageCustomFilter.inputOutput.MVYGPUImageTextureOutput;
import com.myvideoyun.giftrenderer.gpuImage.MVYGPUImageConstants;
import com.myvideoyun.giftrenderer.gpuImage.MVYGPUImageFilter;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static android.opengl.GLES20.*;
import static com.myvideoyun.giftrenderer.gpuImage.MVYGPUImageConstants.AYGPUImageRotationMode.kAYGPUImageRotateLeft;
import static com.myvideoyun.giftrenderer.gpuImage.MVYGPUImageConstants.AYGPUImageRotationMode.kAYGPUImageRotateRight;
import static com.myvideoyun.giftrenderer.gpuImage.MVYGPUImageConstants.TAG;

public class MVYEffectHandler {

    private MVYGPUImageEGLContext eglContext;
    private SurfaceTexture surfaceTexture;

    protected MVYGPUImageTextureInput textureInput;
    protected MVYGPUImageTextureOutput textureOutput;

    protected MVYGPUImageI420DataInput i420DataInput;
    protected MVYGPUImageI420DataOutput i420DataOutput;

    protected MVYGPUImageBGRADataInput bgraDataInput;
    protected MVYGPUImageBGRADataOutput bgraDataOutput;

    protected MVYGPUImageFilter commonInputFilter;
    protected MVYGPUImageFilter commonOutputFilter;

    protected MVYGPUImageDelayFilter delayFilter;
    protected MVYGPUImageLookupFilter lookupFilter;
    protected MVYGPUImageEffectFilter effectFilter;

    protected boolean initCommonProcess = false;
    protected boolean initProcess = false;

    private int[] bindingFrameBuffer = new int[1];
    private int[] bindingRenderBuffer = new int[1];
    private int[] viewPoint = new int[4];
    private int vertexAttribEnableArraySize = 5;
    private ArrayList<Integer> vertexAttribEnableArray = new ArrayList(vertexAttribEnableArraySize);

    public MVYEffectHandler(final Context context) {
        this(context, true);
    }

    public MVYEffectHandler(final Context context, boolean useCurrentEGLContext) {

        eglContext = new MVYGPUImageEGLContext();
        if (useCurrentEGLContext) {
            if (EGL14.eglGetCurrentContext() == null) {
                surfaceTexture = new SurfaceTexture(0);
                eglContext.initWithEGLWindow(surfaceTexture);
            } else {
                Log.d(TAG, "No need to initial EGL enviroment");
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

                i420DataInput = new MVYGPUImageI420DataInput(eglContext);
                i420DataOutput = new MVYGPUImageI420DataOutput(eglContext);

                bgraDataInput = new MVYGPUImageBGRADataInput(eglContext);
                bgraDataOutput = new MVYGPUImageBGRADataOutput(eglContext);

                commonInputFilter = new MVYGPUImageFilter(eglContext);
                commonOutputFilter = new MVYGPUImageFilter(eglContext);

                delayFilter = new MVYGPUImageDelayFilter(eglContext);

                try {
                    Bitmap lookupBitmap = BitmapFactory.decodeStream(context.getAssets().open("lookup.png"));
                    lookupFilter = new MVYGPUImageLookupFilter(eglContext,lookupBitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                effectFilter = new MVYGPUImageEffectFilter(eglContext);
            }
        });
    }

    public void setEffectPath(String effectPath) {
        File file = new File(effectPath);
        if (!file.exists() && !effectPath.equals("")) {
            Log.e("TGR", "Invalid asset path: " + effectPath);
            return;
        }
        if (effectFilter != null) {
            effectFilter.setEffectPath(effectPath);
        }
    }

    public void setEffectPlayCount(int effectPlayCount) {
        if (effectFilter != null) {
            effectFilter.setEffectPlayCount(effectPlayCount);
        }
    }

    public void setEffectPlayFinishListener(MVYGPUImageEffectPlayFinishListener effectPlayFinishListener) {
        if (effectFilter != null) {
            effectFilter.setEffectPlayFinishListener(effectPlayFinishListener);
        }
    }

    public void pauseEffect() {
        if (effectFilter != null) {
            effectFilter.pause();
        }
    }

    public void resumeEffect() {
        if (effectFilter != null) {
            effectFilter.resume();
        }
    }

    public void setStyle(Bitmap lookup) {
        if (lookupFilter != null) {
            lookupFilter.setLookup(lookup);
        }
    }

    public void setIntensityOfStyle(float intensity) {
        if (lookupFilter != null) {
            lookupFilter.setIntensity(intensity);
        }
    }

    public void setRotateMode(MVYGPUImageConstants.AYGPUImageRotationMode rotateMode) {
        this.textureInput.setRotateMode(rotateMode);
        this.i420DataInput.setRotateMode(rotateMode);
        this.bgraDataInput.setRotateMode(rotateMode);

        if (rotateMode == kAYGPUImageRotateLeft) {
            rotateMode = kAYGPUImageRotateRight;
        }else if (rotateMode == kAYGPUImageRotateRight) {
            rotateMode = kAYGPUImageRotateLeft;
        }

        this.textureOutput.setRotateMode(rotateMode);
        this.i420DataOutput.setRotateMode(rotateMode);
        this.bgraDataOutput.setRotateMode(rotateMode);
    }

    protected void commonProcess(boolean useDelay) {

        if (!initCommonProcess) {
            List<MVYGPUImageFilter> filterChainArray = new ArrayList<MVYGPUImageFilter>();

            if (useDelay && delayFilter != null) {
                filterChainArray.add(delayFilter);
            }

            if (lookupFilter != null) {
                filterChainArray.add(lookupFilter);
            }

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

    public void processWithYUVData(final byte[] yuvData, final int width, final int height) {
        eglContext.syncRunOnRenderThread(new Runnable() {
            @Override
            public void run() {
                eglContext.makeCurrent();

                saveOpenGLState();

                commonProcess(true);

                if (!initProcess) {
                    i420DataInput.addTarget(commonInputFilter);
                    commonOutputFilter.addTarget(i420DataOutput);
                    initProcess = true;
                }

                // 设置输出的Filter
                i420DataOutput.setOutputWithYUVData(yuvData, width, height, width);

                // 设置输入的Filter, 同时开始处理纹理数据
                i420DataInput.processWithYUV(yuvData, width, height, width);

                restoreOpenGLState();
            }
        });
    }

    public void processWithBGRAData(final byte[] bgraData, final int width, final int height) {
        eglContext.syncRunOnRenderThread(new Runnable() {
            @Override
            public void run() {
                eglContext.makeCurrent();

                saveOpenGLState();

                commonProcess(false);

                if (!initProcess) {
                    bgraDataInput.addTarget(commonInputFilter);
                    commonOutputFilter.addTarget(bgraDataOutput);
                    initProcess = true;
                }

                // 设置输出的Filter
                bgraDataOutput.setOutputWithBGRAData(bgraData, width, height, width);

                // 设置输入的Filter, 同时开始处理纹理数据
                bgraDataInput.processWithBGRAData(bgraData, width, height, width);

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
                    i420DataInput.destroy();
                    i420DataOutput.destroy();
                    bgraDataInput.destroy();
                    bgraDataOutput.destroy();
                    commonInputFilter.destroy();
                    commonOutputFilter.destroy();

                    if (delayFilter != null) {
                        delayFilter.destroy();
                    }

                    if (lookupFilter != null) {
                        lookupFilter.destroy();
                    }
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
