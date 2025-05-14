#include <jni.h>
#include <string>

//extern "C" JNIEXPORT jstring JNICALL
//Java_com_example_myapp_MainActivity_helloFromRust(
//        JNIEnv* env,
//        jobject /* this */);

//extern "C" JNIEXPORT jint JNICALL
extern "C" JNIEXPORT jint  extern "C" JNICALL
Java_com_nt202_knockvpn_VpnActivity_sumFromRust(
        JNIEnv* env,
        jobject /* this */,
        jint a,
        jint b);

//extern "C" JNIEXPORT jstring JNICALL
//Java_com_example_myapp_MainActivity_stringFromJNI(
//        JNIEnv* env,
//        jobject /* this */) {
//    return Java_com_example_myapp_MainActivity_helloFromRust(env, nullptr);
//}

extern "C" JNIEXPORT jint  extern "C" jint
Java_com_nt202_knockvpn_VpnActivity_sumFromJNI(
//Java_com_example_myapp_MainActivity_sumFromJNI(
        JNIEnv* env,
        jobject /* this */,
        jint a,
        jint b) {
    return Java_com_nt202_knockvpn_VpnActivity_sumFromRust(env, nullptr, a, b);
}