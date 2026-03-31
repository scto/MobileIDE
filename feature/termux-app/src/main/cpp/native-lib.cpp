#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_mobileide_feature_termux_app_TerminalActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */
) {
    std::string hello = "Hello from libtermux placeholder";
    return env->NewStringUTF(hello.c_str());
}
