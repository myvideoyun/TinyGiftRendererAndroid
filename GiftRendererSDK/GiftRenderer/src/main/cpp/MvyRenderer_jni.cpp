#include <jni.h>
#include <string>
#include <android/log.h>
#include <sstream>
#include "render_api.h"
#include <android/asset_manager_jni.h>

static JavaVM *ay_effectJvm = NULL;

class GiftRenderer
{
public:
    void *render;
    jobject callback;

    void effectMessage(int type, int ret, const char *info){
        JNIEnv *env;

        ay_effectJvm->AttachCurrentThread(&env, NULL);

        if (callback == NULL) {
            ay_effectJvm->DetachCurrentThread();
            return;
        }

        jclass clazz = env->GetObjectClass(callback);
        if (clazz == NULL) {
            ay_effectJvm->DetachCurrentThread();
            return;
        }

        jmethodID methodID = env->GetMethodID(clazz, "aiyaEffectMessage", "(II)V");
        if (methodID == NULL) {
            ay_effectJvm->DetachCurrentThread();
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
#if 0
    env->GetJavaVM(&ay_effectJvm);

    GiftRenderer *renderer = reinterpret_cast<GiftRenderer *>(render_);
    if (renderer) {
        renderer->render->message = std::bind(&GiftRenderer::effectMessage, renderer, std::placeholders::_1, std::placeholders::_2, std::placeholders::_3);
        renderer->callback = env->NewGlobalRef(callback_);
    }
#endif
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
#if 0
    GiftRenderer *renderer = reinterpret_cast<GiftRenderer *>(render_);
    if (renderer) {

        FaceData **faceData = reinterpret_cast<FaceData **>(value_);

        if (faceData && *faceData) {
            renderer->render->setParam("FaceData", *faceData);
        } else {
            renderer->render->setParam("FaceData", NULL);
        }
    }
#endif
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
Java_com_myvideoyun_giftrenderer_MvyRenderer_Draw(JNIEnv *env, jobject instance, jlong render_, jint texture, jint width, jint height) {

    GiftRenderer *renderer = reinterpret_cast<GiftRenderer *>(render_);
    if (renderer) {
        //renderer->render->draw(texture, width, height, NULL);
        renderer_render((void*)renderer->render, texture, width, height, nullptr);
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_myvideoyun_giftrenderer_MvyRenderer_InitLicense(JNIEnv *env, jclass instance, jobject context,
                                         jstring appKey_, jint keyLength) {

    const char *appKey = env->GetStringUTFChars(appKey_, 0);
    env->GetJavaVM(&ay_effectJvm);


    //AyCore_Auth2(env, context, appKey);
    renderer_auth(env, context, appKey, keyLength, NULL);

    env->ReleaseStringUTFChars(appKey_, appKey);
}