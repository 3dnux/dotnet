#include <jni.h>
#include <android/native_window_jni.h>
#include <android/log.h>
#include <atomic>
#include "xex_info.h"

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "x360", __VA_ARGS__)

// Stub: aquí se conectará el núcleo del emulador. Por ahora solo guarda el estado.
static ANativeWindow* g_window = nullptr;
static std::atomic<bool> g_running{false};

extern "C" {

JNIEXPORT jstring JNICALL Java_dev_x360_emu_NativeBridge_coreVersion(JNIEnv* env, jobject) {
    return env->NewStringUTF("stub 0.1 (sin núcleo)");
}

JNIEXPORT jboolean JNICALL Java_dev_x360_emu_NativeBridge_loadTitle(JNIEnv*, jobject, jint fd) {
    LOGI("loadTitle fd=%d (no implementado)", fd);
    return JNI_FALSE;  // hasta integrar el cargador XEX/ISO
}

// Devuelve [titulo UTF-8, icono PNG, ID de titulo (4 bytes BE)] o null si no es un XEX valido.
JNIEXPORT jobjectArray JNICALL Java_dev_x360_emu_NativeBridge_readXexMetaNative(JNIEnv* env, jobject, jint fd) {
    XexMeta meta;
    if (!ReadXexMeta(fd, &meta)) return nullptr;
    jclass byteArray = env->FindClass("[B");
    jobjectArray result = env->NewObjectArray(3, byteArray, nullptr);
    auto bytes = [&](const uint8_t* p, size_t n) {
        jbyteArray a = env->NewByteArray(static_cast<jsize>(n));
        if (n) env->SetByteArrayRegion(a, 0, static_cast<jsize>(n), reinterpret_cast<const jbyte*>(p));
        return a;
    };
    uint8_t id[4] = {uint8_t(meta.title_id >> 24), uint8_t(meta.title_id >> 16), uint8_t(meta.title_id >> 8), uint8_t(meta.title_id)};
    env->SetObjectArrayElement(result, 0, bytes(reinterpret_cast<const uint8_t*>(meta.title.data()), meta.title.size()));
    env->SetObjectArrayElement(result, 1, bytes(meta.icon.data(), meta.icon.size()));
    env->SetObjectArrayElement(result, 2, bytes(id, 4));
    return result;
}

JNIEXPORT void JNICALL Java_dev_x360_emu_NativeBridge_setSurface(JNIEnv* env, jobject, jobject surface) {
    if (g_window) { ANativeWindow_release(g_window); g_window = nullptr; }
    if (surface) g_window = ANativeWindow_fromSurface(env, surface);
}

JNIEXPORT void JNICALL Java_dev_x360_emu_NativeBridge_start(JNIEnv*, jobject) { g_running = true; }
JNIEXPORT void JNICALL Java_dev_x360_emu_NativeBridge_stop(JNIEnv*, jobject) { g_running = false; }

JNIEXPORT void JNICALL Java_dev_x360_emu_NativeBridge_setInput(
    JNIEnv*, jobject, jint, jfloat, jfloat, jfloat, jfloat, jfloat, jfloat) {}

}
