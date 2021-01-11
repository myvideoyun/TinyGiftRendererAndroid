#include <jni.h>
#include <string>
#include <android/log.h>
#include <sstream>
#include "render_api.h"
#include <android/asset_manager_jni.h>

static JavaVM *pJavaVM = NULL;

class GiftRenderer
{
public:
    void *render;
    jobject callback;

    void effectMessage(int type, int ret, const char *info){
        JNIEnv *env;

        pJavaVM->AttachCurrentThread(&env, NULL);

        if (callback == NULL) {
            pJavaVM->DetachCurrentThread();
            return;
        }

        jclass clazz = env->GetObjectClass(callback);
        if (clazz == NULL) {
            pJavaVM->DetachCurrentThread();
            return;
        }

        jmethodID methodID = env->GetMethodID(clazz, "MVYRenderMsg", "(II)V");
        if (methodID == NULL) {
            pJavaVM->DetachCurrentThread();
            return;
        }

        env->CallVoidMethod(callback, methodID, type, ret);
    }
};

static AAssetManager *getAssetsMgr(JNIEnv *env, jobject obj) {
    jmethodID getAssetsMethod = env->GetMethodID(env->GetObjectClass(obj), "getAssets",
                                                 "()Landroid/content/res/AssetManager;");
    jobject assets = env->CallObjectMethod(obj, getAssetsMethod);

    return AAssetManager_fromJava(env, assets);
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_myvideoyun_giftrenderer_MvyRenderer_Create(JNIEnv *env, jobject instance) {
    void *renderer = renderer_create(1);
    //renderer_setParam(renderer, "AssetManager", getAssetsMgr(env, instance));

    // create renderer
    GiftRenderer *renderer_wrapper = new GiftRenderer();
    renderer_wrapper->render = renderer;

    return reinterpret_cast<jlong>(renderer_wrapper);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_myvideoyun_giftrenderer_MvyRenderer_Callback(JNIEnv *env, jobject instance, jlong render_, jobject callback_) {
    env->GetJavaVM(&pJavaVM);

    GiftRenderer *renderer = reinterpret_cast<GiftRenderer *>(render_);
    if (renderer) {
        MsgCallback cb;
        cb.callback = std::bind(&GiftRenderer::effectMessage, renderer, std::placeholders::_1, std::placeholders::_2, std::placeholders::_3);
        renderer->callback = env->NewGlobalRef(callback_);
        renderer_setParam(renderer->render, "MsgFunction", (void*)&cb);
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_myvideoyun_giftrenderer_MvyRenderer_Destroy(JNIEnv *env, jobject instance, jlong render_) {
    GiftRenderer *renderer = reinterpret_cast<GiftRenderer *>(render_);
    if (renderer) {
        renderer_releaseResources((void*)renderer->render);
        renderer_destropy((void*) renderer->render);
        env->DeleteGlobalRef(renderer->callback);
        delete(renderer);
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_myvideoyun_giftrenderer_MvyRenderer_SetFaceData(JNIEnv *env, jobject instance, jlong render_, jlong value_) {
}

extern "C"
JNIEXPORT void JNICALL
Java_com_myvideoyun_giftrenderer_MvyRenderer_SetStickerPath(JNIEnv *env, jobject instance, jlong render_, jstring path_) {
    const char * path = env->GetStringUTFChars(path_,JNI_FALSE);

    GiftRenderer *renderer = reinterpret_cast<GiftRenderer *>(render_);
    if (renderer) {
        int ret = renderer_setParam((void*)renderer->render, "StickerType", (void*)path);
    }
    env->ReleaseStringUTFChars(path_, path);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_myvideoyun_giftrenderer_MvyRenderer_SetEnableVFlip(JNIEnv *env, jobject instance, jlong render_, jboolean enable) {
#if 0
    GiftRenderer *renderer = reinterpret_cast<GiftRenderer *>(render_);
    if (renderer) {
        renderer->render->setParam("EnableVFlip",&enable);
    }
#endif
}

extern "C"
JNIEXPORT void JNICALL
Java_com_myvideoyun_giftrenderer_MvyRenderer_SetPause(JNIEnv *env, jobject instance, jlong render_) {
    GiftRenderer *renderer = reinterpret_cast<GiftRenderer *>(render_);
    if (renderer) {
        int defaultValue = 1;
        renderer_setParam(renderer->render, "Pause", &defaultValue);
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_myvideoyun_giftrenderer_MvyRenderer_SetResume(JNIEnv *env, jobject instance, jlong render_) {
    GiftRenderer *renderer = reinterpret_cast<GiftRenderer *>(render_);
    if (renderer) {
        int defaultValue = 0;
        renderer_setParam(renderer->render, "Pause", &defaultValue);
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_myvideoyun_giftrenderer_MvyRenderer_SetMVPMatrix(JNIEnv *env, jobject instance, jlong render_, jfloatArray mvp) {
    GiftRenderer *renderer = reinterpret_cast<GiftRenderer *>(render_);
    jsize len = env->GetArrayLength(mvp);
    jfloat *body = env->GetFloatArrayElements(mvp, 0);
    if(len != 16){
        // Show some warnings
        return;
    }

    float mvp_matrix[16];
    for(int i = 0; i < 16; ++i)
        mvp_matrix[i] = body[i];

    if (renderer) {
        renderer_setParam(renderer->render, "ModelView", (void*)mvp_matrix);
    }
    env->ReleaseFloatArrayElements(mvp, body, 0);
    return;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_myvideoyun_giftrenderer_MvyRenderer_Draw(JNIEnv *env, jobject instance, jlong render_, jint texture, jint width, jint height) {

    GiftRenderer *renderer = reinterpret_cast<GiftRenderer *>(render_);
    if (renderer) {
        renderer_render((void*)renderer->render, texture, width, height, nullptr);
    }
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_myvideoyun_giftrenderer_MvyRenderer_InitLicense(JNIEnv *env, jclass instance, jobject context,
                                         jstring appKey_, jint keyLength) {

    const char *appKey = env->GetStringUTFChars(appKey_, 0);

    int auth_ret = renderer_auth(env, context, appKey, keyLength);

    env->ReleaseStringUTFChars(appKey_, appKey);
	return auth_ret;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_myvideoyun_giftrenderer_MvyRenderer_InitLicenseEx(JNIEnv *env, jclass instance, jobject context,
                                                         jstring license_, jint licenseLength,
                                                         jstring appKey_, jint keyLength) {

    const char *appKey = env->GetStringUTFChars(appKey_, 0);
    const char *licenseStr = env->GetStringUTFChars(license_, 0);
    int auth_ret = renderer_auth_ex(env, context, licenseStr, licenseLength, appKey, keyLength);

    env->ReleaseStringUTFChars(appKey_, appKey);
    env->ReleaseStringUTFChars(license_, licenseStr);
    return auth_ret;
}