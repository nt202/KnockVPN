use jni::objects::{JClass, JString};
use jni::sys::jint;
use jni::sys::jstring;
use jni::JNIEnv;

use std::net::SocketAddr;
use std::sync::atomic::{AtomicU16, Ordering};
use std::sync::Arc;
use tokio::runtime::Runtime;
use warp::Filter;

// Shared atomic port to track the server's bound port
static PORT: AtomicU16 = AtomicU16::new(0);

#[no_mangle]
pub extern "system" fn Java_com_nt202_knockvpn_VpnActivity_startHttpServer(
    env: JNIEnv,
    _: JClass,
) -> jint {
    // Create a Tokio runtime for async HTTP server
    let rt = Runtime::new().unwrap();

    // Spawn the server in a new thread
    std::thread::spawn(move || {
        rt.block_on(async {
            // Define a simple route
            let hello =
                warp::path!("hello" / String).map(|name| format!("Hello, {} from Rust!", name));

            // Bind to port 0 (OS picks a free port)
            let addr = SocketAddr::from(([0, 0, 0, 0], 0));
            let (addr, server) = warp::serve(hello).bind_ephemeral(addr);

            // Store the assigned port
            PORT.store(addr.port(), Ordering::SeqCst);

            // Run the server forever
            server.await;
        });
    });

    // Return the port number to Java
    PORT.load(Ordering::SeqCst) as jint
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
