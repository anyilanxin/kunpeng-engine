# 微服务编排引擎

[English](README.md) | 简体中文

[![Stars](https://img.shields.io/github/stars/anyilanxin/kunpeng-engine?style=flat-square&logo=github)](https://github.com/anyilanxin/kunpeng-engine/stargazers)
[![Forks](https://img.shields.io/github/forks/anyilanxin/kunpeng-engine?style=flat-square&logo=github)](https://github.com/anyilanxin/kunpeng-engine/network/members)
[![Contributors](https://img.shields.io/github/contributors/anyilanxin/kunpeng-engine?style=flat-square&logo=github)](https://github.com/anyilanxin/kunpeng-engine/graphs/contributors)
[![Commit Activity](https://img.shields.io/github/commit-activity/m/anyilanxin/kunpeng-engine?style=flat-square&logo=github)](https://github.com/anyilanxin/kunpeng-engine/commits)
[![Release](https://img.shields.io/github/v/release/anyilanxin/kunpeng-engine?style=flat-square&logo=github)](https://github.com/anyilanxin/kunpeng-engine/releases)
[![Issues](https://img.shields.io/github/issues/anyilanxin/kunpeng-engine?style=flat-square&logo=github)](https://github.com/anyilanxin/kunpeng-engine/issues)

Kunpeng 为跨微服务的业务流程提供可视化与控制能力。

## 工作原理

<picture>
  <source media="(prefers-color-scheme: dark)" srcset="docs/kunpeng-hero-dark.svg">
  <img src="docs/kunpeng-hero-light.svg" alt="Kunpeng 工作原理">
</picture>

客户端在无状态网关的多副本之间动态分摊负载，网关将每个请求路由到 Raft 组的主分区；BPMN 2.0 引擎在分区内执行流程，每一步都追加写入分区事件日志；同一份日志再经多个 sink 扇出到多个外部存储——支持断点续传，且完全不在执行路径上。客户端侧的连接器运行时以 job worker 方式承载各类集成：开箱即用 REST 与脚本连接器，并可通过 SDK 扩展 Kafka、RocketMQ、邮件等。

**为什么选择 Kunpeng？**

* 以 [BPMN 2.0](https://www.omg.org/spec/BPMN/2.0.2/) 可视化定义流程
* 自由选择编程语言
* 使用 [Docker](https://www.docker.com/) 与 [Kubernetes](https://kubernetes.io/) 部署
* 构建可响应 [Kafka](https://kafka.apache.org/) 等消息队列消息的流程
* 水平扩展，应对超高吞吐
* 容错能力（无需关系型数据库）
* 导出流程数据用于监控与分析
* 参与活跃的社区

## 相关链接

* [文档首页](./docs)
* [快速上手：启动形态与端口模型](./docs/user-guide/getting-started.zh-CN.md)
* [发行版装配与部署](./dist/README.md)
* [Java 客户端 SDK](./clients/client-java)
* [集群消息层基准结论](./docs/cluster-messaging-benchmark.md)
* [Sink 对接指南（自研日志外发）](./docs/sink-integration.md)

## Star 趋势

[![Star History Chart](https://api.star-history.com/svg?repos=anyilanxin/kunpeng-engine&type=Date)](https://star-history.com/#anyilanxin/kunpeng-engine&Date)

## 许可证

本仓库为多许可仓库，**默认协议是 AGPL-3.0**：除个别特别指定的目录外，仓库内代码均按 AGPL-3.0 授权；少数目录采用 Apache-2.0 或 MPL-2.0，协议按目录整体归属——目录内外部引入的文件即使未带协议头，同样遵循所在目录的协议。所用协议的完整文本均存放于 [`licenses/`](./licenses/)。

### 1. 使用许可

1. [APACHE-2.0](./licenses/APACHE-2.0.txt)
2. [GNU-AGPL-3.0](./licenses/GNU-AGPL-3.0.txt)
3. [MPL 2.0](./licenses/MPL-2.0.txt)

### 2. 许可声明

1. 各目录适用的许可证由本项目统一指定，协议完整文本见 [`licenses/`](./licenses)；文件头仅承载版权归属信息
2. 请勿改动版权头：上游文件的版权必须原样保留；我们新建的文件则带有自己的许可证头与版权声明
3. 在协议兼容允许的前提下，大部分文件已加上自有版权头（我们对这些文件做了大量调整，以支持快速迭代）

### 3. 合规反馈与监督

本仓库整合并改造了多个上游项目。我们尽力遵循各上游协议要求，但多协议组合较为复杂，仍可能存在无意的不合规之处（如标注遗漏、协议归属不当）。

如发现许可违规、版权标注错误或其他合规问题，欢迎通过以下渠道反馈：

1. 提交 [GitHub Issue](https://github.com/anyilanxin/kunpeng-engine/issues/new)
2. 发送邮件至 anyilanxin@aliyun.com

我们承诺尽快核实每一条反馈，确认问题后在合理期限内修正（补全标注、调整协议归属或移除相关代码），并在 Issue 中公开处理结果。
