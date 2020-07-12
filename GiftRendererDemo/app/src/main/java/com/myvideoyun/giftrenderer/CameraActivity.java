package com.myvideoyun.giftrenderer;

import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.View;
import android.widget.SeekBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.myvideoyun.giftrenderer.adapter.Adapter;
import com.myvideoyun.giftrenderer.cameraTool.MVYCameraPreviewListener;
import com.myvideoyun.giftrenderer.cameraTool.MVYCameraPreviewWrap;
import com.myvideoyun.giftrenderer.cameraTool.MVYPreviewView;
import com.myvideoyun.giftrenderer.cameraTool.MVYPreviewViewListener;
import com.myvideoyun.giftrenderer.common.UI;
import com.myvideoyun.giftrenderer.render.MVYGPUImageConstants;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.myvideoyun.giftrenderer.render.MVYGPUImageConstants.AYGPUImageContentMode.kAYGPUImageScaleAspectFill;
import static com.myvideoyun.giftrenderer.render.MVYGPUImageConstants.AYGPUImageRotationMode.kAYGPUImageRotateRight;

public class CameraActivity extends AppCompatActivity implements MVYCameraPreviewListener, MVYPreviewViewListener, Adapter.OnItemClickListener, SeekBar.OnSeekBarChangeListener {

    Camera camera;
    MVYCameraPreviewWrap cameraPreviewWrap;
    MVYPreviewView surfaceView;

    MVYEffectHandler effectHandler;
    private Adapter adapter;
    private SeekBar seekBar;

    private int mode;
    private Object data;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        UI.setSystemStyle(this, UI.dark);

        setContentView(R.layout.activity_camera);


        surfaceView = findViewById(R.id.camera_preview);
        surfaceView.setListener(this);
        surfaceView.setContentMode(kAYGPUImageScaleAspectFill);

        initUI();

        try {
            initData();
        } catch (Exception e) {
            throw new IllegalStateException("读取资源文件发生错误");
        }
    }

    private void initUI() {
        RecyclerView recyclerView = findViewById(R.id.recycle_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getBaseContext(), LinearLayoutManager.HORIZONTAL, false);
        recyclerView.setLayoutManager(layoutManager);

        adapter = new Adapter();
        adapter.setOnItemClickListener(this);

        recyclerView.setAdapter(adapter);

        seekBar = findViewById(R.id.seek_bar);
        seekBar.setOnSeekBarChangeListener(this);

        findViewById(R.id.effect_btn).setOnClickListener(v -> {
            adapter.setMode(0);
            seekBar.setProgress(0);
            seekBar.setVisibility(View.INVISIBLE);
        });

        findViewById(R.id.beauty_btn).setOnClickListener(v -> {
            adapter.setMode(1);
            seekBar.setProgress(0);
            seekBar.setVisibility(View.VISIBLE);
        });

        findViewById(R.id.style_btn).setOnClickListener(v -> {
            adapter.setMode(2);
            seekBar.setProgress(0);
            seekBar.setVisibility(View.VISIBLE);
        });

        findViewById(R.id.douyin_btn).setOnClickListener(v -> {
            adapter.setMode(3);
            seekBar.setProgress(0);
            seekBar.setVisibility(View.INVISIBLE);
        });
    }

    private void initData() throws IOException {
        // 特效数据
        String effectDataRootPath = getCacheDir() + File.separator + "effect" + File.separator + "data";
        String effectIconRootPath = getCacheDir() + File.separator + "effect" + File.separator + "icon";

        // 获取特效文件名
        List<String> effectNameList = new ArrayList<>();
        File[] effectRes = new File(effectIconRootPath).listFiles(pathname -> !pathname.getName().startsWith("."));
        for (File file : effectRes) {
            effectNameList.add(file.getName().substring(0, file.getName().indexOf(".")));
        }
        Collections.sort(effectNameList);

        // 获取图标, 名称, 特效路径
        List<Object[]> effectDataList = new ArrayList<>();
        for (String effectName : effectNameList) {

            String effectPath = effectDataRootPath + File.separator + effectName + File.separator + "meta.json";

            // 解析json文件中的name
            StringBuilder stringBuilder = new StringBuilder();
            InputStream inputStream = new FileInputStream(effectPath);
            InputStreamReader isr = new InputStreamReader(inputStream);
            BufferedReader reader = new BufferedReader(isr);
            String jsonLine;
            while ((jsonLine = reader.readLine()) != null) {
                stringBuilder.append(jsonLine);
            }
            reader.close();
            isr.close();
            inputStream.close();

            JSONObject jsonObject = null;
            try {
                jsonObject = new JSONObject(stringBuilder.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }

            String iconPath = effectIconRootPath + File.separator + effectName + ".png";
            String name = jsonObject.optString("name");

            effectDataList.add(new Object[]{iconPath, name, effectPath});
        }
        adapter.refreshEffectData(effectDataList);

        // 美颜数据
        List<Object[]> beautyDataList = new ArrayList<>();
        beautyDataList.add(new Object[]{R.mipmap.anim_flower, "磨皮", "smooth"});
        beautyDataList.add(new Object[]{R.mipmap.anim_flower, "红润", "ruby"});
        beautyDataList.add(new Object[]{R.mipmap.anim_flower, "美白", "white"});
        beautyDataList.add(new Object[]{R.mipmap.anim_flower, "大眼", "bigEye"});
        beautyDataList.add(new Object[]{R.mipmap.anim_flower, "瘦脸", "slimFace"});
        adapter.refreshBeautifyData(beautyDataList);

        // 滤镜数据
        String styleDataRootPath = getCacheDir() + File.separator + "style" + File.separator + "data";
        String styleIconRootPath = getCacheDir() + File.separator + "style" + File.separator + "icon";

        List<String> styleNameList = new ArrayList<>();
        File[] styleRes = new File(styleDataRootPath).listFiles(pathname -> !pathname.getName().startsWith("."));
        for (File file : styleRes) {
            styleNameList.add(file.getName());
        }
        Collections.sort(styleNameList);

        // 获取图标, 名称, 滤镜路径
        List<Object[]> styleDataList = new ArrayList<>();
        for (String styleName : styleNameList) {
            String iconPath = styleIconRootPath + File.separator + styleName;
            String name = styleName.substring("00".length(), styleName.length() - ".png".length());
            String stylePath = styleDataRootPath + File.separator + styleName;
            styleDataList.add(new Object[]{iconPath, name, stylePath});
        }
        adapter.refreshStyleData(styleDataList);

        // 抖音数据
        List<Object[]> douyinDataList = new ArrayList<>();
        douyinDataList.add(new Object[]{R.mipmap.anim_flower, "抖动", ""});
        douyinDataList.add(new Object[]{R.mipmap.anim_flower, "九宫格", ""});
        douyinDataList.add(new Object[]{R.mipmap.anim_flower, "转场", ""});
        adapter.refreshDouyinData(douyinDataList);
    }

    // ---------- UI变化 ----------

    @Override
    public void onItemClick(int mode, Object data) {
        this.mode = mode;
        this.data = data;

        if (mode == 0) {
            // 设置特效
            effectHandler.setEffectPath(data.toString());
            effectHandler.setEffectPlayCount(0);

        } else if (mode == 1) {
            // 设置美颜

        } else if (mode == 2) {
            // 设置滤镜
            effectHandler.setStyle(BitmapFactory.decodeFile(data.toString()));

        } else if (mode == 3) {
            // 设置抖音

        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (mode == 1) {
            // 设置美颜强度
            if ("smooth".contentEquals(data.toString())) {
//                effectHandler.setIntensityOfSmooth(progress / 100.f);

            } else if ("ruby".contentEquals(data.toString())) {

            } else if ("white".contentEquals(data.toString())) {

            } else if ("bigEye".contentEquals(data.toString())) {

            } else if ("slimFace".contentEquals(data.toString())) {

            }
        } else if (mode == 2) {
            // 设置滤镜强度
            effectHandler.setIntensityOfStyle(progress / 100.f);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    // ---------- 渲染相关 ----------

    /**
     * 打开硬件设备
     */
    private void openHardware() {
        if (camera != null) {
            camera.release();
            camera = null;
        }
        camera = Camera.open(1);

        cameraPreviewWrap = new MVYCameraPreviewWrap(camera);
        cameraPreviewWrap.setPreviewListener(this);
        cameraPreviewWrap.setRotateMode(kAYGPUImageRotateRight);
        cameraPreviewWrap.startPreview(surfaceView.eglContext);
    }

    /**
     * 关闭硬件设备
     */
    private void closeHardware() {
        // 关闭相机
        if (camera != null) {
            cameraPreviewWrap.stopPreview();
            cameraPreviewWrap = null;
            camera.release();
            camera = null;
        }
    }

    @Override
    public void cameraCrateGLEnvironment() {

        effectHandler = new MVYEffectHandler(this);
        effectHandler.setRotateMode(MVYGPUImageConstants.AYGPUImageRotationMode.kAYGPUImageFlipVertical);
    }

    @Override
    public void cameraVideoOutput(int texture, int width, int height, long timeStamp) {
        if (effectHandler != null) {
            effectHandler.processWithTexture(texture, width, height);
        }

        // 渲染到surfaceView
        if (surfaceView != null) {
            surfaceView.render(texture, width, height);
        }
    }

    @Override
    public void cameraDestroyGLEnvironment() {
        if (effectHandler != null) {
            effectHandler.destroy();
            effectHandler = null;
        }
    }

    @Override
    public void createGLEnvironment() {
        openHardware();
    }

    @Override
    public void destroyGLEnvironment() {
        closeHardware();
    }
}
