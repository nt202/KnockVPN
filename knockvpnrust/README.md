```
rustup target add armv7-linux-androideabi aarch64-linux-android i686-linux-android x86_64-linux-android
```

```
cargo build --target armv7-linux-androideabi --release
cargo build --target aarch64-linux-android --release
cargo build --target i686-linux-android --release
cargo build --target x86_64-linux-android --release
```

```
ssh -D 1080 -C -N -p 2222 testuser@127.0.0.1
```

```
sudo /usr/sbin/sshd -f sshd_config_test -D
ssh testuser@127.0.0.1 -p 2222
```

```
fuser -k 1080/tcp
```

```
curl -x socks5://127.0.0.1:1080 https://dummyjson.com/todos/1
```