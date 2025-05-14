use jni::objects::{JClass, JString};
use jni::sys::jint;
use jni::JNIEnv;

// // Function to return "Hello World"
// #[no_mangle]
// pub extern "system" fn Java_com_example_myapp_MainActivity_helloFromRust(
//     env: JNIEnv,
//     _: JClass,
// ) -> JString {
//     env.new_string("Hello from Rust!").unwrap()
// }

// Function to sum two integers
#[no_mangle]
pub extern "system" fn Java_com_nt202_knockvpn_VpnActivity_sumFromRust(
    _: JNIEnv,
    _: JClass,
    a: jint,
    b: jint,
) -> jint {
    a + b
}