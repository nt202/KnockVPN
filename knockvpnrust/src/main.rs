use std::borrow::Cow;
use std::net::{Ipv4Addr, Ipv6Addr};
use std::sync::Arc;
use std::thread::sleep;
use std::time::Duration;

use anyhow::Result;
use lib::start_with_password;
use log::info;
use russh::client::Handler;
use russh::keys::PublicKey;
use russh::*;
use tokio::io::{AsyncReadExt, AsyncWriteExt};
use tokio::net::{TcpListener, ToSocketAddrs};
use tokio::runtime::Runtime;
use tokio::sync::Mutex;
mod lib;

fn main() {
        let rt = Runtime::new().unwrap();

        // Spawn the server in a new thread
        std::thread::spawn(move || {
            rt.block_on(async {
                let _ = start_with_password("testuser".to_string(), "127.0.0.1".to_string(), 2222, "qwerty".to_string()).await;
            });
        });
        sleep(Duration::from_secs(100));
}