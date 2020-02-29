package com.myvideoyun.TinyGiftRenderer.png;

import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.VideoView;

import com.myvideoyun.TinyGiftRenderer.R;
import com.myvideoyun.TinyGiftRenderer.png.tool.PngSurfaceView;

public class PngRenderActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_png_render);

        startVideoPlay();
        startPngRender();
    }

    private void startVideoPlay() {
        VideoView videoView = findViewById(R.id.mVideoView);
        videoView.setOnPreparedListener(mp -> mp.setLooping(true));
        String fileName = "android.resource://" + getPackageName() + "/raw/zhubo";
        videoView.setVideoURI(Uri.parse(fileName));
        videoView.start();
    }

    private void startPngRender() {
        String[] fileNames = new String[40];
        for (int i = 0; i < fileNames.length; i++) {
            fileNames[i] = String.format("pngframes/huacao/png/bg/bg_%02d.png", i + 1);
        }
        PngSurfaceView mSurFrame = findViewById(R.id.suv_frame_0);
        mSurFrame.startAnim(fileNames);

        String[] fileNames1 = new String[40];
        for (int i = 0; i < fileNames1.length; i++) {
            fileNames1[i] = String.format("pngframes/huacao/png/bot/bot_%02d.png", i + 1);
        }
        PngSurfaceView mSurFrame1 = findViewById(R.id.suv_frame_1);
        mSurFrame1.startAnim(fileNames1);

        String[] fileNames2 = new String[40];
        for (int i = 0; i < fileNames2.length; i++) {
            fileNames2[i] = String.format("pngframes/huacao/png/top/top_%02d.png", i + 1);
        }
        PngSurfaceView mSurFrame2 = findViewById(R.id.suv_frame_2);
        mSurFrame2.startAnim(fileNames2);
    }
}