use jni::objects::{JClass, JString};
use jni::sys::jint;
use jni::sys::jstring;
use jni::JNIEnv;
use ssh_socks::start_with_password;
// use tun2socks::{main_from_str, quit};

use std::sync::atomic::{AtomicU16, Ordering};
use tokio::runtime::Runtime;

mod ssh_socks;


// Shared atomic port to track the server's bound port
static PORT: AtomicU16 = AtomicU16::new(0); // 0 if error.

#[no_mangle]
pub extern "system" fn Java_com_nt202_knockvpn_VpnActivity_startSocksServer(
    mut env: JNIEnv,
    _: JClass,
    username: JString,
    address: JString,
    port: jint,
    password: JString, // Might be empty
    key: JString, // private key. Might be empty
) -> jstring {
    PORT.store(0, Ordering::SeqCst);

    let rust_password: String = env
        .get_string(&password)
        .expect("lHcSzg")
        .into();

    let rust_key: String = env
        .get_string(&key)
        .expect("odnq8P")
        .into();

    if rust_password.is_empty() && rust_key.is_empty() {
        return env.new_string(format!(r#"{{"error": "{}"}}"#, "xLLsAw")).unwrap().into_raw();
    }

    if !rust_password.is_empty() && !rust_key.is_empty() {
        return env.new_string(format!(r#"{{"error": "{}"}}"#, "bxr4mD")).unwrap().into_raw();
    }

    let rust_username: String = env
        .get_string(&username)
        .expect("YcLJMc")
        .into();

    let rust_address: String = env
        .get_string(&address)
        .expect("GATFFD")
        .into();

    let rust_port = port as u16;

    if !rust_password.is_empty() {

        // Create a Tokio runtime for async HTTP server
        let rt = Runtime::new().unwrap();

        // Spawn the server in a new thread
        std::thread::spawn(move || {
            rt.block_on(async {
                let result = start_with_password(rust_username, rust_address, rust_port, rust_password).await;
                match result {
                    Ok(bound_port) => {
                        PORT.store(bound_port, Ordering::SeqCst);
                    }
                    Err(e) => {
                        PORT.store(0, Ordering::SeqCst);
                    }
                }
            });
        });

        

    } else {
        return env.new_string(format!(r#"{{"error": "{}"}}"#, "0OyuTy")).unwrap().into_raw();
    }


    return env.new_string(format!(r#"{{"error": "{}"}}"#, "")).unwrap().into_raw();
}

#[no_mangle]
pub extern "system" fn Java_com_nt202_knockvpn_VpnActivity_getServerPort(
    _env: JNIEnv,
    _: JClass,
) -> jint {
    PORT.load(Ordering::SeqCst) as jint
}

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
    str1: JString, // First Java string
    str2: JString, // Second Java string
) -> jstring {
    // Convert Java strings to Rust strings
    let rust_str1: String = env
        .get_string(&str1)
        .expect("Failed to get Rust string from Java")
        .into();
    let rust_str2: String = env
        .get_string(&str2)
        .expect("Failed to get Rust string from Java")
        .into();

    // Concatenate
    let combined = format!("{}{}", rust_str1, rust_str2);

    // Convert back to Java string and return as raw `jstring`
    env.new_string(combined)
        .expect("Couldn't create Java string!")
        .into_raw()
}
