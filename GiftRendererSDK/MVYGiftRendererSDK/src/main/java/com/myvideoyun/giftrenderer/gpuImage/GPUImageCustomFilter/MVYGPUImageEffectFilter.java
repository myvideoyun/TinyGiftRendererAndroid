package com.myvideoyun.giftrenderer.gpuImage.GPUImageCustomFilter;

import com.myvideoyun.giftrenderer.MvyRenderer;
import com.myvideoyun.giftrenderer.gpuImage.MVYGPUImageEGLContext;
import com.myvideoyun.giftrenderer.gpuImage.MVYGPUImageFilter;
import com.myvideoyun.giftrenderer.gpuImage.MVYGPUImageFramebuffer;

import java.nio.Buffer;

import static android.opengl.GLES20.GL_BLEND;
import static android.opengl.GLES20.GL_DEPTH_ATTACHMENT;
import static android.opengl.GLES20.GL_DEPTH_COMPONENT16;
import static android.opengl.GLES20.GL_DEPTH_TEST;
import static android.opengl.GLES20.GL_FRAMEBUFFER;
import static android.opengl.GLES20.GL_RENDERBUFFER;
import static android.opengl.GLES20.glBindRenderbuffer;
import static android.opengl.GLES20.glDeleteRenderbuffers;
import static android.opengl.GLES20.glDisable;
import static android.opengl.GLES20.glEnable;
import static android.opengl.GLES20.glFramebufferRenderbuffer;
import static android.opengl.GLES20.glGenRenderbuffers;
import static android.opengl.GLES20.glRenderbufferStorage;
import static com.myvideoyun.giftrenderer.MvyRenderer.MSG_STAT_EFFECTS_END;

public class MVYGPUImageEffectFilter extends MVYGPUImageFilter implements MvyRenderer.OnEffectCallback {
    private MvyRenderer effect;

    private String effectPath;
    private int effectPlayCount;
    private int currentPlayCount;
    private long faceData;

    private int[] depthRenderbuffer = new int[]{0};

    private MVYGPUImageEffectPlayFinishListener effectPlayFinishListener;

    public MVYGPUImageEffectFilter(MVYGPUImageEGLContext context) {
        super(context);

        context.syncRunOnRenderThread(new Runnable() {
            @Override
            public void run() {
                createRBO();

                effect = new MvyRenderer();
                effect.initGLResource();
                effect.setCallback(MVYGPUImageEffectFilter.this);
            }
        });
    }

    @Override
    protected void renderToTexture(Buffer vertices, Buffer textureCoordinates) {
        context.syncRunOnRenderThread(new Runnable() {
            @Override
            public void run() {
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

                //------------->绘制图像<--------------//
                // 打开深度Buffer 打开深度测试
                glBindRenderbuffer(GL_RENDERBUFFER, depthRenderbuffer[0]);
                glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT16, outputFramebuffer.width, outputFramebuffer.height);
                glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depthRenderbuffer[0]);
                glBindRenderbuffer(GL_RENDERBUFFER, 0);
                glEnable(GL_DEPTH_TEST);
                glEnable(GL_BLEND);

                effect.setFaceData(faceData);
                effect.processWithTexture(firstInputFramebuffer.texture[0], outputWidth(), outputHeight());

                // 关闭深度Buffer 关闭深度测试
                glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, 0);
                glDisable(GL_DEPTH_TEST);
                glDisable(GL_BLEND);
                //------------->绘制图像<--------------//
            }
        });
    }

    public void setEffectPath(String effectPath) {
        this.effectPath = effectPath;
        effect.setEffectPath(effectPath);
    }

    public void setEffectPlayCount(int effectPlayCount) {
        this.effectPlayCount = effectPlayCount;
        currentPlayCount = 0;
    }

    public void setEffectPlayFinishListener(MVYGPUImageEffectPlayFinishListener effectPlayFinishListener) {
        this.effectPlayFinishListener = effectPlayFinishListener;
    }
	
    public void setMVPMatrix(float[] mvp){
        effect.SetMVPMatrix(mvp);
    }

    public void setFaceData(long faceData) {
        this.faceData = faceData;
    }

    public void pause() {
        effect.pauseProcess();
    }

    public void resume() {
        effect.resumeProcess();
    }

    @Override
    public void destroy() {
        super.destroy();

        context.syncRunOnRenderThread(new Runnable() {
            @Override
            public void run() {
                effect.releaseGLResource();
                destroyRBO();
            }
        });
    }

    private void createRBO() {
        glGenRenderbuffers(1, depthRenderbuffer, 0);
    }

    private void destroyRBO() {
        if (depthRenderbuffer[0] != 0) {
            glDeleteRenderbuffers(1, depthRenderbuffer, 0);
            depthRenderbuffer[0] = 0;
        }
    }

    @Override
    public void MVYRenderMsg(int type, int ret) {

        if (effectPath == null || effectPath.equals("")) {
            // 路径错误
        } else if (ret == MSG_STAT_EFFECTS_END || ret < 0) { //已经渲染完成一遍, 或者发生错误
            currentPlayCount++;
            if (ret < 0 || (effectPlayCount != 0 && currentPlayCount >= effectPlayCount)) {
                setEffectPath("");

                if (effectPlayFinishListener != null) {
                    effectPlayFinishListener.playFinish();
                }
            }

        } else if (effectPlayCount != 0 && currentPlayCount >= effectPlayCount) { //已经播放完成
            setEffectPath("");
        }
    }
}