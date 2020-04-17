package com.myvideoyun.giftrenderer.cameraTool;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.EGL14;
import android.opengl.GLES11Ext;

import com.myvideoyun.giftrenderer.gpuImage.MVYGLProgram;
import com.myvideoyun.giftrenderer.gpuImage.MVYGPUImageConstants;
import com.myvideoyun.giftrenderer.gpuImage.MVYGPUImageEGLContext;
import com.myvideoyun.giftrenderer.gpuImage.MVYGPUImageFilter;
import com.myvideoyun.giftrenderer.gpuImage.MVYGPUImageFramebuffer;

import java.io.IOException;
import java.nio.Buffer;

import static android.opengl.GLES20.*;
import static com.myvideoyun.giftrenderer.gpuImage.MVYGPUImageConstants.AYGPUImageRotationMode.kAYGPUImageNoRotation;
import static com.myvideoyun.giftrenderer.gpuImage.MVYGPUImageConstants.needExchangeWidthAndHeightWithRotation;
import static com.myvideoyun.giftrenderer.gpuImage.MVYGPUImageConstants.textureCoordinatesForRotation;

public class MVYCameraPreviewWrap implements SurfaceTexture.OnFrameAvailableListener {

    private static final String kAYOESTextureFragmentShader = "" +
            "#extension GL_OES_EGL_image_external : require\n" +
            "\n" +
            "varying highp vec2 textureCoordinate;\n" +
            "\n" +
            "uniform samplerExternalOES inputImageTexture;\n" +
            "\n" +
            "void main() {\n" +
            "    gl_FragColor = texture2D(inputImageTexture, textureCoordinate);\n" +
            "}";

    private Camera mCamera;

    private MVYGPUImageEGLContext eglContext;

    private SurfaceTexture surfaceTexture;

    private int oesTexture;

    private MVYCameraPreviewListener previewListener;

    private MVYGPUImageFramebuffer outputFramebuffer;

    private MVYGPUImageConstants.AYGPUImageRotationMode rotateMode = kAYGPUImageNoRotation;

    private int inputWidth;
    private int inputHeight;

    private MVYGLProgram filterProgram;

    private int filterPositionAttribute, filterTextureCoordinateAttribute;
    private int filterInputTextureUniform;

    private Buffer imageVertices = MVYGPUImageConstants.floatArrayToBuffer(MVYGPUImageConstants.imageVertices);

    public MVYCameraPreviewWrap(Camera camera) {
        mCamera = camera;
    }

    public void startPreview(MVYGPUImageEGLContext eglContext) {
        this.eglContext = eglContext;
        createGLEnvironment();

        try {
            mCamera.setPreviewTexture(surfaceTexture);
        } catch (IOException ignored) {
        }

        Camera.Size s = mCamera.getParameters().getPreviewSize();
        inputWidth = s.width;
        inputHeight = s.height;

        setRotateMode(rotateMode);

        mCamera.startPreview();
   }

   public void stopPreview() {
       destroyGLContext();
       mCamera.stopPreview();
   }

    public void setPreviewListener(MVYCameraPreviewListener previewListener) {
        this.previewListener = previewListener;
    }

    public void setRotateMode(MVYGPUImageConstants.AYGPUImageRotationMode rotateMode) {
        this.rotateMode = rotateMode;

        if (needExchangeWidthAndHeightWithRotation(rotateMode)) {
            int temp = inputWidth;
            inputWidth = inputHeight;
            inputHeight = temp;
        }
    }

    private void createGLEnvironment() {
        eglContext.syncRunOnRenderThread(() -> {
            eglContext.makeCurrent();

            oesTexture = createOESTextureID();
            surfaceTexture = new SurfaceTexture(oesTexture);
            surfaceTexture.setOnFrameAvailableListener(MVYCameraPreviewWrap.this);

            filterProgram = new MVYGLProgram(MVYGPUImageFilter.kAYGPUImageVertexShaderString, kAYOESTextureFragmentShader);
            filterProgram.link();

            filterPositionAttribute = filterProgram.attributeIndex("position");
            filterTextureCoordinateAttribute = filterProgram.attributeIndex("inputTextureCoordinate");
            filterInputTextureUniform = filterProgram.uniformIndex("inputImageTexture");

            if (previewListener != null) {
                previewListener.cameraCrateGLEnvironment();
            }
        });
    }

    @Override
    public void onFrameAvailable(final SurfaceTexture surfaceTexture) {
        eglContext.syncRunOnRenderThread(() -> {
            eglContext.makeCurrent();

            if (EGL14.eglGetCurrentDisplay() != EGL14.EGL_NO_DISPLAY) {
                surfaceTexture.updateTexImage();

                glFinish();

                // 因为在shader中处理oes纹理需要使用到扩展类型, 必须要先转换为普通纹理再传给下一级
                renderToFramebuffer(oesTexture);

                if (previewListener != null) {
                    previewListener.cameraVideoOutput(outputFramebuffer.texture[0], inputWidth, inputHeight, surfaceTexture.getTimestamp());
                }
            }
        });
    }

    private void renderToFramebuffer(int oesTexture) {

        filterProgram.use();

        if (outputFramebuffer != null) {
            if (inputWidth != outputFramebuffer.width || inputHeight != outputFramebuffer.height) {
                outputFramebuffer.destroy();
                outputFramebuffer = null;
            }
        }

        if (outputFramebuffer == null) {
            outputFramebuffer = new MVYGPUImageFramebuffer(inputWidth, inputHeight);
        }

        outputFramebuffer.activateFramebuffer();

        glClearColor(0, 0, 0, 0);
        glClear(GL_COLOR_BUFFER_BIT);

        glActiveTexture(GL_TEXTURE2);
        glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTexture);

        glUniform1i(filterInputTextureUniform, 2);

        glEnableVertexAttribArray(filterPositionAttribute);
        glEnableVertexAttribArray(filterTextureCoordinateAttribute);

        glVertexAttribPointer(filterPositionAttribute, 2, GL_FLOAT, false, 0, imageVertices);
        glVertexAttribPointer(filterTextureCoordinateAttribute, 2, GL_FLOAT, false, 0, MVYGPUImageConstants.floatArrayToBuffer(textureCoordinatesForRotation(rotateMode)));

        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

        glDisableVertexAttribArray(filterPositionAttribute);
        glDisableVertexAttribArray(filterTextureCoordinateAttribute);
    }

    private int createOESTextureID() {
        int[] texture = new int[1];
        glGenTextures(1, texture, 0);
        glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture[0]);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_TEXTURE_MIN_FILTER);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_TEXTURE_MAG_FILTER);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_TEXTURE_WRAP_S);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_TEXTURE_WRAP_T);

        return texture[0];
    }

    private void destroyGLContext() {
        eglContext.syncRunOnRenderThread(() -> {

            filterProgram.destroy();

            if (outputFramebuffer != null) {
                outputFramebuffer.destroy();
            }

            if (previewListener != null) {
                previewListener.cameraDestroyGLEnvironment();
            }
        });
    }
}

