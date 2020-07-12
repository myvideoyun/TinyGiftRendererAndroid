package com.myvideoyun.giftrenderer.render.filter.convert;

import com.aiyaapp.aiya.AYYuvUtil;
import com.myvideoyun.giftrenderer.render.MVYGLProgram;
import com.myvideoyun.giftrenderer.render.MVYGPUImageConstants;
import com.myvideoyun.giftrenderer.render.MVYGPUImageEGLContext;
import com.myvideoyun.giftrenderer.render.MVYGPUImageFramebuffer;
import com.myvideoyun.giftrenderer.render.MVYGPUImageInput;

import java.nio.Buffer;
import java.nio.ByteBuffer;

import static android.opengl.GLES20.*;
import static com.myvideoyun.giftrenderer.render.MVYGPUImageConstants.AYGPUImageRotationMode.kAYGPUImageNoRotation;
import static com.myvideoyun.giftrenderer.render.MVYGPUImageFilter.kAYGPUImagePassthroughFragmentShaderString;
import static com.myvideoyun.giftrenderer.render.MVYGPUImageFilter.kAYGPUImageVertexShaderString;

public class MVYGPUImageI420DataOutput implements MVYGPUImageInput {

    private MVYGPUImageEGLContext context;

    private Buffer imageVertices = MVYGPUImageConstants.floatArrayToBuffer(MVYGPUImageConstants.imageVertices);

    protected MVYGPUImageFramebuffer outputFramebuffer;
    protected MVYGPUImageFramebuffer firstInputFramebuffer;

    protected MVYGLProgram filterProgram;

    protected int filterPositionAttribute, filterTextureCoordinateAttribute;
    protected int filterInputTextureUniform;

    protected byte[] outputYUVData;
    protected int outputWidth;
    protected int outputHeight;
    protected int outputLineSize;

    private ByteBuffer bgraBuffer;
    private ByteBuffer yuvBuffer;

    private MVYGPUImageConstants.AYGPUImageRotationMode rotateMode = kAYGPUImageNoRotation;

    public MVYGPUImageI420DataOutput(MVYGPUImageEGLContext context) {
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

                if (outputFramebuffer != null) {
                    if (outputLineSize != outputFramebuffer.width || outputHeight != outputFramebuffer.height) {
                        outputFramebuffer.destroy();
                        outputFramebuffer = null;
                    }
                }

                if (outputFramebuffer == null) {
                    outputFramebuffer = new MVYGPUImageFramebuffer(outputLineSize, outputHeight);
                }

                outputFramebuffer.activateFramebuffer();

                glClearColor(0, 0, 0, 0);
                glClear(GL_COLOR_BUFFER_BIT);

                glActiveTexture(GL_TEXTURE2);
                glBindTexture(GL_TEXTURE_2D, firstInputFramebuffer.texture[0]);

                glUniform1i(filterInputTextureUniform, 2);

                glEnableVertexAttribArray(filterPositionAttribute);
                glEnableVertexAttribArray(filterTextureCoordinateAttribute);

                float[] textureCoordinates = MVYGPUImageConstants.textureCoordinatesForRotation(rotateMode);

                // 处理lineSize != width
                for (int x = 0; x < textureCoordinates.length; x = x + 2) {
                    if (textureCoordinates[x] == 1) {
                        textureCoordinates[x] = (float)outputLineSize / (float)outputWidth;
                    }
                }

                glVertexAttribPointer(filterPositionAttribute, 2, GL_FLOAT, false, 0, vertices);
                glVertexAttribPointer(filterTextureCoordinateAttribute, 2, GL_FLOAT, false, 0, MVYGPUImageConstants.floatArrayToBuffer(textureCoordinates));

                glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

                // 利用libyuv 进行格式转换
                bgraBuffer.clear();
                yuvBuffer.clear();
                glFinish();
                glReadPixels(0, 0, outputLineSize, outputHeight, GL_RGBA, GL_UNSIGNED_BYTE, bgraBuffer);

                AYYuvUtil.RGBA_To_I420(bgraBuffer, yuvBuffer, outputLineSize, outputHeight);

                yuvBuffer.rewind();
                yuvBuffer.get(outputYUVData);

                //TODO TEST: 保存最终的RGBA数据到 SDCard/test.png
//                try {
//                    Bitmap bitmap = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888);
//                    bitmap.copyPixelsFromBuffer(bgraBuffer);
//                    FileOutputStream fileOutputStream = new FileOutputStream(new File(Environment.getExternalStorageDirectory() + "/test.png"));
//                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
//                    fileOutputStream.flush();
//                    fileOutputStream.close();
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }

                glDisableVertexAttribArray(filterPositionAttribute);
                glDisableVertexAttribArray(filterTextureCoordinateAttribute);
            }
        });
    }

    public void setOutputWithYUVData(byte[] yuvData, final int width, final int height, final int lineSize) {
        this.outputYUVData = yuvData;
        this.outputWidth = width;
        this.outputHeight = height;
        this.outputLineSize = lineSize;

        this.bgraBuffer = ByteBuffer.allocateDirect(lineSize * height * 4);
        this.yuvBuffer = ByteBuffer.allocateDirect(lineSize * height * 3 / 2);
    }

    public void setRotateMode(MVYGPUImageConstants.AYGPUImageRotationMode rotateMode) {
        this.rotateMode = rotateMode;
    }

    public void destroy() {
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
