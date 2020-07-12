package com.myvideoyun.giftrenderer.render.filter.convert;

import com.myvideoyun.giftrenderer.render.MVYGLProgram;
import com.myvideoyun.giftrenderer.render.MVYGPUImageConstants;
import com.myvideoyun.giftrenderer.render.MVYGPUImageEGLContext;
import com.myvideoyun.giftrenderer.render.MVYGPUImageFramebuffer;
import com.myvideoyun.giftrenderer.render.MVYGPUImageInput;
import com.myvideoyun.giftrenderer.render.MVYGPUImageOutput;

import java.nio.Buffer;
import java.nio.ByteBuffer;

import static android.opengl.GLES20.*;
import static com.myvideoyun.giftrenderer.render.MVYGPUImageConstants.AYGPUImageRotationMode.kAYGPUImageNoRotation;
import static com.myvideoyun.giftrenderer.render.MVYGPUImageConstants.needExchangeWidthAndHeightWithRotation;
import static com.myvideoyun.giftrenderer.render.MVYGPUImageFilter.kAYGPUImagePassthroughFragmentShaderString;
import static com.myvideoyun.giftrenderer.render.MVYGPUImageFilter.kAYGPUImageVertexShaderString;

public class MVYGPUImageBGRADataInput extends MVYGPUImageOutput {

    private MVYGPUImageEGLContext context;

    private Buffer imageVertices = MVYGPUImageConstants.floatArrayToBuffer(MVYGPUImageConstants.imageVertices);

    protected MVYGPUImageFramebuffer outputFramebuffer;

    protected MVYGLProgram filterProgram;

    protected int filterPositionAttribute, filterTextureCoordinateAttribute;
    protected int filterInputTextureUniform;

    protected int[] inputDataTexture = {0};
    protected ByteBuffer bgraBuffer;

    private MVYGPUImageConstants.AYGPUImageRotationMode rotateMode = kAYGPUImageNoRotation;

    public MVYGPUImageBGRADataInput(MVYGPUImageEGLContext context) {
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

    public void processWithBGRAData(final byte[] bgraData, final int width, final int height, final int lineSize) {
        context.syncRunOnRenderThread(new Runnable() {
            @Override
            public void run() {

                int inputWidth = width;
                int inputHeight = height;

                if (needExchangeWidthAndHeightWithRotation(rotateMode)) {
                    int temp = width;
                    inputWidth = height;
                    inputHeight = temp;
                }

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

                if (inputDataTexture[0] == 0) {
                    glGenTextures(1, inputDataTexture, 0);
                    glBindTexture(GL_TEXTURE_2D, inputDataTexture[0]);
                    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
                    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
                    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
                    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
                    glBindTexture(GL_TEXTURE_2D, 0);
                }

                if (bgraBuffer == null) {
                    bgraBuffer = ByteBuffer.allocateDirect(lineSize * height * 4);
                }
                bgraBuffer.clear();
                bgraBuffer.put(bgraData, 0, bgraData.length);
                bgraBuffer.rewind();

                glActiveTexture(GL_TEXTURE2);
                glBindTexture(GL_TEXTURE_2D, inputDataTexture[0]);
                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, lineSize, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, bgraBuffer);

                glUniform1i(filterInputTextureUniform, 2);

                glEnableVertexAttribArray(filterPositionAttribute);
                glEnableVertexAttribArray(filterTextureCoordinateAttribute);

                float[] textureCoordinates = MVYGPUImageConstants.textureCoordinatesForRotation(rotateMode);

                // 处理lineSize != width
                for (int x = 0; x < textureCoordinates.length; x = x + 2) {
                    if (textureCoordinates[x] == 1) {
                        textureCoordinates[x] = (float)width / (float)lineSize;
                    }
                }

                glVertexAttribPointer(filterPositionAttribute, 2, GL_FLOAT, false, 0, imageVertices);
                glVertexAttribPointer(filterTextureCoordinateAttribute, 2, GL_FLOAT, false, 0, MVYGPUImageConstants.floatArrayToBuffer(MVYGPUImageConstants.textureCoordinatesForRotation(rotateMode)));

                glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

                glDisableVertexAttribArray(filterPositionAttribute);
                glDisableVertexAttribArray(filterTextureCoordinateAttribute);

                for (MVYGPUImageInput currentTarget : getTargets()) {
                    currentTarget.setInputSize(inputWidth, inputHeight);
                    currentTarget.setInputFramebuffer(outputFramebuffer);
                }

                for (MVYGPUImageInput currentTarget : getTargets()) {
                    currentTarget.newFrameReady();
                }
            }
        });
    }

    public void setRotateMode(MVYGPUImageConstants.AYGPUImageRotationMode rotateMode) {
        this.rotateMode = rotateMode;
    }

    public void destroy() {
        removeAllTargets();

        context.syncRunOnRenderThread(new Runnable() {
            @Override
            public void run() {
                filterProgram.destroy();

                if (outputFramebuffer != null) {
                    outputFramebuffer.destroy();
                }
            }
        });
    }
}
