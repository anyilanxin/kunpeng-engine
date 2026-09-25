# 04 — Getter 与 Setter

## 总规则

| 类型 | getter | setter |
|------|--------|--------|
| `long` / `int` / `short` | `@Override getXxx()` | `setXxx(long)` |
| `boolean` | `@Override isXxx()` | `setXxx(boolean)` |
| `String` | `@Override getXxx()` + `@JsonIgnore getXxxBuffer()` | `setXxx(String)` + `setXxx(DirectBuffer)` |
| `byte[]` (Binary) | `@Override getXxx()` + `@JsonIgnore getXxxBuffer()` | `setXxx(byte[])` + `setXxx(DirectBuffer, int, int)` |
| 枚举 | `@Override getXxx()` | `setXxx(EnumType)` |
| `Set<String>` | `@Override getXxx()` | `setXxx(Set<String>)` |
| `List<XxxRecord>` | `@Override getXxx()` 返回 `List<XxxRecordValue>` | 通常用 `addXxx(...)` 见 05 |

所有 setter 返回 `Self`（链式调用）。

## 标量 getter / setter

```java
@Override
public long getProcessDefinitionId() {
  return processDefinitionIdProp.getValue();
}

public ProcessDefinitionRecord setProcessDefinitionId(long processDefinitionId) {
  processDefinitionIdProp.setValue(processDefinitionId);
  return this;
}
```

```java
@Override
public boolean isSuspension() {
  return suspensionProp.getValue();
}

public ProcessDefinitionRecord setSuspension(boolean suspension) {
  suspensionProp.setValue(suspension);
  return this;
}
```

## String — 双 setter + Buffer getter

```java
@Override
public String getProcessDefinitionName() {
  return bufferAsString(processDefinitionNameProp.getValue());
}

@JsonIgnore
public DirectBuffer getProcessDefinitionNameBuffer() {
  return processDefinitionNameProp.getValue();
}

public ProcessDefinitionRecord setProcessDefinitionName(String processDefinitionName) {
  if (processDefinitionName != null) {
    processDefinitionNameProp.setValue(wrapString(processDefinitionName));
  }
  return this;
}

public ProcessDefinitionRecord setProcessDefinitionName(DirectBuffer processDefinitionName) {
  processDefinitionNameProp.setValue(processDefinitionName);
  return this;
}
```

规则：

- **String setter 判空**：`if (xxx != null)`，防止 null 把已有值覆盖成空字符串
- **DirectBuffer setter 不判空**：直接赋值
- **Buffer getter 加 `@JsonIgnore`**：避免 Jackson 序列化时报错或输出 DirectBuffer 的对象结构

## Binary (byte[]) — 双 setter + Buffer getter

```java
@Override
public byte[] getChecksum() {
  return bufferAsArray(checksumProp.getValue());
}

@JsonIgnore
public DirectBuffer getChecksumBuffer() {
  return checksumProp.getValue();
}

public ProcessDefinitionRecord setChecksum(byte[] checksum) {
  checksumProp.setValue(BufferUtil.wrapArray(checksum));
  return this;
}

public ProcessDefinitionRecord setChecksum(DirectBuffer checksum, int offset, int length) {
  checksumProp.setValue(checksum, offset, length);
  return this;
}
```

⚠️ DirectBuffer 版 setter **是 3 参**（buffer + offset + length），不要写成单参：

```java
// ❌ 错的：单参 DirectBuffer 没有对应重载
record.setChecksum(someDirectBuffer);

// ✅ 对的：3 参
final DirectBuffer buf = source.getChecksumBuffer();
record.setChecksum(buf, 0, buf.capacity());

// ✅ 也行：byte[] 重载
record.setChecksum(source.getChecksum());
```

## 枚举 getter / setter

```java
@Override
public ResourceType getResourceType() {
  return resourceTypeProp.getValue();
}

public ResourceDefinitionRecord setResourceType(ResourceType resourceType) {
  resourceTypeProp.setValue(resourceType);
  return this;
}
```

## Set<String> getter / setter

```java
@Override
public Set<String> getCandidateStarterGroups() {
  final Set<String> set = new HashSet<>();
  if (candidateGroupsProp.hasValue()) {
    for (final StringValue v : candidateGroupsProp) {
      set.add(bufferAsString(v.getValue()));
    }
  }
  return set;
}

public ProcessDefinitionRecord setCandidateStarterGroups(Set<String> candidateStarterGroups) {
  candidateGroupsProp.reset();
  if (candidateStarterGroups == null) {
    return this;
  }
  for (final String g : candidateStarterGroups) {
    candidateGroupsProp.add().wrap(wrapString(g));
  }
  return this;
}
```

要点：

- setter 第一步**先 reset**，再遍历 add（防止累加重复）
- 判空避免 NPE
- 遍历 ArrayProperty 用 `for (final StringValue v : prop)` 形式

## List<XxxRecordValue> getter（集合返回接口契约）

如果接口返回 `List<XxxRecordValue>`，而内部用 `ArrayProperty<XxxRecord>` 存储：

```java
@Override
public List<ProcessDefinitionRecordValue> getProcessDefinitions() {
  final List<ProcessDefinitionRecordValue> list = new ArrayList<>(processDefinitionsProp.size());
  for (final ProcessDefinitionRecord record : processDefinitionsProp) {
    list.add(record);
  }
  return list;
}
```

⚠️ 当 ArrayProperty 的元素类型与接口 List 泛型不一致时（如 `ResourceDefinitionRecord` vs `ResourceDefinitionValue`），见 [05-集合字段与-addXxx.md](./05-collections-and-addxxx.md) 的处理方式。

## 常见错误

| 错误 | 现象 | 修正 |
|------|------|------|
| 忘了 `@JsonIgnore` Buffer getter | Jackson 序列化报错或输出乱七八糟 | 加注解 |
| String setter 不判空 | null 参数把已有值清掉 | 加 `if (xxx != null)` |
| DirectBuffer 单参 setter | 编译报错 | 改成 3 参或 byte[] |
| Set setter 忘了 reset | 多次调用导致列表累加 | setter 开头先 `prop.reset()` |
| 接口要求 `List<Value>` 但 ArrayProperty 类型不匹配 | 编译报错或返回 `List.of()` | 见 05 章处理 |
