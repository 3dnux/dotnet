#include <jni.h>
#include <android/native_window_jni.h>
#include <android/log.h>
#include <atomic>

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

JNIEXPORT void JNICALL Java_dev_x360_emu_NativeBridge_setSurface(JNIEnv* env, jobject, jobject surface) {
    if (g_window) { ANativeWindow_release(g_window); g_window = nullptr; }
    if (surface) g_window = ANativeWindow_fromSurface(env, surface);
}

JNIEXPORT void JNICALL Java_dev_x360_emu_NativeBridge_start(JNIEnv*, jobject) { g_running = true; }
JNIEXPORT void JNICALL Java_dev_x360_emu_NativeBridge_stop(JNIEnv*, jobject) { g_running = false; }

JNIEXPORT void JNICALL Java_dev_x360_emu_NativeBridge_setInput(
    JNIEnv*, jobject, jint, jfloat, jfloat, jfloat, jfloat, jfloat, jfloat) {}

}
