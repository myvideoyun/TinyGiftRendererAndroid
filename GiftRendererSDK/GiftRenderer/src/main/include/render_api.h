#ifndef _TINY_GIFT_RENDERER_API_H
#define _TINY_GIFT_RENDERER_API_H

#include <functional>
typedef struct {
    std::function<void(int type, int ret, const char *info)> callback;
} MsgCallback;

struct ObserverMsg {
    static const int MSG_TYPE_INIT = 0x0001;
    static const int MSG_TYPE_AUTH = 0x0002;
    static const int MSG_TYPE_TRACK = 0x1000;
    static const int MSG_TYPE_RENDER = 0x2000;
    static const int MSG_TYPE_BEAUTY = 0x4000;
    static const int MSG_TYPE_SHORT_VIDEO = 0x8000;

    static const int MSG_STAT_LOOP_EXIT = 0x0101;
    static const int MSG_STAT_MODEL_EXIT = 0x0102;

    static const int MSG_STAT_EFFECTS_INIT = 0x00010000;
    static const int MSG_STAT_EFFECTS_PLAY = 0x00020000;
    static const int MSG_STAT_EFFECTS_END = 0x00040000;
    static const int MSG_STAT_EFFECTS_PAUSE = 0x00100000;

    static const int MSG_ERR_FUNC_FORBIDDEN = 0xFE000010;
};

#ifdef __cplusplus
extern "C" {
#endif

#if defined(ANDROID)
#define EXPORT __attribute((visibility("default")))
#include <jni.h>
// 0: ok; -1: fail;
int renderer_auth(JNIEnv *env, jobject obj, std::string appKey, int length);
#else
#define EXPORT
// 0: ok; -1: fail;
int renderer_auth(std::string appId, std::string appKey, int length);
#endif

EXPORT void *renderer_create(int vertical_flip);
EXPORT void renderer_destropy(void *renderer);
EXPORT int renderer_setParam(void *renderer, const char *param_name,
                             void *param_value);
EXPORT int renderer_setFloatParam(void *renderer, const char *param_name,
                                  float param_value);
EXPORT int renderer_render(void *renderer, int texId, int width, int height,
                    const char *rgba_buf);
EXPORT int renderer_releaseResources(void *renderer_handler);

#ifdef __cplusplus
}
#endif
#endif
