# 08 — LifeCycle 枚举开发

所有 `*LifeCycle` / `*State` 枚举都实现 `com.anyilanxin.kunpeng.protocol.ValueLifeCycle`，按"业务命令"还是"对外 API"分两种 archetype，**契约不一样，不能混着写**。

> 索引编号（`PROCESS_INDEX_*` / `RECORD_INDEX_*`）的池子维护规则见 [07-Process-Record-Index-分配.md](./01-process-record-index-allocation.md)。本章节只讲枚举本身怎么写。

## 两种 archetype

| 维度 | `CommandValueLifeCycle`（业务命令） | `CommandApiValueLifeCycle`（对外 API） |
|------|-----------------------------------|-------------------------------------|
| 包路径 | `record.command.*` | `record.commandapi.record.*` |
| `isState()` | 覆盖全部常量的 switch 逐个标注 | `false`（接口默认，API 层永远不是状态） |
| `NULL_VAL` | **必须**有，`(short) -1, NOT_PROCESS_INDEX` | **不要**有 |
| 枚举数量 | N 个业务状态 + 1 个 `NULL_VAL` | 2 × M 个，`*_REQUEST` / `*_RESPONSE` 成对 |
| `PROCESS_INDEX` | 除 `NULL_VAL` 外，每个状态都引用一个真实 `PROCESS_INDEX_*` | 只有 `*_REQUEST` 引用，`*_RESPONSE` 一律 `NOT_PROCESS_INDEX` |
| `RECORD_INDEX` | **整文件共用一个**，由 `recordIndex()` 返回 | 每个枚举值各引用一个 `RECORD_INDEX_*`，存字段 |
| 构造器参数 | `(short value, short processIndex)` | `(short value, short processIndex, short recordIndex)` |
| `recordIndex()` 实现 | `return RECORD_INDEX_N;`（常量） | `return recordIndex;`（字段） |
| 示例 | `DeploymentLifeCycle` / `ProcessInstanceLifeCycle` | `BusinessDispatchApiValueLifeCycle` / `CommandApiJobValueLifeCycle` |

## Archetype A — `CommandValueLifeCycle`

适用：流程引擎内部业务状态（被引擎消费、走 Raft 日志、生成 event）。

### 模板

```java
package com.anyilanxin.kunpeng.protocol.dispatch.record.command.xxx;

import static com.anyilanxin.kunpeng.protocol.dispatch.RecordMappingIndex.RECORD_INDEX_<N>;
import static com.anyilanxin.kunpeng.protocol.dispatch.RecordProcessIndex.*;

import com.anyilanxin.kunpeng.protocol.dispatch.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.dispatch.ValueType;

public enum XxxLifeCycle implements ValueLifeCycle {
    NULL_VAL((short) -1, NOT_PROCESS_INDEX),

    CREATING((short) 1, PROCESS_INDEX_ < a >),
    CREATED((short) 2, PROCESS_INDEX_ < a + 1 >),
    DELETING((short) 3, PROCESS_INDEX_ < a + 2 >),
    DELETED((short) 4, PROCESS_INDEX_ < a + 3 >);

    private final short value;
    private final short processIndex;

    XxxLifeCycle(final short value, final short processIndex) {
        this.value = value;
        this.processIndex = processIndex;
    }

    public short getValueState() {
        return value;
    }

    public static ValueLifeCycle from(final short value) {
        return switch (value) {
            case 1 -> CREATING;
            case 2 -> CREATED;
            case 3 -> DELETING;
            case 4 -> DELETED;
            default -> UNKNOWN;
        };
    }

    @Override
    public short value() {
        return value;
    }

    @Override
    public boolean isState() {
        // 状态 = AdminApplier.lifeCycle() 引用的常量；switch 必须覆盖全部常量，不允许 default
        return switch (this) {
            case NULL_VAL, CREATING, DELETING -> false;
            case CREATED, DELETED -> true;
        };
    }

    @Override
    public short processIndex() {
        return processIndex;
    }

    @Override
    public ValueType getValueType() {
        return ValueType.XXX;
    }

    @Override
    public short recordIndex() {
        return RECORD_INDEX_ < N >;     // 整个文件只引用一个 RECORD_INDEX
    }
}
```

### 硬约束

1. **必须**有 `NULL_VAL((short) -1, NOT_PROCESS_INDEX)` 作为第一个枚举常量，表示"未设置"
2. 除 `NULL_VAL` 外，**每个**枚举值必须引用一个真实的 `PROCESS_INDEX_*`（不能是 `NOT_PROCESS_INDEX`）
3. **整个文件共用一个 `RECORD_INDEX_*`**，由 `recordIndex()` 直接 `return`；不要给每个状态分配一个
4. `PROCESS_INDEX_*` 的编号在文件内必须连续（见 [07](./01-process-record-index-allocation.md) 铁律 2）
5. `value()` 与 `processIndex()` 是**两个独立维度**：value 是协议字段值（可能有跳号），processIndex 是 Raft 处理槽位（必须连续）。不要为了对齐 value 而在 processIndex 里留洞
6. `isState()` 必须用**覆盖全部常量的 switch** 标注（编译期穷尽检查）；禁止用字段存布尔值，禁止写 `default` 分支
7. **状态判定依据**：`isState() == true` 的常量必须与 admin-repository 中 `Applier.lifeCycle()` 返回的常量一一对应——新增状态常量要同步新增 Applier，删状态要删 Applier
8. **注册口校验**：命令只准走 `LogEventProcessors.onCommand`（拒绝状态），状态只准走 `RecordApplierMap.put`（拒绝非状态），底层注册方法会校验 `isState()`
9. **状态只存在于 Command archetype**；`from(short)` 的 default 分支 `return UNKNOWN;`（不是抛异常）

## Archetype B — `CommandApiValueLifeCycle`

适用：gateway 对外暴露的请求/响应状态（不走 Raft，只用于响应序列化）。

### 模板

```java
package com.anyilanxin.kunpeng.protocol.dispatch.record.commandapi.record.xxx;

import com.anyilanxin.kunpeng.protocol.dispatch.ValueType;
import com.anyilanxin.kunpeng.protocol.dispatch.record.commandapi.CommandApiValueLifeCycle;

import static com.anyilanxin.kunpeng.protocol.dispatch.RecordMappingIndex.*;
import static com.anyilanxin.kunpeng.protocol.dispatch.RecordProcessIndex.*;

public enum CommandApiXxxValueLifeCycle implements CommandApiValueLifeCycle {
    CREATE_REQUEST((short) 0, PROCESS_INDEX_ < a >, RECORD_INDEX_ < b >),
    CREATE_RESPONSE((short) 1, NOT_PROCESS_INDEX, RECORD_INDEX_ < b + 1 >),
    DELETE_REQUEST((short) 2, PROCESS_INDEX_ < a + 1 >, RECORD_INDEX_ < b + 2 >),
    DELETE_RESPONSE((short) 3, NOT_PROCESS_INDEX, RECORD_INDEX_ < b + 3 >);

    private final short value;
    private final short processIndex;
    private final short recordIndex;

    CommandApiXxxValueLifeCycle(
            final short value, final short processIndex, final short recordIndex) {
        this.value = value;
        this.processIndex = processIndex;
        this.recordIndex = recordIndex;
    }

    public short getValueState() {
        return value;
    }

    public static CommandApiValueLifeCycle from(final short value) {
        return switch (value) {
            case 0 -> CREATE_REQUEST;
            case 1 -> CREATE_RESPONSE;
            case 2 -> DELETE_REQUEST;
            case 3 -> DELETE_RESPONSE;
            default -> throw new IllegalStateException("Unexpected value: " + value);
        };
    }

    @Override
    public short value() {
        return value;
    }

    @Override
    public short processIndex() {
        return processIndex;
    }

    @Override
    public ValueType getValueType() {
        return ValueType.XXX_API;
    }

    @Override
    public short recordIndex() {
        return recordIndex;            // 字段，不是常量
    }
}
```

### 硬约束

1. **不要**写 `NULL_VAL`（API 层不需要"未设置"语义）
2. 枚举值**必须成对**出现：`XXX_REQUEST` 紧跟 `XXX_RESPONSE`，命名严格对齐
3. `value` 编号必须成对连续：`REQUEST` 用偶数（0/2/4/...），`RESPONSE` 用奇数（1/3/5/...）
4. `*_REQUEST` 一律引用真实 `PROCESS_INDEX_*`；`*_RESPONSE` 一律 `NOT_PROCESS_INDEX`
5. **每个枚举值各引用一个独立的 `RECORD_INDEX_*`**（存进字段，`recordIndex()` 返回字段）——这是和 Archetype A 最大的区别
6. `PROCESS_INDEX_*` 在文件内连续；`RECORD_INDEX_*` 也在文件内连续（两个独立区间，互不干扰）
7. `isState()` 不要 override（接口已经 default `false`——API 层永远不是状态，状态只存在于 Command archetype）
8. `from(short)` 的 default 分支**抛 `IllegalStateException`**（不是返回 `UNKNOWN`，因为 API 层收到未知值就是协议错）

### REQUEST / RESPONSE 配对表

写新枚举时按下表对齐：

| REQUEST 名 | RESPONSE 名 | value | processIndex | recordIndex |
|-----------|------------|-------|--------------|-------------|
| `CREATE_REQUEST` | `CREATE_RESPONSE` | 0 / 1 | `PROCESS_INDEX_<a>` / `NOT_PROCESS_INDEX` | `RECORD_INDEX_<b>` / `RECORD_INDEX_<b+1>` |
| `DELETE_REQUEST` | `DELETE_RESPONSE` | 2 / 3 | `PROCESS_INDEX_<a+1>` / `NOT_PROCESS_INDEX` | `RECORD_INDEX_<b+2>` / `RECORD_INDEX_<b+3>` |
| `UPDATE_REQUEST` | `UPDATE_RESPONSE` | 4 / 5 | `PROCESS_INDEX_<a+2>` / `NOT_PROCESS_INDEX` | `RECORD_INDEX_<b+4>` / `RECORD_INDEX_<b+5>` |
| `CANCEL_REQUEST` | `CANCEL_RESPONSE` | 6 / 7 | `PROCESS_INDEX_<a+3>` / `NOT_PROCESS_INDEX` | `RECORD_INDEX_<b+6>` / `RECORD_INDEX_<b+7>` |

> 加一个 REQUEST/RESPONSE 对，就同时申请一个新的 `PROCESS_INDEX_*` 和**两个**新的 `RECORD_INDEX_*`。

## 与对应 Record 类的命名映射

`CommandApiValueLifeCycle` 总是和某个 `CommandValueLifeCycle` 对应（例如 `BusinessDispatchApiValueLifeCycle` ↔ `DeploymentLifeCycle`）。新增 API 枚举时：

1. 先确认对应的业务 LifeCycle 已存在
2. `getValueType()` 返回 `ValueType.<XXX>_API`（业务那个是 `ValueType.<XXX>`）
3. `value()` 字段允许和业务那个**完全不同**：API 用 0/1/2/3 的请求响应编号，业务用 1/2/3... 的状态编号
4. `PROCESS_INDEX_*` 区间**不要重叠**——两套是各自独立的池子消费方

## 反例（不要做）

```java
// ❌ CommandValueLifeCycle 里给每个状态分配 RECORD_INDEX
CREATING((short) 1,PROCESS_INDEX_74,RECORD_INDEX_24),

CREATED((short) 2,PROCESS_INDEX_75,RECORD_INDEX_25);
// recordIndex() 应该是文件级单值，不是状态级

// ❌ CommandValueLifeCycle 漏 NULL_VAL
public enum XxxLifeCycle implements ValueLifeCycle {
    CREATING((short) 1, PROCESS_INDEX_74),  // 第一个就直接业务状态
  ...
}

// ❌ CommandValueLifeCycle 在 NULL_VAL 之外还出现 NOT_PROCESS_INDEX
DELETED((short) 4,NOT_PROCESS_INDEX)  // 业务状态不进 process 队列没意义

// ❌ CommandApiValueLifeCycle 出现不成对的 REQUEST
CORRELATION_REQUEST((short) 0,PROCESS_INDEX_150,RECORD_INDEX_65),

CORRELATION_RESPONSE((short) 1,NOT_PROCESS_INDEX,RECORD_INDEX_66);
// 只有一对是合法的，但如果加了 CORRELATE_REQUEST 就必须有 CORRELATE_RESPONSE

// ❌ CommandApiValueLifeCycle 的 RESPONSE 用了真实 PROCESS_INDEX
CREATE_RESPONSE((short) 1,PROCESS_INDEX_5,RECORD_INDEX_1)
// RESPONSE 永远 NOT_PROCESS_INDEX

// ❌ CommandApiValueLifeCycle 用 RECORD_INDEX 不连续
CREATE_REQUEST(RECORD_INDEX_0),

CREATE_RESPONSE(RECORD_INDEX_5)   // 中间 1/2/3/4 留洞

// ❌ CommandApiValueLifeCycle 的 from default 返回 UNKNOWN
default ->UNKNOWN;   // 应该抛 IllegalStateException

// ❌ CommandValueLifeCycle 的 from default 抛异常
default ->throw new

IllegalStateException(...);  // 应该 return UNKNOWN

// ❌ 用构造器字段存状态标注
CREATED((short) 2, PROCESS_INDEX_75, true)
// isState 用 switch 标注，不加第三个构造参数

// ❌ isState 的 switch 写 default 分支
default -> true;   // 新增常量时会静默漏判，必须穷尽列出全部常量

// ❌ 在 CommandApiValueLifeCycle 里 override isState() 返回 true
//    状态只存在于 Command archetype，API 层由接口默认 false

// ❌ 把 CommandApi 的 import 路径写到 command.* 包
package com.anyilanxin.kunpeng.protocol.dispatch.record.command.xxx;
// CommandApi 的枚举必须在 commandapi.record.* 包
```

## 新增枚举流程

1. 确认 archetype（看 ValueType：业务还是 API）
2. 复制上面相应模板
3. 按 [07](./01-process-record-index-allocation.md) 的"增"流程申请新的 `PROCESS_INDEX_*` / `RECORD_INDEX_*`
4. 把新枚举类加进 `ValueLifeCycle.INTENT_CLASSES` 列表
5. 在 `ValueLifeCycle.fromProtocolValue(...)` 的 switch 里加新分支
6. 跑验证脚本（见 [07](./01-process-record-index-allocation.md#验证脚本)）+ 编译

## 编译验证

```bash
./gradlew :kunpeng:protocol:protocol:compileJava --no-daemon
```

`BUILD SUCCESSFUL` + 验证脚本 `problems: 0` = 收工。
