package com.myvideoyun.giftrenderer.cameraTool;

public interface MVYCameraPreviewListener {
    void cameraCrateGLEnvironment();
    void cameraVideoOutput(int texture, int width, int height, long timeStamp);
    void cameraDestroyGLEnvironment();
}
