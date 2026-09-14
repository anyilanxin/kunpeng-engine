---
name: enum-standards
description: Use when creating or modifying LifeCycle / State enums in kunpeng-engine (ValueLifeCycle, CommandValueLifeCycle, CommandApiValueLifeCycle) or allocating PROCESS_INDEX_* / RECORD_INDEX_* pool constants — adding LifeCycle states, adding REQUEST/RESPONSE pairs, or renumbering the index pools
---

# 枚举编写规范（LifeCycle / State 枚举）

所有 `*LifeCycle` / `*State` 枚举都实现 `com.anyilanxin.kunpeng.protocol.ValueLifeCycle`，分两种 archetype，**契约不一样，不能混着写**。`PROCESS_INDEX_*` / `RECORD_INDEX_*` 编号池有严格连续性规则：编号错会导致 Raft 日志反序列化错位。

## 何时用

- 新建 `*LifeCycle` / `*State` 枚举（业务命令 / 对外 API 两种 archetype）
- 增删 LifeCycle 状态
- 申请或重排 `PROCESS_INDEX_*` / `RECORD_INDEX_*` 常量
- 把新枚举接入 `ValueLifeCycle.INTENT_CLASSES` / `ValueLifeCycle.fromProtocolValue(...)`

## 速查

- **索引池**：`PROCESS_INDEX_N = N`，后缀==值==从 0 起全局连续、不留洞；`NOT_PROCESS_INDEX` / `NOT_RECORD_INDEX` 是唯一可多处引用的例外
- **索引引用**：每个枚举引用的 `PROCESS_INDEX_*` 与 `RECORD_INDEX_*` 在文件内必须各自连续
- **两类 archetype**：
  - `CommandValueLifeCycle`（业务命令）：必须有 `NULL_VAL`、文件级单 `recordIndex()` 常量、`isState()` 用覆盖全部常量的 switch 逐个标注、除 `NULL_VAL` 外每个常量引用真实 `PROCESS_INDEX_*`；**状态常量只允许出现在这类枚举**
  - `CommandApiValueLifeCycle`（对外 API）：成对 `*_REQUEST`/`*_RESPONSE`、状态级 `recordIndex` 字段、`*_RESPONSE` 一律 `NOT_PROCESS_INDEX`、不写 `NULL_VAL`、`isState()` 接口默认 `false`（API 层永远不是状态）
- **状态 vs 命令**：`isState() == true` 的常量是"状态"，判定依据 = admin-repository 中 `Applier.lifeCycle()` 返回的常量；新增/删除状态常量必须同步增删对应 Applier。命令注册（`LogEventProcessors.onCommand`）与状态注册（`RecordApplierMap.put`）在底层校验 `isState()`：命令口只收命令、状态口只收状态，注错即抛 `IllegalStateException`
- **value 与 processIndex 是两个独立维度**：value 是协议字段值（可跳号），processIndex 是 Raft 处理槽位（必须连续）。不要为了对齐 value 在 processIndex 里留洞

## 章节

| # | 文件 | 内容 | 何时看 |
|---|------|------|--------|
| 01 | [01-process-record-index-allocation.md](./01-process-record-index-allocation.md) | `RecordProcessIndex` / `RecordMappingIndex` 编号规则、增删流程、验证脚本 | 加/删 LifeCycle 状态时 |
| 02 | [02-lifecycle-enum-development.md](./02-lifecycle-enum-development.md) | `CommandValueLifeCycle` vs `CommandApiValueLifeCycle` 契约、模板、REQUEST/RESPONSE 配对 | 新建/修改 LifeCycle 枚举时 |

## 编译命令

```bash
./gradlew :kunpeng:protocol:protocol:compileJava --no-daemon
```

`BUILD SUCCESSFUL` + 验证脚本 `problems: 0`（见章节 01）= 收工。
