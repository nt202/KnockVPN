use std::borrow::Cow;
use std::net::{Ipv4Addr, Ipv6Addr};
use std::sync::Arc;
use std::time::Duration;

use anyhow::Result;
use log::info;
use russh::client::Handler;
use russh::keys::PublicKey;
use russh::ChannelMsg;
use russh::*;
use tokio::io::{AsyncReadExt, AsyncWriteExt};
use tokio::net::ToSocketAddrs;
use tokio::sync::Mutex;

#[tokio::main]
async fn main() -> Result<()> {
    env_logger::builder()
        .filter_level(log::LevelFilter::Debug)
        .init();

    let ssh = Session::connect_password(
        "qwerty".to_string(),
        "testuser".to_string(),
        ("127.0.0.1", 2222),
    )
    .await?;
    info!("Connected");

    let ssh_clone = ssh.clone();
    let socks_task = tokio::spawn(async move {
        if let Err(e) = ssh_clone.start_socks_proxy("127.0.0.1:1080").await {
            eprintln!("SOCKS proxy error: {:?}", e);
        }
    });

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
            inactivity_timeout: Some(Duration::from_secs(5)),
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
        let mut handle = self.handle.lock().await;
        handle
            .disconnect(Disconnect::ByApplication, "", "English")
            .await?;
        Ok(())
    }

    async fn start_socks_proxy(&self, local_addr: &str) -> Result<()> {
        let listener = tokio::net::TcpListener::bind(local_addr).await?;
        info!("SOCKS proxy listening on {}", local_addr);

        loop {
            let (stream, addr) = listener.accept().await?;
            info!("Accepted connection from {}", addr);

            let session = self.clone();
            tokio::spawn(async move {
                if let Err(e) = session.handle_socks_client(stream).await {
                    eprintln!("Error handling client: {:?}", e);
                }
            });
        }
    }

    async fn handle_socks_client(&self, mut stream: tokio::net::TcpStream) -> Result<()> {
        // SOCKS5 handshake
        let mut handshake = [0u8; 2];
        stream.read_exact(&mut handshake).await?;

        if handshake[0] != 0x05 {
            anyhow::bail!("Unsupported SOCKS version: {}", handshake[0]);
        }

        // Read methods
        let mut methods = vec![0u8; handshake[1] as usize];
        stream.read_exact(&mut methods).await?;

        // Send no-auth response
        stream.write_all(&[0x05, 0x00]).await?;

        // Read request
        let mut request = [0u8; 4];
        stream.read_exact(&mut request).await?;

        if request[0] != 0x05 || request[1] != 0x01 {
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
            _ => anyhow::bail!("Invalid address type"),
        };

        // Open SSH channel
        let mut handle = self.handle.lock().await;
        let channel = handle
            .channel_open_direct_tcpip(&host, port.into(), "0.0.0.0", 0)
            .await?;

        // Split channel into read/write halves
        let (mut channel_read, mut channel_write) = channel.split();

        // Send success response
        stream
            .write_all(&[0x05, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
            .await?;

        // Split TCP stream
        let (mut client_read, mut client_write) = stream.split();

        // Client -> SSH Server
        let client_to_channel = async move {
            let mut buf = [0u8; 4096];
            loop {
                let n = client_read.read(&mut buf).await?;
                if n == 0 {
                    break;
                }
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
                client_write.write_all(&buf[..n]).await?;
            }
            Ok::<_, anyhow::Error>(())
        };

        tokio::try_join!(client_to_channel, channel_to_client)?;
        Ok(())
    }
}
