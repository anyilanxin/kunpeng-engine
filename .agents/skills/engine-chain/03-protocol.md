# 03 协议层（protocol-business + protocol-business-impl）

## 本端职责与边界

**职责（本端 owns）**：

- **类型身份**：`ValueType` 类型码——一个业务域一个码，API 型（`XXX_API`）与域型（`XXX`）成对
- **状态身份**：`CommandApiXxxValueLifeCycle` 的 value（协议身份码，不可重排）、processIndex（处理器分发槽位）、recordIndex（Record 元数据索引）
- **载体定义**：`XxxRequestRecord`/`XxxResponseRecord`（structpack key-id 自描述帧，字段=协议）
- **反序列化注册**：`XxxApiRecordRegister` → `RecordValueMapper`——broker 入口 `getValue(lifeCycle)` 与网关响应解码都依赖，全链必经
- **编号池与容量**：`RecordProcessIndex`/`RecordMappingIndex` 顺延 + `size()` 同步（处理器数组容量）
- **全局枚举接入**：`INTENT_CLASSES` 注册 + `fromProtocolValue` 解析

**边界（不做）**：

- ❌ 无任何业务逻辑——本层是纯身份定义，没有运行时行为
- ❌ 不定义传输方式（传输在 broker/gateway；序列化格式在 structpack）
- ❌ 不做兼容兜底——字段/编号一旦变更就是协议变更，必须先走 `enum-standards`/`structpack` 评估，不悄悄改

**输入 → 输出**：无运行时输入输出；产出是被 ②④⑤⑥ 各端引用的"身份字典"

> 定位一句话：协议层是全链的**户籍科**——谁是谁、编号多少、住哪个槽位，全在这里登记；其他层只来查户口。

## ① ValueType 加 API 类型码

`kunpeng/protocol/protocol-business/src/main/java/com/anyilanxin/kunpeng/protocol/business/ValueType.java`

```java
EXPRESSION_API((short) 53),   // 域名 + _API 后缀；编号顺延取当前最大+1，禁止插空、禁止重排
```

- **API 类型**（如 `EXPRESSION_API`）承载外部命令请求/响应；**域类型**（如 `PROCESS_INSTANCE`）承载引擎内部状态机 Record，两者成对出现（持久化型命令才需要域类型）
- 已有编号空缺（如 33/34/37 等）是历史裁剪，**不要复用空位**，一律从尾部顺延

## ② CommandApi LifeCycle（API 生命周期）

```
kunpeng/protocol/protocol-business/src/main/java/com/anyilanxin/kunpeng/protocol/business/record/commandapi/record/xxx/CommandApiXxxValueLifeCycle.java
```

```java
public enum CommandApiExpressionValueLifeCycle implements CommandApiValueLifeCycle {
  EVALUATE_REQUEST((short) 0, PROCESS_INDEX_227, RECORD_INDEX_85),   // 请求态：占 processIndex（处理器分发槽位）
  EVALUATE_RESPONSE((short) 1, NOT_PROCESS_INDEX, RECORD_INDEX_86);  // 响应态：不进处理器，只占 recordIndex

  // 必备成员：value()/processIndex()/recordIndex()/getValueType()/from(short)
  // from() 的 default 分支 throw new IllegalStateException("Unexpected value: " + value)
}
```

- 枚举内 `value` 从 0 顺延；**同一 ValueType 下的 value 码不重排**（协议身份）
- `PROCESS_INDEX_*` 用于 `LogEventProcessors` 数组分发（引擎按它找处理器）；`RECORD_INDEX_*` 用于 Record 元数据索引
- 多状态 API（请求/响应/分页等）都放同一个枚举里

## ③ 编号池顺延 + size() 同步（易漏，最高危）

```
kunpeng/protocol/protocol-business/src/main/java/com/anyilanxin/kunpeng/protocol/business/RecordProcessIndex.java
kunpeng/protocol/protocol-business/src/main/java/com/anyilanxin/kunpeng/protocol/business/RecordMappingIndex.java
```

```java
short PROCESS_INDEX_227 = 227;          // 尾部顺延
static short size() { return PROCESS_INDEX_227 + 1; }   // ★ 必须同步改，处理器数组按 size() 分配
```

**忘记改 `size()` = 新处理器槽位越界/注册不进**。申请编号的完整规则（连续性、破坏性评估）走 `enum-standards`，重排编号池属于破坏性变更必须先出方案。

## ④ Record 载体

```
kunpeng/protocol/protocol-business-impl/src/main/java/com/anyilanxin/kunpeng/protocol/business/impl/record/commandapi/xxx/
├── XxxRequestRecord.java     # extends UnifiedRecordValue，实现 RequestRecordValue 语义
└── XxxResponseRecord.java    # extends UnifiedRecordValue，响应负载
```

- structpack key-id 自描述帧：新字段用全新 key，不复用不改既有（`structpack` 强规则）
- 变量/大文档字段用 DirectBuffer 载体（`getVariablesBuffer()` 风格），由 structpack document 处理
- 类骨架、Property 声明、getter/setter/addXxx、wrap/unwrap 见 `record-standards`

## ⑤ RecordRegister（反序列化注册，全链必经）

```
kunpeng/protocol/protocol-business-impl/src/main/java/com/anyilanxin/kunpeng/protocol/business/impl/record/commandapi/xxx/XxxApiRecordRegister.java
```

```java
public class ExpressionApiRecordRegister {
  public static void register(final RecordValueMapperRegister valueMapper) {
    valueMapper
        .register(CommandApiExpressionValueLifeCycle.EVALUATE_REQUEST, EvaluateExpressionRequestRecord::new)
        .register(CommandApiExpressionValueLifeCycle.EVALUATE_RESPONSE, EvaluateExpressionResponseRecord::new);
  }
}
```

挂入总注册器 `commandapi/CommandApiRecordRegister.register()`（加一行）。

**为什么致命**：broker 入口 `CommandApiHandleImpl.writeCommand` 与响应解码 `BrokerResponse` 都靠 `DefaultRecordValueMapper.getValue(lifeCycle)` 拿到空 Record 实例再 `wrap(data)`——没注册这步，请求在 broker 入口直接异常。

## ⑥ ValueLifeCycle 全局注册（两处）

`kunpeng/protocol/protocol-business/src/main/java/com/anyilanxin/kunpeng/protocol/business/ValueLifeCycle.java`

1. `INTENT_CLASSES` 列表尾部追加 `CommandApiXxxValueLifeCycle.class`（供 maxCardinality 等全局扫描）
2. `fromProtocolValue(ValueType, state)` switch 加 case：

```java
case EXPRESSION_API -> CommandApiExpressionValueLifeCycle.from(state);
```

漏了 → 反序列化 `IllegalStateException: Illegal valueType`。

## 验证

```bash
./gradlew :kunpeng:protocol:protocol-business:compileJava :kunpeng:protocol:protocol-business-impl:compileJava
```

管理面（cluster-dispatch 用）有平行的 `protocol-admin(-impl)` 体系（`AdminValueLifeCycle`/`AdminApiRecordRegister`），业务命令不碰它；两套体系结构同构，改法一致。
