# 02 — Property 字段类型

所有 Property 类都在 `com.anyilanxin.kunpeng.structpack.property` 包下，构造器**首参一律为 id**（强制，1..127；新字段可省略 id 由构建任务自动插入）。选型表：

| Java 类型 | Property 类型 | 示例 |
|----------|--------------|------|
| `long` / `Long` | `LongProperty` | `new LongProperty(1, "ID", -1)` |
| `int` / `Integer` | `IntegerProperty` | `new IntegerProperty(2, "VERSION", 0)` |
| `short` / `Short` | `ShortProperty` | `new ShortProperty(3, "FLAG", (short) -1)` |
| `boolean` / `Boolean` | `BooleanProperty` | `new BooleanProperty(4, "SUSPENSION", true)` |
| `String` | `StringProperty` | `new StringProperty(5, "NAME", "")` |
| `byte[]` (二进制 blob) | `BinaryProperty` | `new BinaryProperty(6, "RESOURCE", new UnsafeBuffer())` |
| 枚举 | `EnumProperty<E>` | `new EnumProperty<>(7, "STATE", State.class, State.NULL_VAL)` |
| 嵌套对象（DocumentProperty / Map） | `DocumentProperty` | `new DocumentProperty(8, "VARIABLES")` |
| 嵌套其他 Record / UnpackedObject | `ObjectProperty<T>` | `new ObjectProperty<>(9, "RECORD", new XxxRecord())` |
| `List<String>` / `Set<String>` | `ArrayProperty<StringValue>` | `new ArrayProperty<>(10, "GROUPS", StringValue::new)` |
| `List<Integer>` | `ArrayProperty<IntegerValue>` | `new ArrayProperty<>(11, "INDEXES", IntegerValue::new)` |
| `List<XxxRecord>` | `ArrayProperty<XxxRecord>` | `new ArrayProperty<>(12, "ITEMS", XxxRecord::new)` |
| `Map<String, Object>` | `DocumentProperty` | 配合 `DocumentUtil.convertToMap` / `convertToMsgPack` |

## LongProperty / IntegerProperty / ShortProperty

最简单的标量：

```java
private final LongProperty idProp = new LongProperty(1, "ID", -1);

public long getId() {
  return idProp.getValue();
}

public XxxRecord setId(long id) {
  idProp.setValue(id);
  return this;
}
```

## BooleanProperty

```java
private final BooleanProperty suspensionProp = new BooleanProperty(2, "SUSPENSION", true);

public boolean isSuspension() {
  return suspensionProp.getValue();
}

public XxxRecord setSuspension(boolean suspension) {
  suspensionProp.setValue(suspension);
  return this;
}
```

⚠️ getter 名按 Java Bean 规范用 `isXxx()` 而不是 `getXxx()`。

## StringProperty

字符串底层是 `DirectBuffer`，可以零拷贝访问：

```java
private final StringProperty nameProp = new StringProperty(3, "NAME", "");

@Override
public String getName() {
  return bufferAsString(nameProp.getValue());
}

@JsonIgnore
public DirectBuffer getNameBuffer() {
  return nameProp.getValue();
}

public XxxRecord setName(String name) {
  if (name != null) {
    nameProp.setValue(wrapString(name));
  }
  return this;
}

public XxxRecord setName(DirectBuffer name) {
  nameProp.setValue(name);
  return this;
}
```

要点：

- String getter：`bufferAsString(prop.getValue())`（来自 `BufferUtil` 静态导入）
- Buffer getter：加 `@JsonIgnore`，避免 Jackson 序列化时把 `DirectBuffer` 当成对象输出
- 两个 setter：`String` 版判空，`DirectBuffer` 版直接赋值

## BinaryProperty

类似 StringProperty，但底层是字节：

```java
private final BinaryProperty resourceProp = new BinaryProperty(4, "RESOURCE", new UnsafeBuffer());

@Override
public byte[] getResource() {
  return bufferAsArray(resourceProp.getValue());
}

@JsonIgnore
public DirectBuffer getResourceBuffer() {
  return resourceProp.getValue();
}

public XxxRecord setResource(byte[] resource) {
  resourceProp.setValue(BufferUtil.wrapArray(resource));
  return this;
}

public XxxRecord setResource(DirectBuffer resource, int offset, int length) {
  resourceProp.setValue(resource, offset, length);
  return this;
}
```

⚠️ DirectBuffer 版 setter **必须是 3 参**（buffer + offset + length），没有 1 参的 DirectBuffer 重载。调用方需要：

```java
final DirectBuffer buf = source.getBuffer();
record.setXxx(buf, 0, buf.capacity());
```

## EnumProperty

```java
private final EnumProperty<State> stateProp =
    new EnumProperty<>(5, "STATE", State.class, State.NULL_VAL);

public State getState() {
  return stateProp.getValue();
}

public XxxRecord setState(State state) {
  stateProp.setValue(state);
  return this;
}
```

要求枚举类有 `value()` 方法（实现 `AdminValueLifeCycle`）或用默认序号。NULL_VAL 作为默认值防止反序列化时 NPE。

## ArrayProperty — 集合

详细见 [05-集合字段与-addXxx.md](./05-collections-and-addxxx.md)。

最简单形式：

```java
private final ArrayProperty<StringValue> groupsProp =
    new ArrayProperty<>(6, "GROUPS", StringValue::new);

public Set<String> getGroups() {
  final Set<String> set = new HashSet<>();
  if (groupsProp.hasValue()) {
    for (final StringValue v : groupsProp) {
      set.add(bufferAsString(v.getValue()));
    }
  }
  return set;
}

public XxxRecord setGroups(Set<String> groups) {
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

## DocumentProperty — Map / 动态结构

```java
private final DocumentProperty variablesProp = new DocumentProperty(7, "VARIABLES");

@Override
public Map<String, Object> getVariables() {
  return convertToMap(variablesProp.getValue());
}

public XxxRecord setVariables(Map<String, Object> variables) {
  variablesProp.setValue(wrapArray(convertToMsgPack(variables)));
  return this;
}
```

`convertToMap` / `convertToMsgPack` 来自 `com.anyilanxin.kunpeng.structpack.util.DocumentUtil`（静态导入）；document 内容是标准 msgpack 字节，与存量变量、网关 API 完全兼容。

## ObjectProperty — 嵌套其他 Record

```java
private final ObjectProperty<UnifiedRecordValue> innerProp =
    new ObjectProperty<>(8, "INNER", new UnifiedRecordValue(10));
```

不太常用，多数场景用 ArrayProperty 持有嵌套 Record 即可。
