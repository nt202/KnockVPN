use std::borrow::Cow;
use std::sync::Arc;
use std::time::Duration;

use anyhow::Result;
use log::info;
use russh::keys::PublicKey;
use russh::*;
use tokio::io::AsyncWriteExt;
use tokio::net::ToSocketAddrs;

#[tokio::main]
async fn main() -> Result<()> {
    env_logger::builder()
        .filter_level(log::LevelFilter::Debug)
        .init();
    let mut ssh = Session::connect_password(
        "qwerty".to_string(),
        "testuser".to_string(),
        ("127.0.0.1", 2222),
    )
    .await?;
    info!("Connected");
    let code = ssh
        .call("ls")
        .await?;
    println!("Exitcode: {:?}", code);
    ssh.close().await?;
    Ok(())
}

struct Client {}

impl client::Handler for Client {
    type Error = russh::Error;

    async fn check_server_key(
        &mut self,
        _server_public_key: &PublicKey,
    ) -> Result<bool, Self::Error> {
        Ok(true)
    }
}

pub struct Session {
    session: client::Handle<Client>,
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
                kex: Cow::Owned(vec![
                    russh::kex::CURVE25519_PRE_RFC_8731,
                    russh::kex::EXTENSION_SUPPORT_AS_CLIENT,
                ]),
                ..Default::default()
            },
            ..<_>::default()
        };
        let config = Arc::new(config);
        let sh = Client {};
        let mut session = client::connect(config, addrs, sh).await?;
        let auth_res = session.authenticate_password(user, password).await?;
        if !auth_res.success() {
            anyhow::bail!("Authentication with password failed");
        }
        Ok(Self { session })
    }

    async fn call(&mut self, command: &str) -> Result<u32> {
        let mut channel = self.session.channel_open_session().await?;
        channel.exec(true, command).await?;
        let mut code = None;
        let mut stdout = tokio::io::stdout();
        loop {
            let Some(msg) = channel.wait().await else {
                break;
            };
            match msg {
                ChannelMsg::Data { ref data } => {
                    stdout.write_all(data).await?;
                    stdout.flush().await?;
                }
                ChannelMsg::ExitStatus { exit_status } => {
                    code = Some(exit_status);
                }
                _ => {}
            }
        }
        Ok(code.expect("program did not exit cleanly"))
    }

    async fn close(&mut self) -> Result<()> {
        self.session
            .disconnect(Disconnect::ByApplication, "", "English")
            .await?;
        Ok(())
    }
}
