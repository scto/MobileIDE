#include <jni.h>
#include <stdint.h>
#include "tree_sitter/parser.h"

extern const TSLanguage *tree_sitter_css(void);

JNIEXPORT jlong JNICALL
Java_io_github_rosemoe_sora_editor_ts_CssLanguage_00024Companion_tree_1sitter_1css(JNIEnv *env, jclass clazz) {
    (void)env;
    (void)clazz;
    return (jlong)(intptr_t)tree_sitter_css();
}
