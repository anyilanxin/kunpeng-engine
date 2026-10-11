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

客户端在无状态网关的多副本之间动态分摊负载，网关将每个请求路由到 Raft 组的主分区；BPMN 2.0 引擎在分区内执行流程，每一步都追加写入分区事件日志；同一份日志再经多个 sink 扇出到多个外部存储——支持断点续传，且完全不在执行路径上。

**为什么选择 Kunpeng？**

* 以 [BPMN 2.0](https://www.omg.org/spec/BPMN/2.0.2/) 可视化定义流程
* 自由选择编程语言
* 使用 [Docker](https://www.docker.com/) 与 [Kubernetes](https://kubernetes.io/) 部署
* 构建可响应 [Kafka](https://kafka.apache.org/) 等消息队列消息的流程
* 水平扩展，应对超高吞吐
* 容错能力（无需关系型数据库）
* 导出流程数据用于监控与分析
* 参与活跃的社区

## Star 趋势

[![Star History Chart](https://api.star-history.com/svg?repos=anyilanxin/kunpeng-engine&type=Date)](https://star-history.com/#anyilanxin/kunpeng-engine&Date)

## 许可证

### 1. 使用许可

1. [APACHE-2.0](./licenses/APACHE-2.0.txt)
2. [GNU-AGPL-3.0](./licenses/GNU-AGPL-3.0.txt)
3. [MPL 2.0](./licenses/MPL-2.0.txt)

### 2. 许可声明

1. 文件适用的许可证详情见文件头
2. 请勿改动版权头：上游文件的版权必须原样保留；我们新建的文件则带有自己的许可证头与版权声明
3. 在协议兼容允许的前提下，大部分文件已加上自有版权头（我们对这些文件做了大量调整，以支持快速迭代）
