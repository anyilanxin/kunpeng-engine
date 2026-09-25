# 05 — 集合字段与 addXxx 模式

## 三种场景

集合字段在不同 Record 里有三种典型用法：

| 场景 | 内部存储 | 对外暴露 | 适用 |
|------|---------|---------|------|
| A. 单纯 List<String> / Set<String> | `ArrayProperty<StringValue>` | `getXxx()` 返回 `Set<String>` | 简单标量集合 |
| B. 嵌套 List<XxxRecordValue> | `ArrayProperty<XxxRecord>` | `getXxx()` 返回 `List<XxxRecordValue>` + `xxx()` 返回 ArrayProperty | 引擎处理器需要 `prop.add()` 直接追加 |
| C. Response Record 嵌套 | `ArrayProperty<XxxResponseRecord>` | `getXxx()` 返回 `List<XxxResponseRecordValue>` + `addXxx(commandRecord)` | 把命令侧 Record 转成响应侧 Record |
| D. Entity ↔ Record 互转 | `ArrayProperty<XxxEntity>` ↔ `ArrayProperty<XxxRecord>` | `xxx()` 原始访问器 + `wrap()` / `unwrap()` | 存储实体与协议 Record 之间的嵌套集合转换 |

## A. 标量集合

```java
private final ArrayProperty<StringValue> groupsProp =
    new ArrayProperty<>("CANDIDATE_STARTER_GROUPS", StringValue::new);

@Override
public Set<String> getCandidateStarterGroups() {
  final Set<String> set = new HashSet<>();
  if (groupsProp.hasValue()) {
    for (final StringValue v : groupsProp) {
      set.add(bufferAsString(v.getValue()));
    }
  }
  return set;
}

public XxxRecord setCandidateStarterGroups(Set<String> groups) {
  groupsProp.reset();
  if (groups == null) {
    return this;
  }
  for (final String g : groups) {
    groupsProp.add().wrap(wrapString(g));
  }
  return this;
}
```

## B. 嵌套 List<XxxRecord>，引擎直接追加

适用场景：命令侧 Record（如 `DeploymentRecord`、`ProcessDefinitionRecord`）需要引擎处理器直接 `prop.add()` 追加元素。

```java
private final ArrayProperty<ProcessDefinitionRecord> processDefinitionsProp =
    new ArrayProperty<>("PROCESS_DEFINITIONS", ProcessDefinitionRecord::new);

/** 流程定义列表(原始数组访问,引擎处理器使用) */
public ArrayProperty<ProcessDefinitionRecord> processDefinitions() {
  return processDefinitionsProp;
}

@Override
public List<ProcessDefinitionRecordValue> getProcessDefinitions() {
  final List<ProcessDefinitionRecordValue> list =
      new ArrayList<>(processDefinitionsProp.size());
  for (final ProcessDefinitionRecord record : processDefinitionsProp) {
    list.add(record);
  }
  return list;
}
```

调用方：

```java
final ProcessDefinitionRecord item = deploymentRecord.processDefinitions().add();
item.setProcessDefinitionId(...)
    .setProcessDefinitionKey(...)
    ...;
```

⚠️ 适用条件：
- ArrayProperty 元素类型 (`ProcessDefinitionRecord`) 与 List 泛型 (`ProcessDefinitionRecordValue`) **兼容**（前者 implements 后者）
- 引擎代码需要直接 `add()` 链式追加字段

## C. Response Record 的 addXxx 封装

适用场景：响应侧 Record 字段是命令侧 Record 的子集，需要**屏蔽字段差异**，对外只提供 `addXxx(commandRecord)` 方法。

### C 的两个特征

1. **不暴露** `xxx()` 原始 ArrayProperty 访问器
2. **提供** `addXxx(commandRecord)` 方法，内部完成字段复制

### 例子

`DeploymentCreateResponseRecord` 的 `addProcessDefinition(ProcessDefinitionRecord)`：

```java
private final ArrayProperty<DeploymentProcessDefinitionResponseRecord>
    processDefinitionsProp =
        new ArrayProperty<>(
            "PROCESS_DEFINITIONS", DeploymentProcessDefinitionResponseRecord::new);

public DeploymentCreateResponseRecord addProcessDefinition(
    final ProcessDefinitionRecord record) {
  final DirectBuffer checksumBuffer = record.getChecksumBuffer();
  processDefinitionsProp
      .add()
      .setProcessDefinitionId(record.getProcessDefinitionId())
      .setProcessDefinitionName(record.getProcessDefinitionNameBuffer())
      .setProcessDefinitionKey(record.getProcessDefinitionKeyBuffer())
      .setProcessDefinitionVersion(record.getProcessDefinitionVersion())
      .setSuspension(record.isSuspension())
      .setStartable(record.isStartable())
      .setHistoryTimeToLive(record.getHistoryTimeToLive())
      .setCandidateStarterGroups(record.getCandidateStarterGroups())
      .setCandidateStarterUsers(record.getCandidateStarterUsers())
      .setVersionTag(record.getVersionTagBuffer())
      .setResourceId(record.getResourceId())
      .setChecksum(checksumBuffer, 0, checksumBuffer.capacity());
  return this;
}

@Override
public List<DeploymentProcessDefinitionResponseRecordValue> getProcessDefinitions() {
  final List<DeploymentProcessDefinitionResponseRecordValue> list =
      new ArrayList<>(processDefinitionsProp.size());
  for (final DeploymentProcessDefinitionResponseRecord record : processDefinitionsProp) {
    list.add(record);
  }
  return list;
}
```

调用方：

```java
response.addProcessDefinition(processDefinitionRecord);   // 一行搞定
```

### addXxx 的字段复制原则

1. **先取 buffer**：`final DirectBuffer checksumBuffer = record.getChecksumBuffer();` 避免重复调用
2. **String 用 Buffer 变体**：`setXxx(record.getXxxBuffer())`，零拷贝
3. **Binary 用 3 参 DirectBuffer setter**：`setChecksum(buf, 0, buf.capacity())`
4. **不要漏字段**：对照 Response 接口和 Command 接口的字段，一一映射；Command 没有的字段就让 Response 默认值生效（不调 setter）
5. **类型不匹配**：不要做隐式转换，让编译器报错提醒

## D. ArrayProperty ↔ ArrayProperty 转换（wrap / unwrap）

适用场景：Entity ↔ Record 互转，例如 `ProcessDefinitionEntity.wrap(ProcessDefinitionRecord)` 与 `unwrap(...)`。两侧都是 `ArrayProperty<XxxEntity>` / `ArrayProperty<XxxRecord>`，元素类型不同但字段语义一致。

### 铁律：用原始 `xxx()` 遍历，不要用 `getXxx()`

`getXxx()` 会先 `new ArrayList<>(size)` 把 ArrayProperty 拷贝一遍再返回 —— 在 wrap/unwrap 热路径上纯属浪费。**必须**用原始 `xxx()` 拿 `ArrayProperty` 直接 for-each，零拷贝、零分配。

✅ 正确（直接遍历 ArrayProperty）：

```java
public void wrap(final ProcessDefinitionRecord record) {
  setProcessDefinitionId(record.getProcessDefinitionId())
      ...
      .setTenantId(record.getTenantIdBuffer());
  starterEventsProp.reset();
  for (final StarterEventRecord event : record.starterEvents()) {  // ← 原始访问器
    starterEventsProp
        .add()
        .setStartEventId(event.getStartEventId())
        .setStartEventName(event.getStartEventNameBuffer())
        .setActivityDefinitionKey(event.getActivityDefinitionKeyBuffer())
        .setType(event.getType());
  }
}
```

❌ 错误（多一次 ArrayList 分配 + 类型不一致还得强转）：

```java
for (final StarterEventRecordValue event : record.getStarterEvents()) {  // ← 多分配一个 List
  starterEventsProp.add()...                                              // ← 且 event 拿不到 Buffer 变体
}
```

### 字段复制原则

跟 C 的 `addXxx` 完全一致：

1. **String 走 Buffer 变体**：`setStartEventName(event.getStartEventNameBuffer())`，零拷贝
2. **Binary 用 3 参 setter**：`setChecksum(buf, 0, buf.capacity())`
3. **long / int / boolean / enum 直接 setValue**
4. **不要漏字段**：对照 Entity 字段和 Record 字段，一一映射；两侧字段定义必须保持同步

### 双向都要写

- **`wrap(record) → entity`**：链末尾 `prop.reset()`，然后 for-each `record.xxx()`，每个元素 `prop.add().setXxx(...)`
- **`unwrap(record) ← entity`**：开头先 `record.xxx().reset()`，for-each 自身的 `xxxProp`，每个元素 `record.xxx().add().setXxx(...)`

```java
public ProcessDefinitionRecord unwrap(final ProcessDefinitionRecord definitionRecord) {
  definitionRecord.reset();
  final ArrayProperty<StarterEventRecord> recordEvents = definitionRecord.starterEvents();
  recordEvents.reset();
  for (final StarterEventEntity event : starterEventsProp) {
    recordEvents
        .add()
        .setStartEventId(event.getStartEventId())
        .setStartEventName(event.getStartEventNameBuffer())
        .setActivityDefinitionKey(event.getActivityDefinitionKeyBuffer())
        .setType(event.getType());
  }
  return definitionRecord
      .setProcessDefinitionId(getProcessDefinitionId())
      ...
}
```

### 兜底：源类型没暴露 `xxx()` 时

如果对方 Record 是 A 场景的标量集合（只暴露 `getXxx()` 返回 `Set<String>` / `List<String>`），那只能用 `getXxx()` —— 但**仅限**这种标量场景。一旦元素是嵌套对象（Record / Entity），必须按 B 场景暴露 `xxx()` 原始访问器，再按本节方式转换。

## 何时选 B / 何时选 C

| 情况 | 选 |
|------|---|
| Response 字段 = Command 字段全集（同名同类型） | B（直接暴露 ArrayProperty） |
| Response 字段 = Command 字段子集（去掉部署/资源/tenantId 等） | C（用 addXxx 封装字段映射） |
| 引擎代码已经在 `prop.add().setXxx()` 这种风格里 | B |
| 引擎代码不在你这改，但希望 API 干净 | C |

## 接口 List 泛型与 ArrayProperty 类型不一致时的兜底

例如 `getResourceDefinitions(): List<ResourceDefinitionValue>`，但 ArrayProperty 元素是 `ResourceRecord`（不是 `ResourceDefinitionValue`），且不想/不能让 `ResourceRecord` implements `ResourceDefinitionValue`：

```java
@Override
public List<ResourceDefinitionValue> getResourceDefinitions() {
  return List.of();   // 占位，引擎代码通过别的方式访问
}
```

⚠️ 这种情况罕见，正常应该让内部存储类型与接口泛型兼容。如果遇到，请先确认接口设计和实际数据流是否对齐。
