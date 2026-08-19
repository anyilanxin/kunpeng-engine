### demo

```text

-Dserver.port=2020 -Dmanagement.server.port=9600 -Dkunpeng.cluster.node-id=node01 -Dkunpeng.cluster.network.start-port=2026 -Dkunpeng.broker.gateway.network.port=2024 --add-opens=java.base/jdk.internal.misc=ALL-UNNAMED

-Dserver.port=2021 -Dmanagement.server.port=9601 -Dkunpeng.cluster.node-id=node02 -Dkunpeng.cluster.network.start-port=2035 -Dkunpeng.broker.gateway.network.port=2034 --add-opens=java.base/jdk.internal.misc=ALL-UNNAMED


-Dserver.port=2022 -Dmanagement.server.port=9602 -Dkunpeng.cluster.node-id=node03 -Dkunpeng.cluster.network.start-port=2045 -Dkunpeng.broker.gateway.network.port=2044 --add-opens=java.base/jdk.internal.misc=ALL-UNNAMED

```

### 启动由于 agrona 限制，需要添加如下 jvm 参数
```text
--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED
```
