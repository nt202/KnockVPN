use jni::objects::JClass;
use jni::sys::jint;
use jni::JNIEnv;
use jni::sys::jstring;

#[no_mangle]
pub extern "system" fn Java_com_nt202_knockvpn_VpnActivity_helloFromRust(
    env: JNIEnv,
    _: JClass,
) -> jstring {
    env.new_string("Hello from Rust!").unwrap().into_raw()
}

#[no_mangle]
pub extern "system" fn Java_com_nt202_knockvpn_VpnActivity_sumFromRust(
    _: JNIEnv,
    _: JClass,
    a: jint,
    b: jint,
) -> jint {
    a + b
}