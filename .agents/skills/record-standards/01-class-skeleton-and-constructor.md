# 01 — 类骨架与构造器

## 类声明

```java
@AutoDeclareProperties
public class XxxRecord extends UnifiedRecordValue<XxxRecord>
    implements XxxRecordValue {
  // ...
}
```

要点：

- 必须 `@AutoDeclareProperties`（来自 `com.anyilanxin.kunpeng.structpack`），否则构建任务不生成 declareProperty 链
- 继承 `UnifiedRecordValue<SelfType>`，泛型填自己
- implements 接口契约 `XxxRecordValue`
- 不写 `serialVersionUID`、不写 `equals/hashCode`、不写 `toString`（父类 / Property 体系处理）

## 字段声明

所有 Property 字段都是 **`private final`**，命名以 `Prop` 结尾，构造器**首参为 id**（强制，1..127）：

```java
private final LongProperty processDefinitionIdProp =
    new LongProperty(1, PROCESS_DEFINITION_ID, -1);
private final StringProperty processDefinitionNameProp =
    new StringProperty(2, PROCESS_DEFINITION_NAME, "");
private final BinaryProperty checksumProp = new BinaryProperty(3, "CHECKSUM", new UnsafeBuffer());
```

- **id 规则**：同一 Record 内严禁重复；新字段可以**不写 id**（`new LongProperty(PROCESS_DEFINITION_ID, -1)`），构建任务从当前最大 id 之上分配（max+1 起步单调递增）插入首参
- 字段类型按 [02-Property-字段类型.md](./02-property-field-types.md) 选
- key 按 [03-Key-常量使用.md](./03-key-constants.md) 写
- 默认值尽量有语义：long 用 `-1`（表示未设置），String 用 `""`，boolean 看业务，集合用对应默认空值

## 构造器

```java
public XxxRecord() {
  super(N);                              // N = Property 字段总数
  declareProperty(field1Prop)
      .declareProperty(field2Prop)
      .declareProperty(field3Prop)
      ...;
}
```

⚠️ **关键约束**：

- `super(N)` 的数字**必须等于** `declareProperty(...)` 调用次数，否则要么容量浪费，要么 Property 装不下
- 一条 `declareProperty` 对应一个 Property 字段
- `declareProperty` 链式调用，但中间可以换行 / 缩进（构建任务生成的链带 `// formatting:off/on` 标记）

数 N 的简单办法：

```bash
grep -c "declareProperty" XxxRecord.java
```

## newRecord()

底部必须有：

```java
@Override
protected XxxRecord newRecord() {
  return new XxxRecord();
}
```

`UnifiedRecordValue` 内部做 copy / reset 时会用反射调用这个方法。少了它会导致序列化 / 反序列化失败。

## 完整骨架

```java
package com.anyilanxin.kunpeng.protocol.impl.record.xxx;

import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.*;

import com.anyilanxin.kunpeng.protocol.dispatch.TenantOwned;
import com.anyilanxin.kunpeng.protocol.dispatch.UnifiedRecordValue;
import com.anyilanxin.kunpeng.protocol.dispatch.record.xxx.XxxRecordValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.property.LongProperty;
import com.anyilanxin.kunpeng.structpack.property.StringProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.agrona.DirectBuffer;

@AutoDeclareProperties
public class XxxRecord extends UnifiedRecordValue<XxxRecord>
  implements XxxRecordValue {

  private final LongProperty idProp = new LongProperty(1, "ID", -1);
  private final StringProperty nameProp = new StringProperty(2, "NAME", "");
  private final StringProperty tenantIdProp =
    new StringProperty(3, TENANT_ID, TenantOwned.DEFAULT_TENANT_IDENTIFIER);

  public XxxRecord() {
    super(3);
    declareProperty(idProp)
      .declareProperty(nameProp)
      .declareProperty(tenantIdProp);
  }

  // getters / setters 见 04 章节

  @Override
  protected XxxRecord newRecord() {
    return new XxxRecord();
  }
}
```

## 常见错误

| 错误 | 现象 |
|------|------|
| 忘了 `@AutoDeclareProperties` | 构建任务不生成 declareProperty 链，字段没注册 |
| `super(N)` 与 `declareProperty` 数量不一致 | 运行时容量异常或浪费 |
| 忘了 `newRecord()` | copy / reset 失败 |
| 字段不是 `final` | 反射读不到值 |
| Property key 在同一 Record 内重复 | 构造期即抛 StructPackException |
| 手写 id 重复 / id ∉ 1..127 | **构建失败**（AutoDeclarePropertiesTask 守卫） |
| 复用已删除字段的 id | 标记注释守卫；同 id 必同字段同类型，复用会错读历史数据 |
