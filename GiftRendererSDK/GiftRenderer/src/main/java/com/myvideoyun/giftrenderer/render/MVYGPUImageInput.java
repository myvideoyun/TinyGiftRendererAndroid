package com.myvideoyun.giftrenderer.render;

public interface MVYGPUImageInput {
    void setInputSize(int width, int height);
    void setInputFramebuffer(MVYGPUImageFramebuffer newInputFramebuffer);
    void newFrameReady();
}
