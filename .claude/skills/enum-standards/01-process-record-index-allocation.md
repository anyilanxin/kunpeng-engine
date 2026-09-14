# 07 — Process / Record Index 分配

涉及文件：

- `kunpeng/protocol/protocol/src/main/java/com/anyilanxin/kunpeng/protocol/RecordProcessIndex.java`
- `kunpeng/protocol/protocol/src/main/java/com/anyilanxin/kunpeng/protocol/RecordMappingIndex.java`

这两个接口是所有 `LifeCycle` / `State` 枚举里 `processIndex()` / `recordIndex()` 的取值池。改错地方会导致 Raft 日志反序列化错位，必须按下面两条规则维护。

> 枚举本身怎么写（`CommandValueLifeCycle` vs `CommandApiValueLifeCycle` 的契约差异、REQUEST/RESPONSE 配对等）见 [08-LifeCycle-枚举开发.md](./02-lifecycle-enum-development.md)。本章节只讲"编号池子"怎么维护。

## 两条铁律

### 铁律 1：全局连续 + 后缀等于值

```java
public interface RecordProcessIndex {
  short NOT_PROCESS_INDEX = -1;          // 唯一允许的负值，可被多处引用
  short PROCESS_INDEX_0 = 0;
  short PROCESS_INDEX_1 = 1;
  ...
  short PROCESS_INDEX_N = N;             // 后缀 N == 值 N
}
```

- 后缀和值**严格相等**，从 `0` 开始
- 中间**不允许有洞**（没有 `PROCESS_INDEX_5` 但有 `PROCESS_INDEX_6` 是错的）
- `RECORD_INDEX_*` 同理
- `NOT_PROCESS_INDEX` / `NOT_RECORD_INDEX` 是**唯一**可以多处引用的常量；其他每个 `PROCESS_INDEX_N` / `RECORD_INDEX_N` 都只能被一个枚举状态引用

### 铁律 2：每个枚举引用一段连续区间

某个 `LifeCycle` 枚举里出现的所有 `PROCESS_INDEX_N`，必须构成 `[a, b]` 一个连续闭区间；`RECORD_INDEX_N` 同理。换句话说：**任意两个相邻的 `PROCESS_INDEX_N` / `PROCESS_INDEX_(N+1)` 必定落在同一个枚举里**。

正确示例：

```java
public enum CommandApiDeploymentValueLifeCycle implements CommandApiValueLifeCycle {
  CREATE_REQUEST((short) 0, PROCESS_INDEX_0, RECORD_INDEX_0),
  CREATE_RESPONSE((short) 1, NOT_PROCESS_INDEX, RECORD_INDEX_1),
  DELETE_REQUEST((short) 2, PROCESS_INDEX_1, RECORD_INDEX_2),
  DELETE_RESPONSE((short) 3, NOT_PROCESS_INDEX, RECORD_INDEX_3);
}
// PROCESS: 0..1 连续；RECORD: 0..3 连续 ✓
```

错误示例（破坏铁律 2）：

```java
// ❌ JobLifeCycle 跳号：用了 137..142 和 145..148，中间 143/144 跑去别的枚举了
CREATING((short) 1, PROCESS_INDEX_137),
...
UPDATED((short) 6, PROCESS_INDEX_142),
REFUSING((short) 8, PROCESS_INDEX_145),  // 跳到 145，但 143/144 在别的枚举
```

## 增：加一个新的 LifeCycle 状态

1. 找到该枚举当前用的连续区间 `[a, b]`
2. 新状态拿 `b+1`，并在 `AdminRecordProcessIndex` / `AdminRecordMappingIndex` 末尾追加：
   ```java
   short PROCESS_INDEX_(b+1) = b+1;
   ```
3. 如果新状态是 response 类（`NOT_PROCESS_INDEX`），只追加 `RECORD_INDEX_*`
4. 别忘了更新接口末尾的 `size()`：
   ```java
   static short size() {
     return PROCESS_INDEX_<最后一个> + 1;
   }
   ```

## 删：去掉一个未使用的索引

当某个枚举状态被移除，会留下一个无人引用的 `PROCESS_INDEX_N` / `RECORD_INDEX_N`。必须**全部重排**，不能留洞：

1. 全局 grep 出所有引用，确认哪些编号真的没人用
2. 按当前编号顺序，从 0 重新分配（保留原顺序，只压缩空位）
3. 同步改所有引用该常量的 LifeCycle 文件
4. 重新生成两个接口文件的常量列表
5. 跑一遍 [验证脚本](#验证脚本) 确认两条铁律都成立

推荐用一次性 Python 脚本而不是手工 sed，因为单文件内 sed 容易把 `PROCESS_INDEX_1` 误替换成 `PROCESS_INDEX_10` 的一部分。脚本要用 `\bPROCESS_INDEX_(\d+)\b` 这种带词边界的正则，且**单次扫描**完成（不要先替换再扫描第二遍）。

## 验证脚本

把这段存成 `/tmp/verify_contiguous.py`，每次改完跑一遍：

```python
#!/usr/bin/env python3
import os, re
ROOT = "kunpeng/protocol/protocol/src/main/java/com/anyilanxin/kunpeng/protocol/record"
proc_re = re.compile(r"\bPROCESS_INDEX_(\d+)\b")
rec_re  = re.compile(r"\bRECORD_INDEX_(\d+)\b")
problems = 0
for dirpath, _, files in os.walk(ROOT):
    for fn in files:
        if not fn.endswith(".java"):
            continue
        content = open(os.path.join(dirpath, fn)).read()
        for tag, rx in (("PROCESS", proc_re), ("RECORD", rec_re)):
            vals = sorted(set(int(m.group(1)) for m in rx.finditer(content)))
            if not vals:
                continue
            ok = vals == list(range(vals[0], vals[-1] + 1))
            problems += not ok
            print(f"{'OK' if ok else 'GAP'} {fn:60s} {tag}: {vals[0]}..{vals[-1]} ({len(vals)})")
print(f"\nproblems: {problems}")
```

输出应该全是 `OK`，`problems: 0`。

## 当前分配快照（2026-08-06）

`PROCESS_INDEX_0..186`（共 187 个），`RECORD_INDEX_0..76`（共 77 个）。各枚举的连续区间：

| 枚举 | PROCESS | RECORD |
|---|---|---|
| CommandApiDeploymentValueLifeCycle | 0–1 | 0–3 |
| DeploymentLifeCycle | 2–11 | 4 |
| CommandApiProcessDefinitionValueLifeCycle | 12–14 | 5–10 |
| ProcessDefinitionLifeCycle | 15–33 | 11 |
| DecisionDefinitionLifeCycle | 34–35 | 12 |
| DecisionRequirementDefinitionLifeCycle | 36–37 | 13 |
| FormDefinitionState | 38–39 | 14 |
| CommandApiProcessInstanceValueLifeCycle | 40–42 | 15–20 |
| ProcessInstanceLifeCycle | 43–54 | 21 |
| ProcessInstanceBatchState | 55–62 | 22 |
| DistributeLifeCycle | 63–73 | 23 |
| ActivityInstanceLifeCycle | 74–88 | 24 |
| CommandApiVariableValueLifeCycle | 89–91 | 25–30 |
| VariableLifeCycle | 92–96 | 31 |
| CommandApiUserTaskValueLifeCycle | 97–105 | 32–49 |
| UserTaskLifeCycle | 106–125 | 50 |
| CommandApiIncidentValueLifeCycle | 126 | 51–52 |
| IncidentLifeCycle | 127–132 | 53 |
| AsyncRequestState | 133–134 | 54 |
| CommandApiJobValueLifeCycle | 135–136 | 57–60 |
| JobLifeCycle | 137–146 | 61 |
| CommandApiJobBatchValueLifeCycle | 147 | 62–63 |
| JobBatchLifeCycle | 148–149 | 64 |
| CommandApiMessageValueLifeCycle | 150 | 65–66 |
| MessageSubscriptionLifeCycle | 151–158 | 67 |
| SignalSubscriptionLifeCycle | 159–166 | 56 |
| TimerLifeCycle | 167–172 | 68 |
| BusinessValueRouteLifeCycle | 173–176 | 69 |
| DelayLifeCycle | 177–178 | 70 |
| HistoryCleanupLifeCycle | 179–182 | 71 |
| BusinessRouteApiLifeCycle | 183–184 | 72–75 |
| ResourceDefinitionLifeCycle | 185–186 | 76 |
| CommandApiEmptyValueLifeCycle | — | 55 |

> 注意：`RECORD_INDEX_56`（SignalSubscriptionLifeCycle）和 `RECORD_INDEX_55`（CommandApiEmptyValueLifeCycle）的位置看起来"错位"，但每个枚举内部仍是连续的（单值），不违反铁律 2。如果未来要做更严格的"按模块分块对齐"再统一重排。

## 反例（不要做）

```java
// ❌ 后缀和值不一致
short PROCESS_INDEX_0 = 1;

// ❌ 留洞
short PROCESS_INDEX_5 = 5;
short PROCESS_INDEX_7 = 7;   // 6 去哪了？

// ❌ 跨枚举跳号：同一个枚举内部不连续
// Enum A: PROCESS_INDEX_10, PROCESS_INDEX_11
// Enum B: PROCESS_INDEX_12
// Enum A: PROCESS_INDEX_13   ← A 被劈成两段

// ❌ 用字面量代替常量
CREATING((short) 1, (short) 5),  // 5 是哪个 PROCESS_INDEX_？没人知道

// ❌ 把 NOT_PROCESS_INDEX 当成可回收的普通编号重新分配
//    NOT_PROCESS_INDEX 永远是 -1，永远表示"这个状态不进 process 队列"
```

## 编译验证

```bash
./gradlew :kunpeng:protocol:protocol:compileJava --no-daemon
```

`BUILD SUCCESSFUL` + 验证脚本 `problems: 0` = 收工。
