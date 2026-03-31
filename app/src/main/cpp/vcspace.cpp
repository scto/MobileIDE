#include "gemini.h"
#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_mobileide_app_core_Secrets_getGenerativeAiApiKey(JNIEnv* env, jclass clazz) {
    std::string key = MobileIDE::getGeminiKey();
    return env->NewStringUTF(key.c_str());
}
