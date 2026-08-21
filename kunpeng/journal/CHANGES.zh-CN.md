# kunpeng-journal 更改说明

[English](./CHANGES.md)

本模块的代码来源于 Camunda（Zeebe）的 journal（预写日志，write-ahead log）实现：

- 上游仓库：<https://github.com/camunda/camunda>
- 来源提交：<https://github.com/camunda/camunda/commit/a8fb2a5868e54f118a085bc688338c49950adf0c>（2026-08-19 取自主分支）
- 上游仓库的 fork 存档：<https://github.com/anyilanxin/camunda/commit/a8fb2a5868e54f118a085bc688338c49950adf0c>

## 上游背景

本模块包含的是 Zeebe 使用的分段 journal（预写日志）实现（包名 `io.camunda.zeebe.journal`），用于在
状态变更被应用之前，将其持久化到磁盘。

## 本仓库的更改

本次更改的主要目的是：**解决协议冲突**，以及**方便自己维护和迭代**。在上游代码的基础上，本仓库做了如下调整：

1. **删除了所有非 Apache-2.0 协议的代码**（例如以 Camunda License 1.0 协议发布的代码），以解决协议冲突。本模块内剩余代码均为 Apache-2.0 许可（详见各自文件的 license header）。
2. **只保留真正需要的代码**，让代码库保持精简、便于维护和迭代。
3. 构建方式由 Maven 迁移至 Gradle，并合入本仓库统一构建。
4. **后续新增的文件，其适用协议以文件自身的 license header 与版权声明为准**，不再采用 Apache 协议。

## 版权与许可注意事项

- 上游（Camunda）原有的版权声明必须保留，请勿修改或删除原文件中的 copyright header。
- 新增文件的适用协议以各文件自身的 license header 与版权声明为准。
