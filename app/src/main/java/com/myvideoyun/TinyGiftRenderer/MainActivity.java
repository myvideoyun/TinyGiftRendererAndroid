package com.myvideoyun.TinyGiftRenderer;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.myvideoyun.TinyGiftRenderer.camera.VideoAndTextureActivity;
import com.myvideoyun.TinyGiftRenderer.png.PngRenderActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.fast_render_btn).setOnClickListener(v -> {
            Intent intent = new Intent(getBaseContext(), VideoAndTextureActivity.class);
            startActivity(intent);
        });


        findViewById(R.id.png_render_btn).setOnClickListener(v -> {
            Intent intent = new Intent(getBaseContext(), PngRenderActivity.class);
            startActivity(intent);
        });
    }
}
