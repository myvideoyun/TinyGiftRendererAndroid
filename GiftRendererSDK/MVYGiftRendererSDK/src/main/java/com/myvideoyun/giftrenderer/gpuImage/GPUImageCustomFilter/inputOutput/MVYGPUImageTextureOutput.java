package com.myvideoyun.giftrenderer.gpuImage.GPUImageCustomFilter.inputOutput;

import android.util.Log;

import com.myvideoyun.giftrenderer.gpuImage.MVYGLProgram;
import com.myvideoyun.giftrenderer.gpuImage.MVYGPUImageConstants;
import com.myvideoyun.giftrenderer.gpuImage.MVYGPUImageEGLContext;
import com.myvideoyun.giftrenderer.gpuImage.MVYGPUImageFramebuffer;
import com.myvideoyun.giftrenderer.gpuImage.MVYGPUImageInput;

import java.nio.Buffer;

import static android.opengl.GLES20.*;
import static com.myvideoyun.giftrenderer.gpuImage.MVYGPUImageConstants.AYGPUImageRotationMode.kAYGPUImageNoRotation;
import static com.myvideoyun.giftrenderer.gpuImage.MVYGPUImageConstants.TAG;
import static com.myvideoyun.giftrenderer.gpuImage.MVYGPUImageFilter.kAYGPUImagePassthroughFragmentShaderString;
import static com.myvideoyun.giftrenderer.gpuImage.MVYGPUImageFilter.kAYGPUImageVertexShaderString;

public class MVYGPUImageTextureOutput implements MVYGPUImageInput {

    private MVYGPUImageEGLContext context;
    private Buffer imageVertices = MVYGPUImageConstants.floatArrayToBuffer(MVYGPUImageConstants.imageVertices);

    protected MVYGPUImageFramebuffer firstInputFramebuffer;

    protected MVYGLProgram filterProgram;

    protected int filterPositionAttribute, filterTextureCoordinateAttribute;
    protected int filterInputTextureUniform;

    protected int inputWidth;
    protected int inputHeight;

    private MVYGPUImageConstants.AYGPUImageRotationMode rotateMode = kAYGPUImageNoRotation;

    private int[] framebuffer = new int[1];
    public int[] texture = new int[1];

    public MVYGPUImageTextureOutput(MVYGPUImageEGLContext context) {
        this.context = context;
        context.syncRunOnRenderThread(new Runnable() {
            @Override
            public void run() {
                filterProgram = new MVYGLProgram(kAYGPUImageVertexShaderString, kAYGPUImagePassthroughFragmentShaderString);
                filterProgram.link();

                filterPositionAttribute = filterProgram.attributeIndex("position");
                filterTextureCoordinateAttribute = filterProgram.attributeIndex("inputTextureCoordinate");
                filterInputTextureUniform = filterProgram.uniformIndex("inputImageTexture");
                filterProgram.use();
            }
        });
    }

    protected void renderToTexture(final Buffer vertices, final Buffer textureCoordinates) {
        context.syncRunOnRenderThread(new Runnable() {
            @Override
            public void run() {
                filterProgram.use();

                glBindFramebuffer(GL_FRAMEBUFFER, framebuffer[0]);

                glBindTexture(GL_TEXTURE_2D, texture[0]);
                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, inputWidth, inputHeight, 0, GL_RGBA, GL_UNSIGNED_BYTE, null);
                glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texture[0], 0);

                glViewport(0, 0, inputWidth, inputHeight);

                glClearColor(0, 0, 0, 0);
                glClear(GL_COLOR_BUFFER_BIT);

                glActiveTexture(GL_TEXTURE2);
                glBindTexture(GL_TEXTURE_2D, firstInputFramebuffer.texture[0]);

                glUniform1i(filterInputTextureUniform, 2);

                glEnableVertexAttribArray(filterPositionAttribute);
                glEnableVertexAttribArray(filterTextureCoordinateAttribute);

                glVertexAttribPointer(filterPositionAttribute, 2, GL_FLOAT, false, 0, vertices);
                glVertexAttribPointer(filterTextureCoordinateAttribute, 2, GL_FLOAT, false, 0, textureCoordinates);

                glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

                glDisableVertexAttribArray(filterPositionAttribute);
                glDisableVertexAttribArray(filterTextureCoordinateAttribute);
            }
        });
    }

    public void setOutputWithBGRATexture(final int textureId, int width, int height) {
        this.texture = new int[]{textureId};
        this.inputWidth = width;
        this.inputHeight = height;

        context.syncRunOnRenderThread(new Runnable(){
            @Override
            public void run() {
                if (framebuffer[0] == 0){
                    glGenFramebuffers(1, framebuffer, 0);
                    Log.d(TAG, "创建一个 OpenGL frameBuffer " + framebuffer[0]);
                }
            }
        });
    }

    public void setRotateMode(MVYGPUImageConstants.AYGPUImageRotationMode rotateMode) {
        this.rotateMode = rotateMode;
    }

    public void destroy() {
        context.syncRunOnRenderThread(new Runnable() {
            @Override
            public void run() {
                filterProgram.destroy();

                if (framebuffer[0] != 0){
                    Log.d(TAG, "销毁一个 OpenGL frameBuffer " + framebuffer[0]);
                    glDeleteFramebuffers(1, framebuffer, 0);
                    framebuffer[0] = 0;
                }
            }
        });
    }

    @Override
    public void setInputSize(int width, int height) {

    }

    @Override
    public void setInputFramebuffer(MVYGPUImageFramebuffer newInputFramebuffer) {
        firstInputFramebuffer = newInputFramebuffer;
    }

    @Override
    public void newFrameReady() {
        renderToTexture(imageVertices,  MVYGPUImageConstants.floatArrayToBuffer(MVYGPUImageConstants.textureCoordinatesForRotation(rotateMode)));
    }
}
