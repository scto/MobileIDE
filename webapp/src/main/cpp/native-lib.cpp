#include <jni.h>
#include <string>
#include <vector>
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>
#include <android/log.h>
#include <random>
#include <ctime>
#include "vm_defs.h"

// -----------------------------------------------------------------------------
// Obfuscation Utilities
// -----------------------------------------------------------------------------

// Compile-time string encryption (XOR) to hide sensitive strings from strings command
template <int N>
struct ObfuscatedString {
    char data[N];
    char key;

    constexpr ObfuscatedString(const char (&str)[N], char k) : data{}, key(k) {
        for (int i = 0; i < N; ++i) {
            data[i] = str[i] ^ k;
        }
    }

    std::string decrypt() const {
        std::string s(N - 1, '\0'); // N includes null terminator
        for (int i = 0; i < N - 1; ++i) {
            s[i] = data[i] ^ key;
        }
        return s;
    }
};

// Macros to simplify usage
#define OBF_STR(s, k) (ObfuscatedString<sizeof(s)>(s, k).decrypt().c_str())
#define LOG_TAG_KEY 0x5A
#define LOG_TAG OBF_STR("WebIDECrypto", LOG_TAG_KEY)

#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

// -----------------------------------------------------------------------------
// Environment Binding (Signature/Package Check)
// -----------------------------------------------------------------------------

bool check_signature(JNIEnv* env, jobject context) {
    // Disabled for template usage to allow dynamic package names
    return true;
}

// -----------------------------------------------------------------------------
// Chaos VM Compiler (Encryption)
// -----------------------------------------------------------------------------

void emit_byte(std::vector<uint8_t>& out, uint8_t byte) {
    out.push_back(byte);
}

void emit_op(std::vector<uint8_t>& out, uint8_t op) {
    emit_byte(out, op);
}

void emit_imm(std::vector<uint8_t>& out, uint8_t imm) {
    emit_byte(out, imm);
}

std::vector<uint8_t> compile_vm(const std::vector<uint8_t>& plaintext) {
    std::vector<uint8_t> bytecode;
    bytecode.reserve(plaintext.size() * 2); // Heuristic

    // Simulation State
    uint8_t R[4] = {0, 0, 0, 0};

    // Seed randomness locally (simple LCG or similar if rand is not good enough, but rand is fine for demo)
    // We assume srand is called or we just use rand().
    // Note: In JNI, calling srand() might affect other parts if single threaded, but usually fine.
    
    // 1. Initialize Registers with Random Keys
    for (int i = 0; i < 4; i++) {
        R[i] = std::rand() % 256;
        emit_op(bytecode, VM::OP_MOV_IMM);
        emit_byte(bytecode, i); 
        emit_imm(bytecode, R[i]);
    }

    // 2. Process Data
    for (uint8_t p : plaintext) {
         // Obfuscation (Junk Code)
        if (std::rand() % 5 == 0) { // 20% chance
            int type = std::rand() % 3;
            int rx = std::rand() % 4;
            int ry = std::rand() % 4;
            
            if (type == 0) {
                emit_op(bytecode, VM::OP_ADD_REG);
                emit_byte(bytecode, rx);
                emit_byte(bytecode, ry);
                R[rx] = (R[rx] + R[ry]) & 0xFF;
            } else if (type == 1) {
                emit_op(bytecode, VM::OP_XOR_REG);
                emit_byte(bytecode, rx);
                emit_byte(bytecode, ry);
                R[rx] = (R[rx] ^ R[ry]) & 0xFF;
            } else {
                emit_op(bytecode, VM::OP_SBOX_SUB);
                emit_byte(bytecode, rx);
                R[rx] = VM::SBOX[R[rx]];
            }
        }

        // Calculate Mask
        uint8_t mask = R[0] ^ R[1] ^ R[2] ^ R[3];
        uint8_t cipher = p ^ mask;
        
        // Emit Decrypt Block Opcode
        emit_op(bytecode, VM::OP_DECRYPT_BLOCK);
        emit_byte(bytecode, cipher);
        
        // Side Effects
        R[0] = (R[0] + cipher) & 0xFF;
        R[1] = (R[1] ^ R[0]) & 0xFF;
        R[2] = VM::SBOX[R[2]];
        R[3] = VM::SBOX[(R[3] + 1) & 0xFF];
    }
    
    emit_op(bytecode, VM::OP_HALT);
    return bytecode;
}

// -----------------------------------------------------------------------------
// VM Interpreter (Decryption)
// -----------------------------------------------------------------------------

std::vector<uint8_t> run_vm(const std::vector<uint8_t>& bytecode, bool env_valid) {
    std::vector<uint8_t> output;
    output.reserve(bytecode.size() / 2); // Approximate

    // VM State
    uint8_t R[4] = {0, 0, 0, 0};
    
    // If environment is invalid (e.g. wrong signature), corrupt the initial state or keys
    // This causes the app to decrypt garbage instead of crashing immediately (harder to debug)
    if (!env_valid) {
        R[0] = 0xDE;
        R[1] = 0xAD;
        R[2] = 0xBE;
        R[3] = 0xEF;
    }

    size_t ip = 0; // Instruction Pointer

    while (ip < bytecode.size()) {
        uint8_t op = bytecode[ip++];
        
        if (op == VM::OP_HALT) break;
        if (ip >= bytecode.size()) break;

        switch (op) {
            case VM::OP_MOV_IMM: {
                uint8_t idx = bytecode[ip++] % 4;
                uint8_t val = bytecode[ip++];
                // If environment is invalid, we might ignore key loads or corrupt them
                if (env_valid) {
                    R[idx] = val;
                } else {
                    R[idx] = val ^ 0xFF; // Corrupt key
                }
                break;
            }
            case VM::OP_MOV_REG: {
                uint8_t idx = bytecode[ip++] % 4;
                uint8_t idy = bytecode[ip++] % 4;
                R[idx] = R[idy];
                break;
            }
            case VM::OP_ADD_REG: {
                uint8_t idx = bytecode[ip++] % 4;
                uint8_t idy = bytecode[ip++] % 4;
                R[idx] = (R[idx] + R[idy]) & 0xFF;
                break;
            }
            case VM::OP_SUB_REG: {
                uint8_t idx = bytecode[ip++] % 4;
                uint8_t idy = bytecode[ip++] % 4;
                R[idx] = (R[idx] - R[idy]) & 0xFF;
                break;
            }
            case VM::OP_XOR_REG: {
                uint8_t idx = bytecode[ip++] % 4;
                uint8_t idy = bytecode[ip++] % 4;
                R[idx] = (R[idx] ^ R[idy]) & 0xFF;
                break;
            }
            case VM::OP_SBOX_SUB: {
                uint8_t idx = bytecode[ip++] % 4;
                R[idx] = VM::SBOX[R[idx]];
                break;
            }
            case VM::OP_DECRYPT_BLOCK: {
                uint8_t cipher = bytecode[ip++];
                
                // 1. Calculate Mask
                uint8_t mask = R[0] ^ R[1] ^ R[2] ^ R[3];
                
                // 2. Decrypt
                uint8_t plain = cipher ^ mask;
                output.push_back(plain);
                
                // 3. Side Effects (Must match Encryptor)
                R[0] = (R[0] + cipher) & 0xFF;
                R[1] = (R[1] ^ R[0]) & 0xFF;
                R[2] = VM::SBOX[R[2]];
                R[3] = VM::SBOX[(R[3] + 1) & 0xFF];
                break;
            }
            default:
                // Unknown opcode, maybe just skip or halt
                // LOGE("Unknown Op: %02x", op);
                break;
        }
    }
    return output;
}

// -----------------------------------------------------------------------------
// JNI Exports
// -----------------------------------------------------------------------------

extern "C" JNIEXPORT void JNICALL
Java_com_web_webapp_MainActivity_initRandom(JNIEnv* env, jclass clazz) {
    std::srand(std::time(nullptr));
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_web_webapp_MainActivity_encryptData(
        JNIEnv* env,
        jobject context,
        jbyteArray input) {
    
    // Check Env
    if (!check_signature(env, context)) return nullptr;

    jsize len = env->GetArrayLength(input);
    jbyte* data = env->GetByteArrayElements(input, 0);
    
    std::vector<uint8_t> plaintext(data, data + len);
    env->ReleaseByteArrayElements(input, data, 0);

    // Compile to Bytecode
    std::vector<uint8_t> bytecode = compile_vm(plaintext);

    jbyteArray result = env->NewByteArray(bytecode.size());
    env->SetByteArrayRegion(result, 0, bytecode.size(), reinterpret_cast<const jbyte*>(bytecode.data()));
    return result;
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_web_webapp_MainActivity_decryptData(
        JNIEnv* env,
        jobject context,
        jbyteArray input) {

    // Check Env
    bool valid = check_signature(env, context);

    jsize len = env->GetArrayLength(input);
    jbyte* data = env->GetByteArrayElements(input, 0);
    
    std::vector<uint8_t> bytecode(data, data + len);
    env->ReleaseByteArrayElements(input, data, 0);

    // Run VM
    std::vector<uint8_t> decrypted = run_vm(bytecode, valid);

    jbyteArray result = env->NewByteArray(decrypted.size());
    env->SetByteArrayRegion(result, 0, decrypted.size(), reinterpret_cast<const jbyte*>(decrypted.data()));
    return result;
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_web_webapp_MainActivity_decryptAsset(
        JNIEnv* env,
        jobject context, /* Context is passed as 'this' in Activity, or explicitly */
        jobject assetManager,
        jstring filename) {

    // 1. Environment Check (Anti-Tamper)
    bool is_valid = check_signature(env, context);

    const char* nativeFileName = env->GetStringUTFChars(filename, 0);
    AAssetManager* mgr = AAssetManager_fromJava(env, assetManager);
    
    if (mgr == nullptr) {
        return nullptr;
    }

    AAsset* asset = AAssetManager_open(mgr, nativeFileName, AASSET_MODE_BUFFER);
    if (asset == nullptr) {
        env->ReleaseStringUTFChars(filename, nativeFileName);
        return nullptr;
    }

    off_t length = AAsset_getLength(asset);
    std::vector<uint8_t> buffer(length);
    AAsset_read(asset, buffer.data(), length);
    AAsset_close(asset);

    env->ReleaseStringUTFChars(filename, nativeFileName);

    // 2. Run VM
    std::vector<uint8_t> decrypted = run_vm(buffer, is_valid);

    jbyteArray result = env->NewByteArray(decrypted.size());
    env->SetByteArrayRegion(result, 0, decrypted.size(), reinterpret_cast<const jbyte*>(decrypted.data()));

    return result;
}
