use jni::objects::{JClass, JString};
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

#[no_mangle]
pub extern "system" fn Java_com_nt202_knockvpn_VpnActivity_concatenateStrings(
    mut env: JNIEnv,
    _: JClass,
    str1: JString,  // First Java string
    str2: JString,  // Second Java string
) -> jstring {
    // Convert Java strings to Rust strings
    let rust_str1: String = env.get_string(&str1)
        .expect("Failed to get Rust string from Java")
        .into();
    let rust_str2: String = env.get_string(&str2)
        .expect("Failed to get Rust string from Java")
        .into();

    // Concatenate
    let combined = format!("{}{}", rust_str1, rust_str2);

    // Convert back to Java string and return as raw `jstring`
    env.new_string(combined)
        .expect("Couldn't create Java string!")
        .into_raw()
}