/*
 * Copyright 2019 myvideoyun Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <assert.h>
#include <algorithm>
#include <functional>
#include <vector>
#include <string>
#include "android/log.h"
#include "jni.h"
#include <android/asset_manager_jni.h>

//#include "Observer.h"
#include "render_api.h"

#define GIFTRENDERER_JAVA "com/myvideoyun/TinyGiftRenderer/TinyGiftRenderer"

#ifdef __cplusplus
extern "C" {
#endif

static JavaVM *jvm;
static Observer AuthEventObserver;
static jobject javaListener;
static jmethodID eventMethodId;
static bool isJavaListenerChanged = false;

#define  LOG_TAG "tgr"
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

static AAssetManager *getAssetsMgr(JNIEnv *env, jobject obj) {
    jmethodID getAssetsMethod = env->GetMethodID(env->GetObjectClass(obj), "getAssets",
                                                 "()Landroid/content/res/AssetManager;");
    jobject assets = env->CallObjectMethod(obj, getAssetsMethod);

    return AAssetManager_fromJava(env, assets);
}

jlong CreateRender(JNIEnv *env, jclass clazz, jobject obj, jint type, jint vertFlip) {
    LOGI("create gift : %d vertFlip: %d", type, vertFlip);
    void *renderer = renderer_create(vertFlip);
    renderer_setParam(renderer, "AssetManager", getAssetsMgr(env, obj));
    return (jlong) renderer;
}

jint SetGiftPath(JNIEnv *env, jclass clazz, jlong id, jstring effect) {
    if (effect != NULL) {
        const char *e = env->GetStringUTFChars(effect, JNI_FALSE);
        int ret = renderer_setParam((void*)id, "StickerType", (void*)e);
        LOGI("setGiftPath: %s", e);
        env->ReleaseStringUTFChars(effect, e);
        return ret;
    } else {
        renderer_setParam((void*)id, "StickerType", nullptr);
        LOGI("setGiftPath: null");
    }

    return 0;
}

jint SetRunningFlag(JNIEnv *env, jclass clazz, jlong id, jint type) {
    return renderer_setParam((void*)id, "Pause", &type);
}

void SetRenderEventListener(JNIEnv *env, jclass clazz, jlong id, jobject listener) {
    jobject javaListener = env->NewGlobalRef(listener);
    void (*message)(int, int, const char *, jobject)=[](int type, int ret, const char *info,
                                                        jobject jal) {
        static jmethodID eventMethodId;
        static bool isChanged = true;
        if (jal != nullptr && ret != ObserverMsg::MSG_STAT_EFFECTS_PLAY &&
            ret != ObserverMsg::MSG_STAT_EFFECTS_PAUSE) {
            JNIEnv *envTemp;
            jvm->AttachCurrentThread(&envTemp, NULL);
            if (isChanged && envTemp) {
                jclass clazzTemp = envTemp->GetObjectClass(jal);
                eventMethodId = envTemp->GetMethodID(clazzTemp, "onEvent",
                                                     "(IILjava/lang/String;)I");
                isChanged = false;
            }
            if (eventMethodId != nullptr) {
                envTemp->CallIntMethod(jal, eventMethodId, type, ret, envTemp->NewStringUTF(info));
            }
        }
        if (type == ObserverMsg::MSG_STAT_LOOP_EXIT) {
            if (jal != nullptr) {
                jvm->DetachCurrentThread();
            }
        }
    };
    auto callback = std::bind(message, std::placeholders::_1, std::placeholders::_2,
                              std::placeholders::_3, javaListener);
    MsgCallback cb;
    cb.callback = callback;
    renderer_setParam((void*)id, "MsgFunction", (void*)&cb);
}

jint RenderGift(JNIEnv *env, jclass clazz, jlong id, jint textureId, jint width, jint height) {
    return renderer_render((void*)id, textureId, width, height, nullptr);
}

jint SetRenderParams(JNIEnv *env, jclass clazz, jlong id, jstring key, jlong face) {
    const char *cKey = env->GetStringUTFChars(key, JNI_FALSE);
    int ret = renderer_setParam((void*)id, cKey, (void *) &face);
    env->ReleaseStringUTFChars(key, cKey);
    return ret;
}

jint SetModelView(JNIEnv *env, jclass clazz, jlong id, jfloatArray modelView) {
    jsize size = env->GetArrayLength(modelView);
    if (size < 16)
        return -1;

    jfloat * mv = env->GetFloatArrayElements(modelView, 0);
    if (mv == NULL)
        return -2;

    int ret = renderer_setParam((void*)id, "ModelView", (void *)mv);
    env->ReleaseFloatArrayElements(modelView, mv, 0);

    return ret;
}

jint ReleaseGLResources(JNIEnv *env, jclass clazz, jlong id) {
    if(id > 0)
        renderer_releaseResources((void*)id);
    return 0;
}

jint ReleaseRender(JNIEnv *env, jclass clazz, jlong id) {
    renderer_destropy((void*) id);
    return 0;
}

void observerMessage(int type, int ret, const char *info) {
    LOGI("Authenticate myvideoyun : %d %d %s", type, ret, info);
    if (javaListener != nullptr) {
        JNIEnv *env;
        jvm->AttachCurrentThread(&env, NULL);
        if (isJavaListenerChanged && env) {
            jclass clazz = env->GetObjectClass(javaListener);
            eventMethodId = env->GetMethodID(clazz, "onEvent", "(IILjava/lang/String;)I");
            isJavaListenerChanged = false;
        }
        jint method = env->CallIntMethod(javaListener, eventMethodId, type, ret,
                                         env->NewStringUTF(info));
        if (method == -1) {
            if (type == ObserverMsg::MSG_STAT_LOOP_EXIT) {
                if (javaListener != nullptr) {
                    jvm->DetachCurrentThread();
                }
            }
            return;
        }
    }

    if (type == ObserverMsg::MSG_STAT_LOOP_EXIT) {
        if (javaListener != nullptr) {
            jvm->DetachCurrentThread();
        }
    }
}

jint Auth(JNIEnv *env, jclass clazz, jobject context, jstring authKey, int length) {
    AuthEventObserver.message = observerMessage;
    AuthEventObserver.message(ObserverMsg::MSG_TYPE_INIT, 0, "authentication callback");

    char *_appKey = (char *) env->GetStringUTFChars(authKey, JNI_FALSE);
    renderer_auth(env, context, _appKey, length, &AuthEventObserver);

    return 0;
}

void SetAuthEventListener(JNIEnv *env, jclass clazz, jobject listener) {
    javaListener = env->NewGlobalRef(listener);
    isJavaListenerChanged = true;
}

static JNINativeMethod g_methods[] = {
        {"_createGiftRenderer",     "(Landroid/content/Context;II)J",                         (void *) CreateRender},
        {"_setGiftPath",            "(JLjava/lang/String;)I",                                 (void *) SetGiftPath},
        {"_setRenderEventListener", "(JLcom/myvideoyun/TinyGiftRenderer/render/IEventListener;)V", (void *) SetRenderEventListener},
        {"_renderGift",             "(JIII)I",                                                (void *) RenderGift},
        {"_release",                "(J)I",                                                   (void *) ReleaseRender},
        {"_releaseGLResources",     "(J)I",                                                   (void *) ReleaseGLResources},
        {"_pause",                  "(JI)I",                                                  (void *) SetRunningFlag},
        {"_setRenderParams",        "(JLjava/lang/String;J)I",                                (void *) SetRenderParams},
        {"_auth",                   "(Landroid/content/Context;Ljava/lang/String;I)I",         (void *) Auth},
        {"_setAuthEventListener",   "(Lcom/myvideoyun/TinyGiftRenderer/render/IEventListener;)V",  (void *) SetAuthEventListener},
        {"_setModelView",               "(J[F)I",                                                (void *) SetModelView},
};

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    jvm = vm;
    JNIEnv *env = nullptr;

    if (vm->GetEnv((void **) &env, JNI_VERSION_1_4) != JNI_OK) {
        return JNI_ERR;
    }
    assert(env != nullptr);
    jclass clazz = env->FindClass(GIFTRENDERER_JAVA);
    env->RegisterNatives(clazz, g_methods, (int) (sizeof(g_methods) / sizeof((g_methods)[0])));
    return JNI_VERSION_1_4;
}

JNIEXPORT void JNI_OnUnload(JavaVM *jvm, void *reserved) {
}

#ifdef __cplusplus
}
#endif
