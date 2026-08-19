# kunpeng-cluster 更改说明

[English](./README.md)

本模块的代码来源于 Camunda（Zeebe）仓库的 hard fork 版本 Atomix：

- 上游仓库：<https://github.com/camunda/camunda>
- 来源提交：<https://github.com/camunda/camunda/commit/d9de58a0cee5a84df87cba1ebc0d4fee64836e7d>（对应 Camunda 8.0.4 tag）
- 上游仓库的 fork 存档：<https://github.com/anyilanxin/camunda/commit/d9de58a0cee5a84df87cba1ebc0d4fee64836e7d>

## 上游背景（Camunda 对 Atomix 的 hard fork 说明）

本模块包含的是 [Atomix](https://github.com/atomix/atomix) 的一个 hard fork。最初 Camunda 将 Atomix
作为普通依赖使用，但由于上游仓库活跃度低、PR 长期未被合并，且上游已转向用 Go 重写整个项目，
Camunda 决定将 fork 直接合入自己的仓库，并裁剪到只保留真正使用和需要的部分，包括：

- RAFT 实现（含存储）
- SWIM 实现
- 可用的 transport 实现
- 以及一些粘合代码

这样做的好处是：统一构建、缩短开发与测试周期、更方便做 benchmark 和代码质量检查、简化发布流程；
代价是初期测试更不稳定、构建时间更长。

## 本仓库的更改

在 Camunda 8.0.4 版本的基础上，本仓库做了进一步的裁剪与调整，目的是**方便维护**：

1. **只保留 atomix 与 journal 相关代码**，其余模块全部删除。
2. **删除了以 Camunda License 1.0 协议发布的代码**，本模块内剩余代码均为 Apache-2.0 许可（详见各自文件的 license header）。
3. 构建方式由 Maven 迁移至 Gradle，并合入本仓库统一构建。
4. **后续新增的文件，其适用协议以文件自身的 license header 与版权声明为准**，不再采用 Apache 协议。

## 版权与许可注意事项

- 上游（Atomix / Camunda）原有的版权声明必须保留，请勿修改或删除原文件中的 copyright header。
- 新增文件的适用协议以各文件自身的 license header 与版权声明为准。
