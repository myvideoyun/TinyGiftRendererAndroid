package com.myvideoyun.giftrenderer.gpuImage;

public interface MVYGPUImageInput {
    void setInputSize(int width, int height);
    void setInputFramebuffer(MVYGPUImageFramebuffer newInputFramebuffer);
    void newFrameReady();
}
