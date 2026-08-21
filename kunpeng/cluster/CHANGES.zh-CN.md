# kunpeng-cluster 更改说明

[English](./CHANGES.md)

本模块的代码来源于 Camunda（Zeebe）仓库的 hard fork 版本 Atomix：

- 上游仓库：<https://github.com/camunda/camunda>
- 来源提交：<https://github.com/camunda/camunda/commit/a8fb2a5868e54f118a085bc688338c49950adf0c>（2026-08-19 取自主分支）
- 上游仓库的 fork 存档：<https://github.com/anyilanxin/camunda/commit/a8fb2a5868e54f118a085bc688338c49950adf0c>

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

本次更改的主要目的是：**解决协议冲突**，以及**方便自己维护和迭代**。在上游代码的基础上，本仓库做了如下调整：

1. **删除了所有非 Apache-2.0 协议的代码**（例如以 Camunda License 1.0 协议发布的代码），以解决协议冲突。本模块内剩余代码均为 Apache-2.0 许可（详见各自文件的 license header）。
2. **只保留真正需要的代码**（atomix 与 journal），其余模块全部删除，让代码库保持精简、便于维护和迭代。
3. 构建方式由 Maven 迁移至 Gradle，并合入本仓库统一构建。
4. **后续新增的文件，其适用协议以文件自身的 license header 与版权声明为准**，不再采用 Apache 协议。

## 版权与许可注意事项

- 上游（Atomix / Camunda）原有的版权声明必须保留，请勿修改或删除原文件中的 copyright header。
- 新增文件的适用协议以各文件自身的 license header 与版权声明为准。
