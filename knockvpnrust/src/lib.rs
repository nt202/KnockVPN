use jni::objects::{JClass, JString};
use jni::sys::jint;
use jni::sys::jstring;
use jni::JNIEnv;
use tokio::time::timeout;
use std::sync::atomic::{AtomicI32, AtomicU16, Ordering};
use tokio::runtime::Runtime;
use std::os::fd::AsRawFd;
use std::borrow::Cow;
use std::net::{Ipv4Addr, Ipv6Addr};
use std::sync::Arc;
use std::time::Duration;

use anyhow::Result;
use russh::client::Handler;
use russh::keys::PublicKey;
use russh::*;
use tokio::io::{AsyncReadExt, AsyncWriteExt};
use tokio::net::ToSocketAddrs;
use tokio::sync::Mutex;
use log::{info, LevelFilter};
use android_logger::Config;


static SOCKS_SERVER_PORT: AtomicU16 = AtomicU16::new(0);
static SOCKS_SERVER_FD: AtomicI32 = AtomicI32::new(0);

#[no_mangle]
pub extern "C" fn Java_com_nt202_knockvpn_VpnActivity_initLogging(env: JNIEnv, _: JClass) {
    android_logger::init_once(
        Config::default()
            .with_tag("KnockVpnRust")
            .with_max_level(LevelFilter::Trace)
    );
    info!("Rust logging initialized!");
}

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
    SOCKS_SERVER_PORT.store(0, Ordering::SeqCst);

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
                let _ = start_with_password(rust_username, rust_address, rust_port, rust_password).await;
            });
        });
    } else {
        return env.new_string(format!(r#"{{"error": "{}"}}"#, "0OyuTy")).unwrap().into_raw();
    }
    return env.new_string(format!(r#"{{"error": "{}"}}"#, "")).unwrap().into_raw();
}

#[no_mangle]
pub extern "system" fn Java_com_nt202_knockvpn_VpnActivity_getSocksPort(
    _env: JNIEnv,
    _: JClass,
) -> jint {
    SOCKS_SERVER_PORT.load(Ordering::SeqCst) as jint
}

#[no_mangle]
pub extern "system" fn Java_com_nt202_knockvpn_VpnActivity_getSocksFd(
    _env: JNIEnv,
    _: JClass,
) -> jint {
    SOCKS_SERVER_FD.load(Ordering::SeqCst) as jint
}

pub async fn start_with_password(username: String, address: String, port: u16, password: String) -> Result<()> {
    let ssh = Session::connect_password(
        password,
        username,
        (address, port),
    )
    .await?;
    info!("kvuor2: Connected");

    let ssh_clone = ssh.clone();

    let socks_task = tokio::spawn(async move {
        if let Err(e) = ssh_clone.start_socks_proxy2().await {
            eprintln!("XBg4kq: SOCKS proxy error: {:?}", e);
        }
    });

    info!("{}: {}", "NXeyfq", "Started");

    tokio::signal::ctrl_c().await.ok();
    info!("Shutting down...");

    socks_task.await.ok();
    ssh.close().await?;
    
    Ok(())
}

#[derive(Clone)]
struct Client;

impl Handler for Client {
    type Error = russh::Error;

    async fn check_server_key(
        &mut self,
        _server_public_key: &PublicKey,
    ) -> Result<bool, Self::Error> {
        Ok(true)
    }
}

#[derive(Clone)]
pub struct Session {
    handle: Arc<Mutex<client::Handle<Client>>>,
}

impl Session {
    async fn connect_password<A: ToSocketAddrs>(
        password: String,
        user: impl Into<String>,
        addrs: A,
    ) -> Result<Self> {
        let config = client::Config {
            inactivity_timeout: Some(Duration::from_secs(1000000)),
            preferred: Preferred {
                kex: Cow::Borrowed(&[
                    russh::kex::CURVE25519_PRE_RFC_8731,
                    russh::kex::EXTENSION_SUPPORT_AS_CLIENT,
                ]),
                ..Default::default()
            },
            ..Default::default()
        };

        let config = Arc::new(config);
        let sh = Client {};
        let mut session = client::connect(config, addrs, sh).await?;
        let auth_res = session.authenticate_password(user, password).await?;

        if !auth_res.success() {
            anyhow::bail!("Authentication failed");
        }

        Ok(Self {
            handle: Arc::new(Mutex::new(session)),
        })
    }

    async fn close(&self) -> Result<()> {
        info!("Before locking in close");
        let handle = self.handle.lock().await;
        info!("After locking in close");
        handle
            .disconnect(Disconnect::ByApplication, "", "English")
            .await?;
        Ok(())
    }

    async fn start_socks_proxy2(&self) -> Result<String> {
        let listener = tokio::net::TcpListener::bind("127.0.0.1:0").await?;

        let bound_address = listener.local_addr().expect("EVN7Nq");
        let bound_port = bound_address.port();
        SOCKS_SERVER_PORT.store(bound_port, Ordering::SeqCst);

        let raw_fd = listener.as_raw_fd(); // Get the raw file descriptor
        SOCKS_SERVER_FD.store(raw_fd, Ordering::SeqCst);

        loop {
            let (stream, addr) = listener.accept().await?;
            info!("8x49Qj: Accepted connection from {}", addr);

            let session = self.clone();
            tokio::spawn(async move {
                if let Err(e) = session.handle_socks_client(stream).await {
                    eprintln!("xu8GWI: Error handling client: {:?}", e);
                }
            });
        }
    }

    async fn handle_socks_client(&self, mut stream: tokio::net::TcpStream) -> Result<()> {
        // SOCKS5 handshake
        let mut handshake = [0u8; 2];
        stream.read_exact(&mut handshake).await?;

        if handshake[0] != 0x05 {
            stream.write_all(&[0x05, 0xff]).await?; // No acceptable methods
            anyhow::bail!("L8PRRA: Unsupported SOCKS version: {}", handshake[0]);
        } else {
            info!("fJzYmx: Supported version: {}", handshake[0]);
        }

        // Read methods
        let mut methods = vec![0u8; handshake[1] as usize];
        stream.read_exact(&mut methods).await?;
        info!("Received methods: {:?}", methods);

        // Send no-auth response
        stream.write_all(&[0x05, 0x00]).await?;
        info!("Sent no-auth response");

        // Read request
        let mut request = [0u8; 4];
        stream.read_exact(&mut request).await?;
        info!("Received request: {:?}", request);

        if request[0] != 0x05 || request[1] != 0x01 {
            // Send error response (e.g., command not supported)
            stream
                .write_all(&[0x05, 0x07, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
                .await?;
            stream.flush().await?;
            anyhow::bail!("Unsupported command: {:?}", request);
        }

        // Parse address type
        let (host, port) = match request[3] {
            0x01 => {
                // IPv4
                let mut ip = [0u8; 4];
                stream.read_exact(&mut ip).await?;
                let port = stream.read_u16().await?;
                (Ipv4Addr::from(ip).to_string(), port)
            }
            0x03 => {
                // Domain
                let len = stream.read_u8().await? as usize;
                let mut domain = vec![0u8; len];
                stream.read_exact(&mut domain).await?;
                let port = stream.read_u16().await?;
                (String::from_utf8(domain)?, port)
            }
            0x04 => {
                // IPv6
                let mut ip = [0u8; 16];
                stream.read_exact(&mut ip).await?;
                let port = stream.read_u16().await?;
                (Ipv6Addr::from(ip).to_string(), port)
            }
            _ => {
                stream
                    .write_all(&[0x05, 0x08, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
                    .await?;
                stream.flush().await?;
                anyhow::bail!("Invalid address type");
            }
        };
        info!("Destination: {}:{}", host, port);

        // Open SSH channel
        info!("Before locking in handling client");
        let channel = {
            let handle = self.handle.lock().await;
            info!("After locking in handling client");
            info!("Opening SSH channel to {}:{}", host, port);
            handle.channel_open_direct_tcpip(&host, port.into(), "0.0.0.0", 0).await?
        };


        // let handle = self.handle.lock().await;
        // info!("After locking in handling client");
        // info!("Opening SSH channel to {}:{}", host, port);
        // // let channel = handle.channel_open_direct_tcpip(&host, port.into(), "0.0.0.0", 0).await?;
        // let channel = timeout(Duration::from_secs(10), handle
        //     .channel_open_direct_tcpip(&host, port.into(), "0.0.0.0", 0))
        //     .await
        //     .map_err(|_| {
        //         // Send SOCKS5 error response (general failure)
        //         let _ = stream
        //             .write_all(&[0x05, 0x01, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00]);
        //         anyhow::anyhow!("Timeout opening SSH channel")
        //     })??;
        info!("SSH channel opened successfully to {}:{}", host, port);

        // Split channel into read/write halves
        let (mut channel_read, channel_write) = channel.split();

        // Send success response
        stream
            .write_all(&[0x05, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
            .await?;
        info!("Sent success response");

        // Split TCP stream
        let (mut client_read, mut client_write) = stream.split();

        // Client -> SSH Server
        let client_to_channel = async move {
            let mut buf = [0u8; 4096];
            loop {
                let n = client_read.read(&mut buf).await?;
                if n == 0 {
                    info!("Client closed connection");
                    break;
                }
                info!("Forwarding {} bytes from client to SSH", n);
                channel_write.data(&buf[..n]).await?;
            }
            channel_write.eof().await?;
            Ok::<_, anyhow::Error>(())
        };

        // SSH Server -> Client
        let channel_to_client = async move {
            let mut reader = channel_read.make_reader();
            let mut buf = [0u8; 4096];
            loop {
                let n = reader.read(&mut buf).await?;
                if n == 0 {
                    break;
                }
                info!("Forwarding {} bytes from SSH to client", n);
                client_write.write_all(&buf[..n]).await?;
            }
            Ok::<_, anyhow::Error>(())
        };

        info!("Starting bidirectional forwarding");
        tokio::try_join!(client_to_channel, channel_to_client)?;
        info!("Connection closed");
        Ok(())
    }
}
