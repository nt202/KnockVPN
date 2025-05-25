```
[Android Device]
  │
  ▼
VpnService (TUN interface)
  │
  ▼
Rust Packet Parser (extract TCP/UDP)
  │
  ▼
Rust SOCKS5 Client (connect to 127.0.0.1:1080, async-socks5)
  │
  ▼
SSH Tunnel (via Rust, thrussh)
  │
  ▼
[Remote Server]
```

```
~/Library/Android/sdk/ndk/26.1.10909125/ndk-build
```